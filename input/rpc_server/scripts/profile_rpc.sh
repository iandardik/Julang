#!/usr/bin/env bash
# Profile Julay rpc_server (or Kotlin-native / opts-off) under HTTP load with async-profiler.
#
# Prerequisites: JDK 18+. async-profiler is auto-fetched into tools/async-profiler unless
# ASYNC_PROFILER_HOME or --async-profiler-home is set.
#
# Examples:
#   ./profile_rpc.sh --variant julay --mode cpu --duration 20 --clients 4
#   ./profile_rpc.sh --variant julay --mode wall --duration 20   # includes blocked time (recommended)
#   ./profile_rpc.sh --variant native --mode wall --duration 20
#   ./profile_rpc.sh --variant julay --mode alloc --duration 20
#   ./profile_rpc.sh --variant julay-noopt --mode wall --duration 15
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$ROOT"

if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
  export JAVA_HOME
  [[ -n "$JAVA_HOME" ]] && export PATH="$JAVA_HOME/bin:$PATH"
fi

VARIANT=julay
MODE=cpu
DURATION=20
CLIENTS=4
WARMUP=40
# Enough ops that load outlives profiling (clients * ops should take > duration)
OPS=8000
TIMEOUT=60
OUT_DIR="$ROOT/input/rpc_server/scripts/profile-out"
ASYNC_HOME="${ASYNC_PROFILER_HOME:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --variant) VARIANT="$2"; shift 2 ;;
    --mode) MODE="$2"; shift 2 ;;
    --duration) DURATION="$2"; shift 2 ;;
    --clients) CLIENTS="$2"; shift 2 ;;
    --warmup) WARMUP="$2"; shift 2 ;;
    --ops) OPS="$2"; shift 2 ;;
    --timeout) TIMEOUT="$2"; shift 2 ;;
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    --async-profiler-home) ASYNC_HOME="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,14p' "$0"
      exit 0
      ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

case "$VARIANT" in
  julay|native|julay-noopt) ;;
  *) echo "error: --variant must be julay|native|julay-noopt" >&2; exit 2 ;;
esac
case "$MODE" in
  cpu|alloc|wall) ;;
  *) echo "error: --mode must be cpu|alloc|wall" >&2; exit 2 ;;
esac

BENCH="$ROOT/input/rpc_server/scripts/bench_load.py"
NATIVE_DIR="$ROOT/input/rpc_server/native"
NATIVE_JAR="$NATIVE_DIR/build/libs/RpcServerNative.jar"
TOOLS_DIR="$ROOT/tools"
AP_DIR="$TOOLS_DIR/async-profiler"
mkdir -p "$OUT_DIR" "$TOOLS_DIR"

resolve_asprof() {
  if [[ -n "$ASYNC_HOME" && -x "$ASYNC_HOME/bin/asprof" ]]; then
    echo "$ASYNC_HOME/bin/asprof"
    return
  fi
  if [[ -x "$AP_DIR/bin/asprof" ]]; then
    echo "$AP_DIR/bin/asprof"
    return
  fi
  # Extracted zip may nest one directory
  local nested
  nested="$(find "$AP_DIR" -type f -name asprof 2>/dev/null | head -1 || true)"
  if [[ -n "$nested" && -x "$nested" ]]; then
    echo "$nested"
    return
  fi
  if command -v asprof >/dev/null 2>&1; then
    command -v asprof
    return
  fi
  echo ""
}

ensure_async_profiler() {
  local asprof
  asprof="$(resolve_asprof)"
  if [[ -n "$asprof" ]]; then
    echo "$asprof"
    return
  fi

  echo "== fetching async-profiler into $AP_DIR ==" >&2
  local uname_s url tmp
  uname_s="$(uname -s)"
  case "$uname_s" in
    Darwin)
      url="https://github.com/async-profiler/async-profiler/releases/download/v4.5/async-profiler-4.5-macos.zip"
      ;;
    Linux)
      case "$(uname -m)" in
        x86_64)
          url="https://github.com/async-profiler/async-profiler/releases/download/v4.5/async-profiler-4.5-linux-x64.tar.gz"
          ;;
        aarch64|arm64)
          url="https://github.com/async-profiler/async-profiler/releases/download/v4.5/async-profiler-4.5-linux-arm64.tar.gz"
          ;;
        *)
          echo "error: unsupported Linux arch; set ASYNC_PROFILER_HOME" >&2
          exit 1
          ;;
      esac
      ;;
    *)
      echo "error: unsupported OS $uname_s; set ASYNC_PROFILER_HOME" >&2
      exit 1
      ;;
  esac

  tmp="$(mktemp -d)"
  echo "downloading $url" >&2
  curl -fsSL "$url" -o "$tmp/ap.archive"
  rm -rf "$AP_DIR"
  mkdir -p "$AP_DIR" "$tmp/extract"
  case "$url" in
    *.zip) unzip -q "$tmp/ap.archive" -d "$tmp/extract" ;;
    *) tar -xzf "$tmp/ap.archive" -C "$tmp/extract" ;;
  esac
  local top
  top="$(find "$tmp/extract" -mindepth 1 -maxdepth 1 | head -1)"
  if [[ -d "$top" ]]; then
    mv "$top"/* "$AP_DIR"/
  else
    mv "$tmp/extract"/* "$AP_DIR"/
  fi
  rm -rf "$tmp"

  asprof="$(resolve_asprof)"
  if [[ -z "$asprof" ]]; then
    echo "error: asprof missing after extract at $AP_DIR" >&2
    ls -laR "$AP_DIR" | head -40 >&2
    exit 1
  fi
  echo "$asprof"
}

ASPROF="$(ensure_async_profiler)"
echo "using asprof=$ASPROF"

echo "== building julayc =="
./gradlew shadowJar -q
JULAYC=(java -jar build/libs/julayc.jar)

JAR=""
LABEL=""
case "$VARIANT" in
  julay)
    LABEL=julay
    rm -f RpcServer.jar
    "${JULAYC[@]}" input/rpc_server/main.jul
    JAR="$ROOT/RpcServer.jar"
    ;;
  julay-noopt)
    LABEL=julay-noopt
    rm -f RpcServer.jar
    "${JULAYC[@]}" --disable-opt input/rpc_server/main.jul
    JAR="$ROOT/RpcServer.jar"
    ;;
  native)
    LABEL=native
    echo "== building RpcServerNative =="
    ./gradlew -p "$NATIVE_DIR" shadowJar -q
    JAR="$NATIVE_JAR"
    ;;
esac

if [[ ! -f "$JAR" ]]; then
  echo "error: missing $JAR" >&2
  exit 1
fi

pkill -f 'RpcServer\.jar|RpcServerNative\.jar' 2>/dev/null || true
sleep 0.3

SERVER_LOG="$(mktemp -t julay-profile-server.XXXXXX)"
java -jar "$JAR" >"$SERVER_LOG" 2>&1 &
SERVER_PID=$!
cleanup() {
  kill "$SERVER_PID" 2>/dev/null || true
  wait "$SERVER_PID" 2>/dev/null || true
  [[ -n "${BENCH_PID:-}" ]] && kill "$BENCH_PID" 2>/dev/null || true
  rm -f "$SERVER_LOG"
}
trap cleanup EXIT

for _ in $(seq 1 80); do
  if curl -s -o /dev/null -m 1 -X POST "http://127.0.0.1:8000/rpc/get" 2>/dev/null; then
    break
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "error: server exited early; log:" >&2
    cat "$SERVER_LOG" >&2 || true
    exit 1
  fi
  sleep 0.1
done

echo "== warmup =="
python3 "$BENCH" \
  --url http://127.0.0.1:8000 \
  --target rpc \
  --ops "$WARMUP" \
  --clients "$CLIENTS" \
  --warmup 0 \
  --mode mix \
  --timeout "$TIMEOUT" >/dev/null || true

HTML_OUT="$OUT_DIR/${LABEL}-${MODE}.html"
COLLAPSED_OUT="$OUT_DIR/${LABEL}-${MODE}.collapsed"
EVENT="$MODE"

echo "== load + profile $LABEL mode=$MODE duration=${DURATION}s pid=$SERVER_PID =="
python3 "$BENCH" \
  --url http://127.0.0.1:8000 \
  --target rpc \
  --ops "$OPS" \
  --clients "$CLIENTS" \
  --warmup 0 \
  --mode mix \
  --timeout "$TIMEOUT" >"$OUT_DIR/${LABEL}-${MODE}-bench.txt" 2>&1 &
BENCH_PID=$!

# Collapsed stacks for bucket table
"$ASPROF" -d "$DURATION" -e "$EVENT" -o collapsed -f "$COLLAPSED_OUT" "$SERVER_PID"

# HTML flamegraph (second window; load should still be running)
if kill -0 "$BENCH_PID" 2>/dev/null; then
  "$ASPROF" -d "$DURATION" -e "$EVENT" -f "$HTML_OUT" "$SERVER_PID" || true
else
  echo "warn: load finished before HTML capture; starting another burst" >&2
  python3 "$BENCH" \
    --url http://127.0.0.1:8000 \
    --target rpc \
    --ops "$OPS" \
    --clients "$CLIENTS" \
    --warmup 0 \
    --mode mix \
    --timeout "$TIMEOUT" >"$OUT_DIR/${LABEL}-${MODE}-bench2.txt" 2>&1 &
  BENCH_PID=$!
  "$ASPROF" -d "$DURATION" -e "$EVENT" -f "$HTML_OUT" "$SERVER_PID" || true
fi

wait "$BENCH_PID" 2>/dev/null || true
BENCH_PID=""

SUMMARY_OUT="$OUT_DIR/${LABEL}-${MODE}-buckets.txt"
if [[ -f "$COLLAPSED_OUT" ]]; then
  python3 - "$COLLAPSED_OUT" "$SUMMARY_OUT" <<'PY'
import sys
from collections import defaultdict

path, out = sys.argv[1], sys.argv[2]
totals = defaultdict(int)
grand = 0

def bucket(stack: str) -> str:
    # Most-specific first (exclusive bucket per stack)
    if (
        "com/microsoft/z3" in stack
        or "com.microsoft.z3" in stack
        or "withEphemeralContext" in stack
        or "SyncResolveZ3" in stack
    ):
        return "Z3 / Context / SyncResolveZ3"
    if "SyncResolveFast" in stack:
        return "SyncResolveFast"
    if (
        "SyncChannel" in stack
        or "julay/concurrency/Select" in stack
        or "julay.concurrency.Select" in stack
    ):
        return "SyncChannel / Select"
    if (
        "invokeProcFun" in stack
        or "julay/program/Proc" in stack
        or "julay.program.Proc" in stack
        or "runOneStep" in stack
    ):
        return "invokeProcFun / Proc"
    if "JulHttpServer" in stack or "julay/program/library/Http" in stack:
        return "HTTP / JulHttpServer / bridge"
    if (
        "sun/net/httpserver" in stack
        or "sun.net.httpserver" in stack
        or "SocketDispatcher" in stack
    ):
        return "JDK HttpServer / NIO"
    if "RpcServerNative" in stack:
        return "native Protocol / handler"
    if (
        "kotlinx/coroutines" in stack
        or "LockSupport.park" in stack
        or "__psynch_cvwait" in stack
        or "__psynch_cvsignal" in stack
    ):
        return "thread park / wait"
    # JVM helper / idle threads (not application work)
    if (
        "semaphore_wait_trap" in stack
        or "AttachListener" in stack
        or "attach_listener" in stack
        or "signal_thread_entry" in stack
        or ("__ulock_wait" in stack and "julay" not in stack and "httpserver" not in stack)
    ):
        return "JVM idle / helper threads"
    if "GC" in stack or "[gc_" in stack or "PhaseChaitin" in stack or "PhaseIdealLoop" in stack:
        return "GC / JIT"
    return "Other"

with open(path, encoding="utf-8", errors="replace") as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        sp = line.rsplit(" ", 1)
        if len(sp) != 2 or not sp[1].isdigit():
            continue
        stack, n = sp[0], int(sp[1])
        grand += n
        totals[bucket(stack)] += n

rows = sorted(totals.items(), key=lambda kv: -kv[1])
idle = totals.get("JVM idle / helper threads", 0)
app_grand = grand - idle
with open(out, "w", encoding="utf-8") as o:
    o.write(f"total_samples={grand}\n")
    o.write(f"app_samples={app_grand}  (excludes JVM idle / helper threads)\n")
    o.write("--- all samples ---\n")
    for name, n in rows:
        pct = (100.0 * n / grand) if grand else 0.0
        o.write(f"{pct:6.2f}%  {n:8d}  {name}\n")
    o.write("--- among non-idle samples ---\n")
    for name, n in rows:
        if name == "JVM idle / helper threads":
            continue
        pct = (100.0 * n / app_grand) if app_grand else 0.0
        o.write(f"{pct:6.2f}%  {n:8d}  {name}\n")
print(open(out, encoding="utf-8").read())
PY
fi

echo
echo "artifacts:"
echo "  html:       $HTML_OUT"
echo "  collapsed:  $COLLAPSED_OUT"
echo "  buckets:    $SUMMARY_OUT"
echo "  bench log:  $OUT_DIR/${LABEL}-${MODE}-bench.txt"
echo "done."
