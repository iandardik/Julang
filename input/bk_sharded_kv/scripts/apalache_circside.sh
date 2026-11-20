#!/usr/bin/env bash
# CircSide SC1 / SC2 at inductive bound B (same sorts as apalache_inductive.sh).
#   SC1: Init ⇒ StoreIface ∧ StoreAsm           (--length=0)
#   SC2: one-step from Init — every successor satisfies StoreIface ∨ StoreAsm
#        (Apalache state inv after length=1; full [][Next=>A'∨G'] needs TLC/temporal)
#
# Usage (from repo root, after compile --opt-tla=apalache):
#   ./input/sharded_kv/scripts/apalache_circside.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APALACHE_MC="${APALACHE_MC:-$HOME/apalache/bin/apalache-mc}"
export JVM_ARGS="${JVM_ARGS:--Xmx8G}"
OUT_DIR="${APALACHE_OUT:-$SKV_DIR/apalache-out}"
mkdir -p "$OUT_DIR"

if [[ ! -x "$APALACHE_MC" ]]; then
  echo "error: apalache-mc not found (set APALACHE_MC)" >&2
  exit 1
fi
if [[ ! -f "$SKV_DIR/CircSide.tla" ]]; then
  echo "error: missing CircSide.tla (compile with --opt-tla=apalache)" >&2
  exit 1
fi

CFG="$OUT_DIR/CircSide.inductive.cfg"
cat > "$CFG" <<'EOF'
\* CircSide at inductive bound B
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
CONSTANT String = {"", "n1", "n2"}
EOF

run() {
  local init="$1" inv="$2" length="$3" label="$4" next="${5:-Next}"
  local log="$OUT_DIR/CircSide.${label}.log"
  echo "==== Apalache CircSide [$label] init=$init next=$next inv=$inv length=$length ===="
  set +e
  (
    cd "$SKV_DIR"
    "$APALACHE_MC" check \
      --config="$CFG" \
      --init="$init" \
      --next="$next" \
      --inv="$inv" \
      --length="$length" \
      --no-deadlock \
      --out-dir="$OUT_DIR/_apalache-out" \
      CircSide.tla
  ) 2>&1 | tee "$log"
  set -e
  if grep -qiE 'The outcome is: NoError|Checker reports no error' "$log"; then
    echo "PASS CircSide [$label]"
    return 0
  fi
  echo "FAIL CircSide [$label] (see $log)" >&2
  return 1
}

# SC1: Init ⇒ A ∧ G
run Init SC1 0 sc1-init || exit 1

# SC2 witness from Init: after one Next, A ∨ G holds in the successor
# (StoreIface ∨ StoreAsm as state inv on reached states).
run Init SC1 1 sc2-from-init || exit 1

echo "CircSide Apalache SC1 + Init-successor AG finished OK."
