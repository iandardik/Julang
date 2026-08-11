#!/usr/bin/env bash
# Start one RaftNode process per URL in the cluster config (background).
#
# Usage: ./start_cluster.sh [configPath]
#
# Requires RaftNode.jar (compile input/raft/main.jul), or set RAFT_NODE_JAR.
# PIDs are written to .raft-pids next to the config file.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RAFT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG="${1:-$RAFT_DIR/cluster.conf}"
PIDFILE="$(dirname "$CONFIG")/.raft-pids"
LOGDIR="$(dirname "$CONFIG")"

# Prefer a real JDK over macOS /usr/bin/java stub.
if [[ -z "${JAVA:-}" ]]; then
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA="$JAVA_HOME/bin/java"
  elif [[ -x /Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home/bin/java ]]; then
    JAVA=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home/bin/java
  else
    JAVA=java
  fi
fi

if [[ -n "${RAFT_NODE_JAR:-}" ]]; then
  JAR="$RAFT_NODE_JAR"
elif [[ -f "$RAFT_DIR/RaftNode.jar" ]]; then
  JAR="$RAFT_DIR/RaftNode.jar"
elif [[ -f "$RAFT_DIR/../../RaftNode.jar" ]]; then
  JAR="$RAFT_DIR/../../RaftNode.jar"
elif [[ -f "$PWD/RaftNode.jar" ]]; then
  JAR="$PWD/RaftNode.jar"
else
  echo "error: RaftNode.jar not found; compile input/raft/main.jul or set RAFT_NODE_JAR" >&2
  exit 1
fi

if [[ ! -f "$CONFIG" ]]; then
  echo "error: config not found: $CONFIG" >&2
  exit 1
fi

# Parse config the same way as Julay: trim, skip blanks and # comments.
URLS=()
while IFS= read -r line || [[ -n "$line" ]]; do
  trimmed="$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
  [[ -z "$trimmed" ]] && continue
  [[ "$trimmed" == \#* ]] && continue
  URLS+=("$trimmed")
done < "$CONFIG"

N="${#URLS[@]}"
if [[ "$N" -eq 0 ]]; then
  echo "error: no node URLs in $CONFIG" >&2
  exit 1
fi

CONFIG_ABS="$(cd "$(dirname "$CONFIG")" && pwd)/$(basename "$CONFIG")"

: > "$PIDFILE"
echo "Starting $N RaftNode(s) from $CONFIG_ABS using $JAR"

i=1
while [[ "$i" -le "$N" ]]; do
  log="$LOGDIR/raft-node-$i.log"
  "$JAVA" -jar "$JAR" "$CONFIG_ABS" "$i" >"$log" 2>&1 &
  pid=$!
  echo "$pid" >> "$PIDFILE"
  echo "  node $i (${URLS[$((i - 1))]}) pid=$pid log=$log"
  i=$((i + 1))
done

echo "PIDs written to $PIDFILE"
