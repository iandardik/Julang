#!/usr/bin/env bash
# Smoke-test the sharded KV cluster: start → put/get/owners/redirect → stop.
#
# Usage: ./smoke_test.sh [configPath]
# Env: SHARD_NODE_BIN, SHARD_CLIENT_BIN, LISTEN_TIMEOUT_S (default 15)
#
# Each check prints PASS or FAIL. Exit 0 only if every check passed.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKV_DIR/../.." && pwd)"
CONFIG="${1:-$SKV_DIR/cluster.conf}"
LISTEN_TIMEOUT_S="${LISTEN_TIMEOUT_S:-15}"

CLIENT="${SHARD_CLIENT_BIN:-}"
if [[ -z "$CLIENT" ]]; then
  if [[ -x "$SKV_DIR/ShardClient" ]]; then
    CLIENT="$SKV_DIR/ShardClient"
  elif [[ -x "$REPO_ROOT/ShardClient" ]]; then
    CLIENT="$REPO_ROOT/ShardClient"
  else
    echo "error: ShardClient not found; run input/sharded_kv/scripts/compile.sh or set SHARD_CLIENT_BIN" >&2
    exit 1
  fi
fi

URLS=()
while IFS= read -r line || [[ -n "$line" ]]; do
  trimmed="$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
  [[ -z "$trimmed" ]] && continue
  [[ "$trimmed" == \#* ]] && continue
  URLS+=("$trimmed")
done < "$CONFIG"
N="${#URLS[@]}"
if [[ "$N" -lt 2 ]]; then
  echo "error: need at least 2 nodes in $CONFIG" >&2
  exit 1
fi

port_of() {
  echo "$1" | sed -E 's|^.*:([0-9]+)$|\1|'
}

wait_listen() {
  local port="$1"
  local deadline=$((SECONDS + LISTEN_TIMEOUT_S))
  while (( SECONDS < deadline )); do
    if nc -z 127.0.0.1 "$port" 2>/dev/null; then
      return 0
    fi
    sleep 0.2
  done
  echo "error: port $port not listening within ${LISTEN_TIMEOUT_S}s" >&2
  return 1
}

PASS_N=0
FAIL_N=0

# check NAME PATTERN OUTPUT — prints PASS/FAIL; OUTPUT is the client response.
check() {
  local name="$1"
  local pattern="$2"
  local out="$3"
  echo "-- $name"
  if [[ -n "$out" ]]; then
    echo "$out" | sed 's/^/   /'
  else
    echo "   (empty response)"
  fi
  if printf '%s\n' "$out" | grep -Eq -- "$pattern"; then
    echo "   => PASS"
    PASS_N=$((PASS_N + 1))
    return 0
  else
    echo "   => FAIL (expected match: $pattern)"
    FAIL_N=$((FAIL_N + 1))
    return 1
  fi
}

echo "== stop any prior cluster =="
"$SCRIPT_DIR/stop_cluster.sh" "$CONFIG" || true

echo "== start cluster =="
"$SCRIPT_DIR/start_cluster.sh" "$CONFIG"

cleanup() {
  "$SCRIPT_DIR/stop_cluster.sh" "$CONFIG" || true
}
trap cleanup EXIT

echo "== wait for listen =="
for u in "${URLS[@]}"; do
  wait_listen "$(port_of "$u")"
done
sleep 0.5

U0="${URLS[0]}"
U1="${URLS[1]}"

echo "== checks =="
out="$("$CLIENT" "$U0" put smokeKey smokeVal)" || out="(client exit $?)"
check "put new key via node1" '^200 ok$' "$out" || true

out="$("$CLIENT" "$U0" get smokeKey)" || out="(client exit $?)"
check "get via node1" '^200 v=smokeVal$' "$out" || true

out="$("$CLIENT" "$U0" owners)" || out="(client exit $?)"
check "owners via node1" 'smokeKey->' "$out" || true

out="$("$CLIENT" "$U1" get smokeKey)" || out="(client exit $?)"
check "get via node2 (redirect then value)" 'v=smokeVal' "$out" || true

out="$("$CLIENT" "$U1" put smokeKey smokeVal2)" || out="(client exit $?)"
check "put same key via node2 (redirect then ok)" 'ok' "$out" || true

out="$("$CLIENT" "$U0" get smokeKey)" || out="(client exit $?)"
check "verify value on owner" '^200 v=smokeVal2$' "$out" || true

echo "== summary: $PASS_N passed, $FAIL_N failed =="
if [[ "$FAIL_N" -eq 0 ]]; then
  echo "SMOKE PASS"
  exit 0
fi
echo "SMOKE FAIL"
exit 1
