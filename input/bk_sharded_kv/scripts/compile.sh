#!/usr/bin/env bash
# Compile ShardNode, ShardClient, System, and compositional Obls; place under input/sharded_kv/.
#
# Usage (from repo root): ./input/sharded_kv/scripts/compile.sh
# Optional: JULAYC_OPTS='--opt-tla=apalache' for Snowcat / .apalache.cfg
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
JULAYC_OPTS="${JULAYC_OPTS:-}"
echo "Compiling input/sharded_kv/sys.jul with $JAR ${JULAYC_OPTS}"
# shellcheck disable=SC2086
"$JAVA" -jar "$JAR" $JULAYC_OPTS input/sharded_kv/sys.jul

mv -f "$REPO_ROOT/ShardNode" "$SKV_DIR/ShardNode"
mv -f "$REPO_ROOT/ShardClient" "$SKV_DIR/ShardClient"

move_tla() {
  local name="$1"
  if [[ -f "$REPO_ROOT/${name}.tla" ]]; then
    mv -f "$REPO_ROOT/${name}.tla" "$SKV_DIR/${name}.tla"
  fi
  if [[ -f "$REPO_ROOT/${name}.cfg" ]]; then
    mv -f "$REPO_ROOT/${name}.cfg" "$SKV_DIR/${name}.cfg"
  fi
  if [[ -f "$REPO_ROOT/${name}.apalache.cfg" ]]; then
    mv -f "$REPO_ROOT/${name}.apalache.cfg" "$SKV_DIR/${name}.apalache.cfg"
  fi
  if [[ -f "$REPO_ROOT/${name}_SC2.cfg" ]]; then
    mv -f "$REPO_ROOT/${name}_SC2.cfg" "$SKV_DIR/${name}_SC2.cfg"
  fi
}

# CircSide only for ShardEnvObl × StoreObl (circular AG). Put/Inbound are ordinary RG Obls.
# julayc emits MODULE CircSide (not ShardEnvObl_StoreObl_CircSide).
for name in System PutEnvObl InboundEnvObl StoreObl ShardEnvObl CircSide; do
  move_tla "$name"
done

rm -f "$SKV_DIR/ShardEnvSpec.tla" "$SKV_DIR/ShardEnvSpec.cfg" \
  "$SKV_DIR/StoreSpec.tla" "$SKV_DIR/StoreSpec.cfg" \
  "$SKV_DIR/ShardSpec.tla" "$SKV_DIR/ShardSpec.cfg" \
  "$SKV_DIR/ShardEnvObl_StoreObl_CircSide.tla" \
  "$SKV_DIR/ShardEnvObl_StoreObl_CircSide.cfg" \
  "$SKV_DIR/ShardEnvObl_StoreObl_CircSide_SC2.cfg" \
  "$SKV_DIR/PutEnvObl_StoreObl_CircSide.tla" \
  "$SKV_DIR/PutEnvObl_StoreObl_CircSide.cfg" \
  "$SKV_DIR/PutEnvObl_StoreObl_CircSide_SC2.cfg" \
  "$SKV_DIR/InboundEnvObl_StoreObl_CircSide.tla" \
  "$SKV_DIR/InboundEnvObl_StoreObl_CircSide.cfg" \
  "$SKV_DIR/InboundEnvObl_StoreObl_CircSide_SC2.cfg"

setopt NULL_GLOB 2>/dev/null || true
# Drop stale Apalache cfgs when compiling without --opt-tla=apalache.
if [[ " ${JULAYC_OPTS} " != *" --opt-tla=apalache "* && " ${JULAYC_OPTS} " != *" --opt-tla=ALL "* && " ${JULAYC_OPTS} " != *" --opt-tla "* ]]; then
  rm -f "$SKV_DIR"/*.apalache.cfg "$REPO_ROOT"/*.apalache.cfg
fi
rm -rf "$REPO_ROOT/ShardNode-jul-build" "$REPO_ROOT/ShardClient-jul-build"

chmod +x "$SKV_DIR/ShardNode" "$SKV_DIR/ShardClient"
echo "Wrote:"
echo "  $SKV_DIR/ShardNode"
echo "  $SKV_DIR/ShardClient"
for name in System PutEnvObl InboundEnvObl StoreObl ShardEnvObl CircSide; do
  if [[ -f "$SKV_DIR/${name}.tla" ]]; then
    echo "  $SKV_DIR/${name}.tla"
  fi
  if [[ -f "$SKV_DIR/${name}.cfg" ]]; then
    echo "  $SKV_DIR/${name}.cfg"
  fi
  if [[ -f "$SKV_DIR/${name}.apalache.cfg" ]]; then
    echo "  $SKV_DIR/${name}.apalache.cfg"
  fi
  if [[ -f "$SKV_DIR/${name}_SC2.cfg" ]]; then
    echo "  $SKV_DIR/${name}_SC2.cfg"
  fi
done
