#!/usr/bin/env bash
# Profile no-HTTP SyncChannel / Select rendezvous microbench with async-profiler.
#
# Examples:
#   ./profile_rendezvous.sh --mode syncfast --event alloc --duration 12 --seconds 40
#   ./profile_rendezvous.sh --mode select3 --event cpu --duration 12 --seconds 40
#
# Artifacts in profile-out/: rendezvous-{mode}-{event}.{collapsed,html} + buckets-fine.txt
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$ROOT"

if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
  export JAVA_HOME
  [[ -n "$JAVA_HOME" ]] && export PATH="$JAVA_HOME/bin:$PATH"
fi
if [[ -z "${JAVA:-}" ]]; then
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA="$JAVA_HOME/bin/java"
  else
    JAVA=java
  fi
fi

BENCH_MODE=syncfast
EVENT=alloc
DURATION=12
SECONDS_RUN=40
OUT_DIR="$SCRIPT_DIR/profile-out"
ASYNC_HOME="${ASYNC_PROFILER_HOME:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) BENCH_MODE="$2"; shift 2 ;;
    --event|--profile-mode) EVENT="$2"; shift 2 ;;
    --duration) DURATION="$2"; shift 2 ;;
    --seconds) SECONDS_RUN="$2"; shift 2 ;;
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    --async-profiler-home) ASYNC_HOME="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,12p' "$0"
      exit 0
      ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

case "$BENCH_MODE" in
  syncfast|select3) ;;
  *) echo "error: --mode must be syncfast|select3" >&2; exit 2 ;;
esac
case "$EVENT" in
  cpu|alloc|wall) ;;
  *) echo "error: --event must be cpu|alloc|wall" >&2; exit 2 ;;
esac

TOOLS_DIR="$ROOT/tools"
AP_DIR="$TOOLS_DIR/async-profiler"
BUCKET_PY="$SCRIPT_DIR/bucket_collapsed.py"
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
  local nested
  nested="$(find "$AP_DIR" -type f -name asprof 2>/dev/null | head -1 || true)"
  if [[ -n "$nested" && -x "$nested" ]]; then
    echo "$nested"
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
  # Reuse fetch logic via profile_rpc if present; otherwise fail with hint.
  if [[ -x "$SCRIPT_DIR/profile_rpc.sh" ]]; then
    echo "error: async-profiler missing; run profile_rpc.sh once to fetch it, or set ASYNC_PROFILER_HOME" >&2
  else
    echo "error: async-profiler missing; set ASYNC_PROFILER_HOME" >&2
  fi
  exit 1
}

ASPROF="$(ensure_async_profiler)"
echo "using asprof=$ASPROF"

echo "== compile runtime classpath =="
./gradlew -q classes writeRuntimeClasspath
CP="$(cat build/runtime-classpath.txt)"
if [[ -z "$CP" ]]; then
  echo "error: empty runtime classpath" >&2
  exit 1
fi

LABEL="rendezvous-${BENCH_MODE}-${EVENT}"
COLLAPSED_OUT="$OUT_DIR/${LABEL}.collapsed"
HTML_OUT="$OUT_DIR/${LABEL}.html"
FINE_OUT="$OUT_DIR/${LABEL}-buckets-fine.txt"
BENCH_LOG="$OUT_DIR/${LABEL}-bench.txt"

echo "== start microbench mode=$BENCH_MODE seconds=$SECONDS_RUN =="
"$JAVA" -cp "$CP" julay.bench.RendezvousMicrobenchKt \
  --mode "$BENCH_MODE" \
  --seconds "$SECONDS_RUN" >"$BENCH_LOG" 2>&1 &
BENCH_PID=$!
cleanup() {
  kill "$BENCH_PID" 2>/dev/null || true
  wait "$BENCH_PID" 2>/dev/null || true
}
trap cleanup EXIT

# Wait until banner with pid is printed (or process dies).
for _ in $(seq 1 100); do
  if grep -q 'rendezvous_microbench mode=' "$BENCH_LOG" 2>/dev/null; then
    break
  fi
  if ! kill -0 "$BENCH_PID" 2>/dev/null; then
    echo "error: microbench exited early; log:" >&2
    cat "$BENCH_LOG" >&2 || true
    exit 1
  fi
  sleep 0.05
done

# Prefer JVM pid from banner (more reliable than gradle wrapper).
BENCH_JVM_PID="$(grep -E 'rendezvous_microbench mode=' "$BENCH_LOG" | head -1 | sed -E 's/.*pid=([0-9]+).*/\1/')"
if [[ -z "$BENCH_JVM_PID" ]]; then
  BENCH_JVM_PID="$BENCH_PID"
fi
echo "profiling pid=$BENCH_JVM_PID event=$EVENT duration=${DURATION}s"

sleep 1
"$ASPROF" -d "$DURATION" -e "$EVENT" -o collapsed -f "$COLLAPSED_OUT" "$BENCH_JVM_PID"
if kill -0 "$BENCH_JVM_PID" 2>/dev/null; then
  "$ASPROF" -d "$DURATION" -e "$EVENT" -f "$HTML_OUT" "$BENCH_JVM_PID" || true
fi

# Do not wait out the full microbench window — profiling is done.
kill "$BENCH_JVM_PID" 2>/dev/null || true
wait "$BENCH_PID" 2>/dev/null || true
trap - EXIT

if [[ -f "$COLLAPSED_OUT" && -s "$COLLAPSED_OUT" ]]; then
  python3 "$BUCKET_PY" "$COLLAPSED_OUT" "$FINE_OUT" --fine --keywords
else
  echo "warn: empty or missing collapsed file: $COLLAPSED_OUT" >&2
fi

echo
echo "artifacts:"
echo "  collapsed: $COLLAPSED_OUT"
echo "  html:      $HTML_OUT"
echo "  fine:      $FINE_OUT"
echo "  bench log: $BENCH_LOG"
if [[ -f "$BENCH_LOG" ]]; then
  echo
  echo "=== bench log (tail) ==="
  tail -5 "$BENCH_LOG" || true
fi
echo "done."
