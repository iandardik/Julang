#!/usr/bin/env bash
# Compile ShardNode, ShardClient, and ShardSpec; place artifacts under input/sharded_kv/.
#
# Usage (from repo root): ./input/sharded_kv/scripts/compile.sh
# Requires: ./gradlew shadowJar (or an existing build/libs/julayc.jar)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKV_DIR/../.." && pwd)"

JAR="${JULAYC_JAR:-$REPO_ROOT/build/libs/julayc.jar}"
if [[ ! -f "$JAR" ]]; then
  echo "error: julayc not found at $JAR (run ./gradlew shadowJar or set JULAYC_JAR)" >&2
  exit 1
fi

if [[ -z "${JAVA:-}" ]]; then
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA="$JAVA_HOME/bin/java"
  else
    JAVA=java
  fi
fi

cd "$REPO_ROOT"
echo "Compiling input/sharded_kv/sys.jul with $JAR"
"$JAVA" -jar "$JAR" input/sharded_kv/sys.jul

mv -f "$REPO_ROOT/ShardNode" "$SKV_DIR/ShardNode"
mv -f "$REPO_ROOT/ShardClient" "$SKV_DIR/ShardClient"
mv -f "$REPO_ROOT/ShardSpec.tla" "$SKV_DIR/ShardSpec.tla"
mv -f "$REPO_ROOT/ShardSpec.cfg" "$SKV_DIR/ShardSpec.cfg"
rm -rf "$REPO_ROOT/ShardNode-jul-build" "$REPO_ROOT/ShardClient-jul-build"

chmod +x "$SKV_DIR/ShardNode" "$SKV_DIR/ShardClient"
echo "Wrote:"
echo "  $SKV_DIR/ShardNode"
echo "  $SKV_DIR/ShardClient"
echo "  $SKV_DIR/ShardSpec.tla"
echo "  $SKV_DIR/ShardSpec.cfg"
