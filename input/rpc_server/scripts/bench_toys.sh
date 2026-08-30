#!/usr/bin/env bash
# Compile and run toy HTTP load for echo / inc / rpc / rpc-native.
# Usage:
#   ./bench_toys.sh                  # default opts, echo+inc+rpc
#   ./bench_toys.sh --targets rpc,rpc-native --ops 200 --clients 4
#   ./bench_toys.sh --with-disable-opt
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$ROOT"

# Prefer a real JDK when macOS /usr/bin/java is a stub.
if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
  export JAVA_HOME
  [[ -n "$JAVA_HOME" ]] && export PATH="$JAVA_HOME/bin:$PATH"
fi

OPS=50
CLIENTS=1
WARMUP=10
WITH_DISABLE_OPT=0
TARGETS="echo,inc,rpc"
MODE=mix
TIMEOUT=30

while [[ $# -gt 0 ]]; do
  case "$1" in
    --ops) OPS="$2"; shift 2 ;;
    --clients) CLIENTS="$2"; shift 2 ;;
    --warmup) WARMUP="$2"; shift 2 ;;
    --mode) MODE="$2"; shift 2 ;;
    --targets) TARGETS="$2"; shift 2 ;;
    --timeout) TIMEOUT="$2"; shift 2 ;;
    --with-disable-opt) WITH_DISABLE_OPT=1; shift ;;
    -h|--help)
      sed -n '2,8p' "$0"
      exit 0
      ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

echo "== building julayc =="
./gradlew shadowJar -q

JULAYC=(java -jar build/libs/julayc.jar)
BENCH="$ROOT/input/rpc_server/scripts/bench_load.py"
NATIVE_DIR="$ROOT/input/rpc_server/native"
NATIVE_JAR="$NATIVE_DIR/build/libs/RpcServerNative.jar"
chmod +x "$BENCH"

compile_and_jar() {
  local src="$1"
  local jar_name="$2"
  shift 2
  rm -f "$jar_name"
  "${JULAYC[@]}" "$@" "$src"
  if [[ ! -f "$jar_name" ]]; then
    echo "error: expected $jar_name after compiling $src" >&2
    exit 1
  fi
}

build_native() {
  echo "== building RpcServerNative =="
  "$ROOT/gradlew" -p "$NATIVE_DIR" shadowJar -q
  if [[ ! -f "$NATIVE_JAR" ]]; then
    echo "error: expected $NATIVE_JAR" >&2
    exit 1
  fi
}

run_jar() {
  local target="$1"
  local label="$2"
  local jar="$3"

  echo
  echo "############################################################"
  echo "# $label  target=$target"
  echo "############################################################"

  local server_log
  server_log="$(mktemp -t julay-toy-server.XXXXXX)"
  java -jar "$jar" >"$server_log" 2>&1 &
  local pid=$!
  cleanup() {
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    rm -f "$server_log"
  }
  trap cleanup EXIT

  for _ in $(seq 1 50); do
    if curl -s -o /dev/null -m 1 -X POST "http://127.0.0.1:8000/" 2>/dev/null; then
      break
    fi
    if curl -s -o /dev/null -m 1 -X POST "http://127.0.0.1:8000/rpc/get" 2>/dev/null; then
      break
    fi
    sleep 0.1
  done

  # rpc-native speaks the same wire API as rpc
  local bench_target="$target"
  if [[ "$target" == "rpc-native" ]]; then
    bench_target=rpc
  fi

  python3 "$BENCH" \
    --url http://127.0.0.1:8000 \
    --target "$bench_target" \
    --ops "$OPS" \
    --clients "$CLIENTS" \
    --warmup "$WARMUP" \
    --mode "$MODE" \
    --timeout "$TIMEOUT"

  cleanup
  trap - EXIT
  sleep 0.3
}

run_julay() {
  local target="$1"
  local label="$2"
  local jar="$3"
  local src="$4"
  shift 4
  compile_and_jar "$src" "$jar" "$@"
  run_jar "$target" "$label" "$jar"
}

IFS=',' read -r -a TARGET_ARR <<< "$TARGETS"

for t in "${TARGET_ARR[@]}"; do
  case "$t" in
    echo)
      run_julay echo "opts=default" EchoServer.jar input/echo_server/main.jul
      if [[ "$WITH_DISABLE_OPT" -eq 1 ]]; then
        run_julay echo "opts=disabled" EchoServer.jar input/echo_server/main.jul --disable-opt
      fi
      ;;
    inc)
      run_julay inc "opts=default" IncServer.jar input/inc_server/main.jul
      if [[ "$WITH_DISABLE_OPT" -eq 1 ]]; then
        run_julay inc "opts=disabled" IncServer.jar input/inc_server/main.jul --disable-opt
      fi
      ;;
    rpc)
      run_julay rpc "julay opts=default" RpcServer.jar input/rpc_server/main.jul
      if [[ "$WITH_DISABLE_OPT" -eq 1 ]]; then
        run_julay rpc "julay opts=disabled" RpcServer.jar input/rpc_server/main.jul --disable-opt
      fi
      ;;
    rpc-native)
      build_native
      run_jar rpc-native "kotlin-native" "$NATIVE_JAR"
      ;;
    *)
      echo "unknown target: $t" >&2
      exit 2
      ;;
  esac
done

echo
echo "done."
