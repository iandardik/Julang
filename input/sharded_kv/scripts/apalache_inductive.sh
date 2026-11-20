#!/usr/bin/env bash
# Apalache inductive-invariance checks for Obl LocalInvs on full Next:
#   (1) Init ⇒ Inv            --init=Init --inv=Inv --length=0
#   (2) Inv ∧ Next ⇒ Inv'     --init=InvInit --inv=Inv --length=1
#       where InvInit == TypeOK /\ Inv  (Inv alone is not an Apalache assignment-init)
#
# Usage (from repo root):
#   JULAYC_OPTS='--opt-tla=apalache' ./input/sharded_kv/scripts/compile.sh
#   ./input/sharded_kv/scripts/apalache_inductive.sh              # all Obls
#   ./input/sharded_kv/scripts/apalache_inductive.sh InboundEnvObl
#
# Default JVM_ARGS=-Xmx8G. Tiny CONSTANTS (bound B for inductive discharge):
#   NodeSet={n1,n2}, Key={a}, DynProc={dp1}, String={"","n1","n2"}, bags |·|≤1.
# Full Obl Next including open goMoveBatch. Put/Shard write syncs (putLocal,
# applyClaim, putFromMove, dropReshardBatch) conjoin StoreAsmOK — the same gate
# StoreObl applies under assume StoreAsm — modeling Julang client/service sync
# in the product (not an Inv'-filter under-approx of unrelated transitions).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

APALACHE_MC="${APALACHE_MC:-$HOME/apalache/bin/apalache-mc}"
if [[ ! -x "$APALACHE_MC" ]]; then
  echo "error: apalache-mc not found (set APALACHE_MC)" >&2
  exit 1
fi

export JVM_ARGS="${JVM_ARGS:--Xmx8G}"
OUT_DIR="${APALACHE_OUT:-$SKV_DIR/apalache-out}"
mkdir -p "$OUT_DIR"

inv_for() {
  case "$1" in
    StoreObl) echo StoreInv ;;
    PutEnvObl) echo PutInv ;;
    InboundEnvObl) echo InboundInv ;;
    ShardEnvObl) echo ShardEnvInv ;;
    *) echo "" ;;
  esac
}

write_inductive_cfg() {
  local dst="$1"
  cat > "$dst" <<'EOF'
\* Apalache inductive-check model (bound B; Nat finite for TypeOK)
INIT Init
NEXT Next
CHECK_DEADLOCK FALSE
CONSTANT NodeSet = {"n1", "n2"}
CONSTANT DynProc = {"dp1"}
CONSTANT Key = {"a"}
CONSTANT Value = {"v1"}
CONSTANT Int = {0, 1}
CONSTANT Nat = {0, 1}
CONSTANT MaxListLen = 1
\* Only node URLs / empty — AppliedClaim/ClaimSeen/msg owner fields use String.
\* Step tags ("call","claim",…) are finite enums inside TypeOK, not String.
CONSTANT String = {"", "n1", "n2"}
EOF
}

# Thin EXTENDS wrapper: InvInit assigns via TypeOK (+ optional msg caps / Asm), then Inv.
write_wrapper() {
  local name="$1"
  local inv="$2"
  local dst="$SKV_DIR/${name}_IndCheck.tla"
  local extras=()
  # Obls with PerfectNet msg bags: cap |bag| so TypeOK's SUBSET does not OOM.
  if grep -q 'moveBatchMsgs' "$SKV_DIR/${name}.tla" 2>/dev/null; then
    extras+=(
      '  /\ Cardinality(claimMsgs) <= 1'
      '  /\ Cardinality(moveBatchMsgs) <= 1'
      '  /\ Cardinality(keyCountReqs) <= 1'
      '  /\ Cardinality(keyCountReplies) <= 1'
    )
  fi
  # Inbound: start under assume; couple ApplyClaim / MoveApply slots to fluents.
  if [[ "$name" == "InboundEnvObl" ]]; then
    extras+=(
      '  /\ InboundAsm'
      '  /\ \A n \in NodeSet, i \in DynProc:'
      '        ApplyClaim_constructed[n][i] =>'
      '          /\ owner[n][i] \in NodeSet'
      '          /\ owner[n][i] # n'
      '          /\ \A co \in String:'
      '                ClaimSeen[key_ApplyClaim[n][i], co] => co = owner[n][i]'
      '  /\ \A n \in NodeSet, k \in Key, o \in String:'
      '        PendingClaimApply[n, k, o] <=>'
      '          \E i \in DynProc:'
      '            /\ ApplyClaim_constructed[n][i]'
      '            /\ step_ApplyClaim[n][i] = "call"'
      '            /\ key_ApplyClaim[n][i] = k'
      '            /\ owner[n][i] = o'
      '  /\ \A k \in Key:'
      '        MoveApplyPending[k] <=>'
      '          \E n \in NodeSet, i \in DynProc:'
      '            /\ ApplyMoveBatch_constructed[n][i]'
      '            /\ step_ApplyMoveBatch[n][i] = "store"'
      '            /\ key_ApplyMoveBatch[n][i] = k'
      '  /\ \A n \in NodeSet, i \in DynProc:'
      '        (ApplyMoveBatch_constructed[n][i]'
      '            /\ step_ApplyMoveBatch[n][i] = "store") =>'
      '          meUrl_ApplyMoveBatch[n][i] = n'
      '  /\ \A k \in Key, o \in String:'
      '        MoveApplyOwner[k, o] <=>'
      '          \E n \in NodeSet, i \in DynProc:'
      '            /\ ApplyMoveBatch_constructed[n][i]'
      '            /\ step_ApplyMoveBatch[n][i] = "store"'
      '            /\ key_ApplyMoveBatch[n][i] = k'
      '            /\ meUrl_ApplyMoveBatch[n][i] = o'
      # Bag inject pending: every move msg has MoveBagPending (InboundAsm shape).
      '  /\ \A msg \in moveBatchMsgs: MoveBagPending[msg.k]'
    )
  fi
  {
    echo "---- MODULE ${name}_IndCheck ----"
    echo "EXTENDS ${name}, FiniteSets"
    echo ""
    if [[ "$name" == "StoreObl" ]]; then
      cat <<'STORE'
\* Nondet assignment of every var (Apalache), then the inductive predicate.
\* Compiler TypeOK uses DOMAIN ⊆ (not an Apalache assignment). Build partial
\* maps via \E dom + total fun restriction instead of UNION {[D -> _]}.
ApaTypeOK ==
  /\ AppliedClaim \in [NodeSet \X Key \X String -> BOOLEAN]
  /\ Held \in [NodeSet \X Key -> BOOLEAN]
  /\ ClaimSeen \in [Key \X String -> BOOLEAN]
  /\ MeTold \in [NodeSet \X String -> BOOLEAN]
  /\ OwnerTold \in [NodeSet \X Key \X String -> BOOLEAN]
  /\ me \in [NodeSet -> [id: TypeOKInt, url: String]]
  /\ \E storeDom \in [NodeSet -> SUBSET Key] :
       \E storeTot \in [NodeSet -> [Key -> Value]] :
         store = [n \in NodeSet |-> [k \in storeDom[n] |-> storeTot[n][k]]]
  /\ \E ownersDom \in [NodeSet -> SUBSET Key] :
       \E ownersTot \in [NodeSet -> [Key -> String]] :
         owners = [n \in NodeSet |-> [k \in ownersDom[n] |-> ownersTot[n][k]]]

InvInit ==
  /\ ApaTypeOK
  /\ StoreAsm
  /\ StoreInv

STORE
    elif [[ "$name" == "PutEnvObl" || "$name" == "ShardEnvObl" ]]; then
      # TypeOK uses BoundedSeq = UNION {[1..k->S]} which Apalache refuses to expand.
      python3 - "$SKV_DIR/${name}.tla" "$inv" <<'PY'
import re, sys
from pathlib import Path
t = Path(sys.argv[1]).read_text()
inv = sys.argv[2]
m = re.search(r"^TypeOK ==\n((?:  .*\n)+)", t, re.M)
if not m:
    raise SystemExit("TypeOK not found")
body = m.group(1)
pat = r"  /\\ movedEntries \\in \[NodeSet -> \[DynProc -> BoundedSeq\(.*\)\]\]\n"
repl = (
    "  /\\ movedEntries \\in [NodeSet -> [DynProc -> "
    "{ [x \\in {} |-> [k |-> \"a\", v |-> \"v1\"]] } \\cup "
    "{ [j \\in {1} |-> e] : e \\in [k: Key, v: Value] }]]\n"
)
body2, n = re.subn(pat, lambda _: repl, body, count=1)
if n != 1:
    raise SystemExit(f"movedEntries rewrite failed (n={n})")
print("\\* Nondet assignment (ApaTypeOK avoids BoundedSeq expansion).")
print("ApaTypeOK ==")
print(body2, end="")
print()
print("InvInit ==")
print("  /\\ ApaTypeOK")
print("  /\\ Cardinality(claimMsgs) <= 1")
print("  /\\ Cardinality(moveBatchMsgs) <= 1")
print("  /\\ Cardinality(keyCountReqs) <= 1")
print("  /\\ Cardinality(keyCountReplies) <= 1")
print("  /\\ StoreIface")
print(f"  /\\ {inv}")
print()
# Product write-sync: StoreAsmOK (IndexedStore assume). Held/bag mutators also
# require MoveNetOk' so putLocal cannot arm Held while a move msg names that key.
print("StoreAsmOK ==")
print("  /\\ \\A n \\in NodeSet, k \\in Key: ~AppliedClaim[n, k, n]'")
print("  /\\ \\A k \\in Key, n1, n2 \\in NodeSet:")
print("        (Held[n1, k]' /\\ Held[n2, k]') => n1 = n2")
print("  /\\ \\A k \\in Key, o1, o2 \\in String:")
print("        (ClaimSeen[k, o1]' /\\ ClaimSeen[k, o2]') => o1 = o2")
print()
print("MoveNetOkOK == MoveNetOk_BagShape' /\\ MoveNetOk_UniqueKey'")
print()
t = Path(sys.argv[1]).read_text()
gates = [
    ("putLocal(n, i)", "putLocalSync", "(Str, Str)",
     "putLocal(n, i) /\\ StoreAsmOK /\\ MoveNetOkOK"),
    ("applyClaim(n, i)", "applyClaimSync", "(Str, Str)",
     "applyClaim(n, i) /\\ StoreAsmOK"),
    ("putFromMove(n, i)", "putFromMoveSync", "(Str, Str)",
     "putFromMove(n, i) /\\ StoreAsmOK /\\ MoveNetOkOK"),
    ("dropReshardBatch(n, i, entries)", "dropReshardBatchSync",
     "(Str, Str, Int -> {k: Str, v: Str})",
     "dropReshardBatch(n, i, entries) /\\ StoreAsmOK /\\ MoveNetOkOK"),
    ("goMoveBatch(n, i, dest, peerUrl)", "goMoveBatchSync",
     "(Str, Str, Str, Str)",
     "goMoveBatch(n, i, dest, peerUrl) /\\ MoveNetOkOK"),
]
m = re.search(r"^Next ==\n((?:  .*\n)+)", t, re.M)
if not m:
    raise SystemExit("Next not found for NextSync")
body = m.group(1)
for call, name, typ, rhs in gates:
    if call not in body:
        continue
    args = call[call.index("("):]
    print(f"\\* @type: {typ} => Bool;")
    print(f"{name}{args} == {rhs}")
    print()
    body = body.replace(call, f"{name}{args}")
print("NextSync ==")
print(body, end="")
print()
PY
    else
      echo '\* Nondet assignment of every var (Apalache), then the inductive predicate.'
      echo "InvInit =="
      echo "  /\ TypeOK"
      local line
      for line in "${extras[@]+"${extras[@]}"}"; do
        echo "$line"
      done
      echo "  /\ ${inv}"
      echo ""
    fi
    # InboundAsmOK primes fluents but not moveBatchMsgs — open goMoveBatch can
    # inject a second key duplicate under a still-true AsmOK. Conjoin primed bag
    # UniqueKey/BagShape on goMoveBatch (product-faithful bag discipline).
    if [[ "$name" == "InboundEnvObl" ]]; then
      python3 - "$SKV_DIR/${name}.tla" <<'PY'
import re, sys
from pathlib import Path
t = Path(sys.argv[1]).read_text()
print("MoveBagShapeOK ==")
print("  /\\ \\A msgSrc, msgDest \\in NodeSet, msgOwner \\in String, msgKey \\in Key, msgVal \\in Value:")
print("        ([src |-> msgSrc, dest |-> msgDest, owner |-> msgOwner,")
print("          k |-> msgKey, v |-> msgVal] \\in moveBatchMsgs') =>")
print("          /\\ msgOwner = msgDest")
print("          /\\ msgSrc # msgDest")
print("          /\\ ~Held[msgSrc, msgKey]'")
print("          /\\ ~Held[msgDest, msgKey]'")
print("          /\\ MoveBagPending[msgKey]'")
print("          /\\ ~MoveApplyPending[msgKey]'")
print()
print("MoveBagUniqueOK ==")
print("  /\\ \\A msgSrc1, msgDest1 \\in NodeSet, msgOwner1 \\in String, msgKey \\in Key, msgVal1 \\in Value,")
print("         msgSrc2, msgDest2 \\in NodeSet, msgOwner2 \\in String, msgVal2 \\in Value:")
print("        (([src |-> msgSrc1, dest |-> msgDest1, owner |-> msgOwner1,")
print("           k |-> msgKey, v |-> msgVal1] \\in moveBatchMsgs')")
print("         /\\ ([src |-> msgSrc2, dest |-> msgDest2, owner |-> msgOwner2,")
print("              k |-> msgKey, v |-> msgVal2] \\in moveBatchMsgs')) =>")
print("          /\\ msgSrc1 = msgSrc2 /\\ msgDest1 = msgDest2")
print("          /\\ msgOwner1 = msgOwner2 /\\ msgVal1 = msgVal2")
print()
print("\\* @type: (Str, Str, Str, Str, Str) => Bool;")
print("goMoveBatchSync(n, dest, ownerUrl, key, val_) ==")
print("  goMoveBatch(n, dest, ownerUrl, key, val_) /\\ MoveBagShapeOK /\\ MoveBagUniqueOK")
print()
m = re.search(r"^Next ==\n((?:  .*\n)+)", t, re.M)
if not m:
    raise SystemExit("Next not found for NextSync")
body = m.group(1).replace(
    "goMoveBatch(n, dest, ownerUrl, key, val_)",
    "goMoveBatchSync(n, dest, ownerUrl, key, val_)",
)
print("NextSync ==")
print(body, end="")
print()
PY
    fi
    echo "===="
  } > "$dst"
}

run_check() {
  local obl="$1"
  local module="$2"
  local init="$3"
  local inv="$4"
  local length="$5"
  local label="$6"
  local next="${7:-Next}"
  local cfg="$OUT_DIR/${obl}.inductive.cfg"
  local log="$OUT_DIR/${obl}.inductive-${label}.log"
  echo "==== Apalache inductive $module [$label] init=$init next=$next inv=$inv length=$length (JVM_ARGS=$JVM_ARGS) ===="
  set +e
  (
    cd "$SKV_DIR"
    "$APALACHE_MC" check \
      --config="$cfg" \
      --init="$init" \
      --next="$next" \
      --inv="$inv" \
      --length="$length" \
      --no-deadlock \
      --out-dir="$OUT_DIR/_apalache-out" \
      "${module}.tla"
  ) 2>&1 | tee "$log"
  local rc=${PIPESTATUS[0]}
  set -e
  if grep -qiE 'The outcome is: NoError|Checker reports no error' "$log"; then
    echo "PASS $obl [$label]"
    return 0
  fi
  echo "FAIL $obl [$label] (see $log)" >&2
  return 1
}

run_one() {
  local name="$1"
  local inv
  inv="$(inv_for "$name")"
  if [[ -z "$inv" ]]; then
    echo "error: unknown Obl $name" >&2
    return 1
  fi
  if [[ ! -f "$SKV_DIR/${name}.tla" ]]; then
    echo "error: missing $SKV_DIR/${name}.tla (compile with --opt-tla=apalache)" >&2
    return 1
  fi
  write_inductive_cfg "$OUT_DIR/${name}.inductive.cfg"
  write_wrapper "$name" "$inv"
  run_check "$name" "$name" "Init" "$inv" 0 "init-implies-inv" || return 1
  local next="Next"
  if [[ "$name" == "PutEnvObl" || "$name" == "ShardEnvObl" || "$name" == "InboundEnvObl" ]]; then
    next="NextSync"
  fi
  run_check "$name" "${name}_IndCheck" "InvInit" "$inv" 1 "inv-step" "$next" || return 1
}

DEFAULT_ORDER=(StoreObl PutEnvObl InboundEnvObl ShardEnvObl)

if [[ $# -gt 0 ]]; then
  for name in "$@"; do
    run_one "$name" || exit 1
  done
else
  for name in "${DEFAULT_ORDER[@]}"; do
    run_one "$name" || exit 1
  done
fi

echo "All inductive Apalache Obl runs finished OK."
