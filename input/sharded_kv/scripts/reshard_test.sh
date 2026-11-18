#!/usr/bin/env bash
# Longer cluster test that forces load-based resharding.
#
# Reshard fires on put when some peer has peerCount < myCount/2 and
# myCount - peerCount >= 4 (see node/rpc_in.jul). Putting many new keys
# through node1 while peers stay empty meets that predicate once node1
# already holds >= 4 keys; floor(25%) of its keys move to the lightest peer
# (tie-break lowest node id → node2).
#
# Usage: ./reshard_test.sh [configPath]
# Env: SHARD_NODE_BIN, SHARD_CLIENT_BIN, LISTEN_TIMEOUT_S (default 15),
#      RESHARD_PUTS (default 20)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKV_DIR/../.." && pwd)"
CONFIG="${1:-$SKV_DIR/cluster.conf}"
LISTEN_TIMEOUT_S="${LISTEN_TIMEOUT_S:-15}"
RESHARD_PUTS="${RESHARD_PUTS:-20}"

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

if [[ "$RESHARD_PUTS" -lt 5 ]]; then
  echo "error: RESHARD_PUTS must be >= 5 (need myCount>=4 before a put that reshards)" >&2
  exit 1
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

key_count() {
  local url="$1"
  local body
  # HandleRpc.keyCount binds fromUrl via bodyField(body, 1); empty body panics.
  body="$(curl -sf -X POST --data-binary 'from=probe' "$url/rpc/keyCount")"
  echo "$body" | sed -E 's/^n=([0-9]+).*/\1/'
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

echo "== put $RESHARD_PUTS keys via node1 (drive reshard) =="
i=1
while [[ "$i" -le "$RESHARD_PUTS" ]]; do
  out="$("$CLIENT" "$U0" put "rk$i" "rv$i")"
  echo "  put rk$i: $out"
  echo "$out" | grep -q 'ok'
  i=$((i + 1))
done

echo "== per-node key counts after puts =="
counts=()
total=0
for u in "${URLS[@]}"; do
  c="$(key_count "$u")"
  counts+=("$c")
  total=$((total + c))
  echo "  $u keyCount=$c"
done

if [[ "$total" -ne "$RESHARD_PUTS" ]]; then
  echo "error: sum of keyCounts ($total) != puts ($RESHARD_PUTS) — keys lost or duplicated?" >&2
  exit 1
fi

n0="${counts[0]}"
if [[ "$n0" -ge "$RESHARD_PUTS" ]]; then
  echo "error: node1 still holds all $n0 keys; reshard did not move any" >&2
  exit 1
fi
if [[ "$n0" -le 0 ]]; then
  echo "error: node1 keyCount is 0 after puts via node1" >&2
  exit 1
fi

moved=0
i=1
while [[ "$i" -lt "$N" ]]; do
  moved=$((moved + counts[i]))
  i=$((i + 1))
done
if [[ "$moved" -le 0 ]]; then
  echo "error: no keys on peer nodes; expected reshard to node2 (lowest id among lightest)" >&2
  exit 1
fi
echo "  node1 holds $n0 / $RESHARD_PUTS; peers hold $moved (reshard OK)"

echo "== owners: some keys must point at a peer =="
owners_out="$("$CLIENT" "$U0" owners)"
echo "$owners_out"
echo "$owners_out" | grep -q '^200 owners='
peer_owned=0
i=1
while [[ "$i" -le "$RESHARD_PUTS" ]]; do
  if echo "$owners_out" | grep -q "rk$i->${U1}"; then
    peer_owned=$((peer_owned + 1))
  fi
  # Also count ownership by any later peer URL
  j=2
  while [[ "$j" -lt "$N" ]]; do
    if echo "$owners_out" | grep -q "rk$i->${URLS[$j]}"; then
      peer_owned=$((peer_owned + 1))
    fi
    j=$((j + 1))
  done
  i=$((i + 1))
done
if [[ "$peer_owned" -le 0 ]]; then
  echo "error: owners map has no keys pointing at peers" >&2
  exit 1
fi
echo "  owners lists $peer_owned key(s) on peers"

echo "== get every key (via node1 and via node2; follow redirects) =="
i=1
while [[ "$i" -le "$RESHARD_PUTS" ]]; do
  out0="$("$CLIENT" "$U0" get "rk$i")"
  echo "$out0" | grep -q "v=rv$i"
  out1="$("$CLIENT" "$U1" get "rk$i")"
  echo "$out1" | grep -q "v=rv$i"
  i=$((i + 1))
done
echo "  all $RESHARD_PUTS keys readable from node1 and node2"

echo "== put update on a peer-owned key via wrong node still works =="
# Find one key owned by U1 from owners blob
peer_key=""
i=1
while [[ "$i" -le "$RESHARD_PUTS" ]]; do
  if echo "$owners_out" | grep -q "rk$i->${U1}"; then
    peer_key="rk$i"
    break
  fi
  i=$((i + 1))
done
if [[ -n "$peer_key" ]]; then
  out="$("$CLIENT" "$U0" put "$peer_key" "reshardUpdated")"
  echo "  put $peer_key via node1: $out"
  echo "$out" | grep -q 'ok'
  out="$("$CLIENT" "$U1" get "$peer_key")"
  echo "$out"
  echo "$out" | grep -q 'v=reshardUpdated'
else
  echo "  (no key owned by node2 in owners snapshot; skip update check)"
fi

echo "== reshard test OK =="
