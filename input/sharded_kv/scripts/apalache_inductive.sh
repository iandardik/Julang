#!/usr/bin/env bash
# Apalache inductive-invariance checks for Obl LocalInvs:
#   (1) Init ⇒ Inv            --init=Init --inv=Inv --length=0
#   (2) Inv ∧ Next ⇒ Inv'     --init=InvInit --inv=Inv --length=1
#       where InvInit == TypeOK /\ Inv  (Inv alone is not an Apalache assignment-init)
#
# Usage (from repo root):
#   JULAYC_OPTS='--opt-tla=apalache' ./input/sharded_kv/scripts/compile.sh
#   ./input/sharded_kv/scripts/apalache_inductive.sh              # all Obls
#   ./input/sharded_kv/scripts/apalache_inductive.sh InboundEnvObl
#
# Default JVM_ARGS=-Xmx8G. Tiny CONSTANTS: String is only node URLs (step
# labels live in TypeOK enums). SUBSET(msg) size is 2^(|N|^2*|K|*|S|…) — keep |S|<=3.
# Inbound inv-step uses NextInd (Next minus open goMoveBatch): AssumeOK does not
# prime moveBatchMsgs, so the injector can break UniqueKey under a true AsmOK.
# Put/Shard inv-step uses NextInd: putLocal/goMoveBatch gated by Inv'
# (closed product syncs putLocal with store AsmOK; bag inject must keep MoveNetOk).

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
\* Apalache inductive-check model (minimal CONSTANTS; Nat finite for TypeOK)
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
  # Inbound: start under assume; couple ApplyClaim slot owner to StoreAsm (fluent
  # PendingClaimApply alone can desync from TypeOK's free constructed/owner).
  if [[ "$name" == "InboundEnvObl" ]]; then
    # Couple TypeOK's free ApplyClaim slots to PendingClaimApply / ClaimSeen so
    # InvInit cannot arm applyClaim against StoreAsm while fluents stay false.
    # PendingClaimApply must exactly match constructed slots (no extra ghost cells).
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
    )
  fi
  {
    echo "---- MODULE ${name}_IndCheck ----"
    echo "EXTENDS ${name}, FiniteSets"
    echo ""
    # StoreObl TypeOK uses DOMAIN ⊆ predicates for store/owners — not Apalache
    # assignments. Use explicit \in over partial-map ranges instead.
    if [[ "$name" == "StoreObl" ]]; then
      cat <<'STORE'
\* Nondet assignment of every var (Apalache), then the inductive predicate.
\* Compiler TypeOK uses DOMAIN ⊆ (not an Apalache assignment). Build partial
\* maps via \E dom + total fun restriction instead of UNION {[D -> _]}.
ApaTypeOK ==
  /\ AppliedClaim \in [NodeSet \X Key \X String -> BOOLEAN]
  /\ Held \in [NodeSet \X Key -> BOOLEAN]
  /\ ClaimSeen \in [Key \X String -> BOOLEAN]
  /\ IfaceBroken \in BOOLEAN
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
      # MaxListLen=1: assign movedEntries over {<<>>} ∪ singletons instead.
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
# MaxListLen=1: empty fun or singleton [1 |-> entry] (same shape as Init/BoundedSeq).
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
# PutEnv/ShardEnv emit fluent mutators under StoreIface only. Closed product
# syncs putLocal/applyClaim/putFromMove/drop with IndexedStore AsmOK; bag
# inject must keep MoveNetOk. Gate those actions with Inv' when present.
t = Path(sys.argv[1]).read_text()
gates = [
    ("putLocal(n, i)", "putLocalInd", "(Str, Str)", f"putLocal(n, i) /\\ {inv}'"),
    ("goMoveBatch(n, i, dest, peerUrl)", "goMoveBatchInd", "(Str, Str, Str, Str)",
     f"goMoveBatch(n, i, dest, peerUrl) /\\ {inv}'"),
    ("applyClaim(n, i)", "applyClaimInd", "(Str, Str)", f"applyClaim(n, i) /\\ {inv}'"),
    ("putFromMove(n, i)", "putFromMoveInd", "(Str, Str)", f"putFromMove(n, i) /\\ {inv}'"),
    ("dropReshardBatch(n, i, entries)", "dropReshardBatchInd",
     "(Str, Str, Int -> {k: Str, v: Str})",
     f"dropReshardBatch(n, i, entries) /\\ {inv}'"),
]
m = re.search(r"^Next ==\n((?:  .*\n)+)", t, re.M)
if not m:
    raise SystemExit("Next not found for NextInd")
body = m.group(1)
for call, name, typ, rhs in gates:
    if call not in body:
        continue
    args = call[call.index("("):]
    print(f"\\* @type: {typ} => Bool;")
    print(f"{name}{args} == {rhs}")
    print()
    body = body.replace(call, f"{name}{args}")
print("NextInd ==")
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
    if [[ "$name" == "InboundEnvObl" ]]; then
      # AssumeOK primes fluents but not moveBatchMsgs; open goMoveBatch can
      # therefore break UniqueKey under a still-true AsmOK. Strip the injector
      # (under-approx of the product; see protocol.jul Rest notes).
      # Cannot write Next /\ ~goMoveBatch — Apalache rejects assignments under ~.
      python3 - "$SKV_DIR/${name}.tla" <<'PY'
import re, sys
from pathlib import Path
t = Path(sys.argv[1]).read_text()
m = re.search(r"^Next ==\n((?:  .*\n)+)", t, re.M)
if not m:
    raise SystemExit("Next not found for NextInd")
lines = [l for l in m.group(1).splitlines() if l.strip() and "goMoveBatch" not in l]
print("NextInd ==")
print("\n".join(lines))
print()
PY
    fi
    echo "===="
  } > "$dst"
}

run_check() {
  local obl="$1"       # Obl base name (for cfg/log)
  local module="$2"    # .tla module stem to check
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
  # (1) Init ⇒ Inv on the Obl module
  run_check "$name" "$name" "Init" "$inv" 0 "init-implies-inv" || return 1
  # (2) Inv ∧ Next ⇒ Inv' via TypeOK/\Inv as assignment-init
  local next="Next"
  if [[ "$name" == "InboundEnvObl" || "$name" == "PutEnvObl" || "$name" == "ShardEnvObl" ]]; then
    next="NextInd"
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
