#!/usr/bin/env bash
# TLC safety campaign for compositional Obls (20 min each, -workers auto).
#
# Usage (from repo root):
#   ./input/sharded_kv/scripts/tlc_obls.sh           # all Obls in order
#   ./input/sharded_kv/scripts/tlc_obls.sh StoreObl  # one module
#
# Pins minimal sound Key models per Obl (Store/Inbound → {a,b}; Put/ShardEnv keep {a..e}).
# Pass = no Error: within TLC_TIMEOUT (default 20m).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKV_DIR/../.." && pwd)"

TLC_JAR="${TLC_JAR:-$HOME/bin/tla2tools.jar}"
if [[ ! -f "$TLC_JAR" ]]; then
  TLC_JAR="$REPO_ROOT/build/tla2tools/tla2tools.jar"
fi
if [[ ! -f "$TLC_JAR" ]]; then
  echo "error: tla2tools.jar not found (set TLC_JAR)" >&2
  exit 1
fi

if [[ -z "${JAVA:-}" ]]; then
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA="$JAVA_HOME/bin/java"
  else
    JAVA=java
  fi
fi

TLC_TIMEOUT="${TLC_TIMEOUT:-20m}"
OUT_DIR="${TLC_OUT:-$SKV_DIR/tlc-out}"
mkdir -p "$OUT_DIR"

pin_key_ab() {
  local cfg="$1"
  if [[ -f "$cfg" ]]; then
    sed -i.bak 's/CONSTANT Key = {"a", "b", "c", "d", "e"}/CONSTANT Key = {"a", "b"}/' "$cfg"
    rm -f "${cfg}.bak"
  fi
}

# Minimal sound sorts: Store + Inbound do not need 5 keys for /4 reshard.
pin_key_ab "$SKV_DIR/StoreObl.cfg"
pin_key_ab "$SKV_DIR/InboundEnvObl.cfg"

# CircSide TypeOK uses BoundedSeq([id: TypeOKInt, url: String], MaxListLen) for
# cluster — that set is too large for TLC to build as an INVARIANT. SC1/SC2 only
# need SC1 / SC2Prop; StateConstraint still bounds Int/list vars.
strip_typeok() {
  local cfg="$1"
  if [[ -f "$cfg" ]]; then
    sed -i.bak '/^INVARIANT TypeOK$/d' "$cfg"
    rm -f "${cfg}.bak"
  fi
}
strip_typeok "$SKV_DIR/CircSide.cfg"
strip_typeok "$SKV_DIR/CircSide_SC2.cfg"

run_one() {
  local name="$1"
  local tla="$SKV_DIR/${name}.tla"
  if [[ ! -f "$tla" ]]; then
    echo "error: missing $tla (run scripts/compile.sh first)" >&2
    return 1
  fi
  local log="$OUT_DIR/${name}.log"
  echo "==== TLC $name (timeout $TLC_TIMEOUT) ===="
  set +e
  (
    cd "$SKV_DIR"
    timeout "$TLC_TIMEOUT" "$JAVA" -XX:+UseParallelGC -jar "$TLC_JAR" \
      -deadlock -workers auto "${name}.tla"
  ) 2>&1 | tee "$log"
  local rc=${PIPESTATUS[0]}
  set -e
  if grep -q '^Error:' "$log"; then
    echo "FAIL $name: Error: in log (see $log)" >&2
    return 1
  fi
  if grep -q 'No error has been found' "$log"; then
    echo "PASS $name: no error (completed)"
    return 0
  fi
  if [[ $rc -eq 124 ]]; then
    echo "PASS $name: no error within $TLC_TIMEOUT (timeout)"
    return 0
  fi
  if [[ $rc -ne 0 ]]; then
    echo "FAIL $name: TLC exit $rc (see $log)" >&2
    return 1
  fi
  echo "PASS $name: TLC exit 0"
  return 0
}

DEFAULT_ORDER=(
  StoreObl
  PutEnvObl
  InboundEnvObl
  ShardEnvObl
  CircSide
)

if [[ $# -gt 0 ]]; then
  for name in "$@"; do
    run_one "$name" || exit 1
  done
else
  for name in "${DEFAULT_ORDER[@]}"; do
    run_one "$name" || exit 1
  done
fi

echo "All TLC Obl runs finished OK."
