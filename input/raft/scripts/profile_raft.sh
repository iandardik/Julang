#!/usr/bin/env bash
# Profile a localhost Julay Raft cluster under HTTP client load with async-profiler.
#
# Starts the 3-node cluster, discovers the leader, runs sustained bench_load.py, and
# attaches asprof to the *leader* JVM (where append/AE work concentrates).
#
# Prerequisites: JDK 18+. async-profiler is auto-fetched into tools/async-profiler unless
# ASYNC_PROFILER_HOME or --async-profiler-home is set. Rebuilds RaftNode.jar by default.
#
# Examples:
#   ./profile_raft.sh --mode alloc --duration 12 --clients 4 --ops 2000
#   ./profile_raft.sh --mode cpu --duration 15 --clients 2 --ops 1500 --bench-mode mix
#   ./profile_raft.sh --skip-build   # reuse existing RaftNode.jar
#
# Artifacts land in profile-out/ (gitignored): raft-leader-{mode}.html/.collapsed/-buckets.txt/-bench.txt
#
# LIMITATIONS: Tier-0 localhost only — not comparable to PGo / etcd / YCSB. Profiles one
# leader process; followers still run heartbeats/AE receive but are not sampled unless
# you re-run with --profile-all.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RAFT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT="$(cd "$RAFT_DIR/../.." && pwd)"
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
export JAVA

MODE=alloc
DURATION=12
CLIENTS=4
WARMUP_OPS=40
OPS=2000
TIMEOUT=60
BENCH_MODE=append
CONFIG="$RAFT_DIR/cluster.conf"
OUT_DIR="$SCRIPT_DIR/profile-out"
ASYNC_HOME="${ASYNC_PROFILER_HOME:-}"
SKIP_BUILD=0
PROFILE_ALL=0
LISTEN_TIMEOUT_S=40
ELECTION_WAIT_S=3

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) MODE="$2"; shift 2 ;;
    --duration) DURATION="$2"; shift 2 ;;
    --clients) CLIENTS="$2"; shift 2 ;;
    --warmup) WARMUP_OPS="$2"; shift 2 ;;
    --ops) OPS="$2"; shift 2 ;;
    --timeout) TIMEOUT="$2"; shift 2 ;;
    --bench-mode) BENCH_MODE="$2"; shift 2 ;;
    --config) CONFIG="$2"; shift 2 ;;
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    --async-profiler-home) ASYNC_HOME="$2"; shift 2 ;;
    --skip-build) SKIP_BUILD=1; shift ;;
    --profile-all) PROFILE_ALL=1; shift ;;
    -h|--help)
      sed -n '2,20p' "$0"
      exit 0
      ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

case "$MODE" in
  cpu|alloc|wall) ;;
  *) echo "error: --mode must be cpu|alloc|wall" >&2; exit 2 ;;
esac
case "$BENCH_MODE" in
  append|get|mix) ;;
  *) echo "error: --bench-mode must be append|get|mix" >&2; exit 2 ;;
esac

TOOLS_DIR="$ROOT/tools"
AP_DIR="$TOOLS_DIR/async-profiler"
BENCH="$SCRIPT_DIR/bench_load.py"
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
    exit 1
  fi
  echo "$asprof"
}

ASPROF="$(ensure_async_profiler)"
echo "using asprof=$ASPROF"

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  echo "== building julayc + RaftNode.jar =="
  ./gradlew shadowJar -q
  rm -f "$ROOT/RaftNode.jar" "$RAFT_DIR/RaftNode.jar"
  "$JAVA" -jar "$ROOT/build/libs/julayc.jar" "$RAFT_DIR/sys.jul"
  if [[ ! -f "$ROOT/RaftNode.jar" ]]; then
    echo "error: expected RaftNode.jar after compiling $RAFT_DIR/sys.jul" >&2
    exit 1
  fi
  cp -f "$ROOT/RaftNode.jar" "$RAFT_DIR/RaftNode.jar"
fi
export RAFT_NODE_JAR="${RAFT_NODE_JAR:-$ROOT/RaftNode.jar}"

# Parse URLs from config
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

port_from_url() {
  echo "$1" | sed -E 's|^[a-zA-Z]+://[^:/]+:([0-9]+).*|\1|'
}

PORTS=()
for url in "${URLS[@]}"; do
  PORTS+=("$(port_from_url "$url")")
done

PIDFILE="$(dirname "$CONFIG")/.raft-pids"
cleanup() {
  [[ -n "${BENCH_PID:-}" ]] && kill "$BENCH_PID" 2>/dev/null || true
  "$SCRIPT_DIR/stop_cluster.sh" "$CONFIG" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "== stop any previous cluster =="
"$SCRIPT_DIR/stop_cluster.sh" "$CONFIG" || true

echo "== start cluster =="
"$SCRIPT_DIR/start_cluster.sh" "$CONFIG"

echo "== wait for listen (≤${LISTEN_TIMEOUT_S}s) =="
ready=0
for ((i = 1; i <= LISTEN_TIMEOUT_S; i++)); do
  ok=0
  for port in "${PORTS[@]}"; do
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      ok=$((ok + 1))
    fi
  done
  if [[ "$ok" -eq "$N" ]]; then
    echo "all $N ports listening at ${i}s"
    ready=1
    break
  fi
  sleep 1
done
if [[ "$ready" -ne 1 ]]; then
  echo "error: only $ok/$N ports listening after ${LISTEN_TIMEOUT_S}s" >&2
  exit 1
fi

echo "== wait ${ELECTION_WAIT_S}s for election =="
sleep "$ELECTION_WAIT_S"

SEED_URL="${URLS[0]}"
echo "== discover leader from $SEED_URL =="
LEADER_URL="$(
  python3 - "$SEED_URL" "$TIMEOUT" <<'PY'
import sys
sys.path.insert(0, "")
# Inline discover to avoid importing bench_load as module path issues
import time, urllib.request, urllib.error

def http_post(url, body, timeout):
    req = urllib.request.Request(url, data=body, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.getcode(), resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read() if e.fp else b""

seed = sys.argv[1].rstrip("/")
timeout = float(sys.argv[2])
probe = b"__profile_leader_probe__"
for _ in range(40):
    try:
        code, raw = http_post(f"{seed}/client/append", probe, timeout)
    except Exception:
        time.sleep(0.4)
        continue
    text = raw.decode("utf-8", errors="replace")
    if code == 303 and text.startswith("LEADER "):
        print(text.split(" ", 1)[1].strip().rstrip("/"))
        sys.exit(0)
    if code == 200:
        print(seed)
        sys.exit(0)
    time.sleep(0.4)
print("error: could not discover leader", file=sys.stderr)
sys.exit(1)
PY
)"
echo "leader_url=$LEADER_URL"

# Map leader URL → node index → PID
LEADER_IDX=-1
norm_leader="$(echo "$LEADER_URL" | sed 's|/*$||')"
for i in "${!URLS[@]}"; do
  norm="$(echo "${URLS[$i]}" | sed 's|/*$||')"
  if [[ "$norm" == "$norm_leader" ]]; then
    LEADER_IDX=$((i + 1))
    break
  fi
done
if [[ "$LEADER_IDX" -lt 1 ]]; then
  echo "error: leader URL $LEADER_URL not in config" >&2
  exit 1
fi
PIDS=()
while IFS= read -r _pid || [[ -n "$_pid" ]]; do
  [[ -z "$_pid" ]] && continue
  PIDS+=("$_pid")
done < "$PIDFILE"
LEADER_PID="${PIDS[$((LEADER_IDX - 1))]}"
if ! kill -0 "$LEADER_PID" 2>/dev/null; then
  echo "error: leader pid $LEADER_PID not alive" >&2
  exit 1
fi
echo "leader_node=$LEADER_IDX pid=$LEADER_PID"

LABEL="raft-leader"
HTML_OUT="$OUT_DIR/${LABEL}-${MODE}.html"
COLLAPSED_OUT="$OUT_DIR/${LABEL}-${MODE}.collapsed"
BENCH_OUT="$OUT_DIR/${LABEL}-${MODE}-bench.txt"
SUMMARY_OUT="$OUT_DIR/${LABEL}-${MODE}-buckets.txt"

echo "== warmup ($WARMUP_OPS ops × $CLIENTS clients) via $LEADER_URL =="
python3 "$BENCH" \
  --url "$LEADER_URL" \
  --ops "$WARMUP_OPS" \
  --clients "$CLIENTS" \
  --mode "$BENCH_MODE" \
  --timeout "$TIMEOUT" \
  --skip-leader-wait \
  --value-prefix "raft-prof-warm-$(date +%s)"

echo "== load + profile leader mode=$MODE duration=${DURATION}s ops=$OPS clients=$CLIENTS =="
python3 "$BENCH" \
  --url "$LEADER_URL" \
  --ops "$OPS" \
  --clients "$CLIENTS" \
  --mode "$BENCH_MODE" \
  --timeout "$TIMEOUT" \
  --skip-leader-wait \
  --value-prefix "raft-prof-$(date +%s)" >"$BENCH_OUT" 2>&1 &
BENCH_PID=$!
# Let clients start issuing requests before attaching the profiler.
sleep 2
if ! kill -0 "$BENCH_PID" 2>/dev/null; then
  echo "error: bench exited before profiling; log:" >&2
  cat "$BENCH_OUT" >&2 || true
  exit 1
fi

profile_one() {
  local pid="$1"
  local tag="$2"
  "$ASPROF" -d "$DURATION" -e "$MODE" -o collapsed -f "$OUT_DIR/${tag}-${MODE}.collapsed" "$pid"
  if kill -0 "$BENCH_PID" 2>/dev/null; then
    "$ASPROF" -d "$DURATION" -e "$MODE" -f "$OUT_DIR/${tag}-${MODE}.html" "$pid" || true
  fi
}

profile_one "$LEADER_PID" "$LABEL"

if [[ "$PROFILE_ALL" -eq 1 ]]; then
  for i in "${!PIDS[@]}"; do
    pid="${PIDS[$i]}"
    idx=$((i + 1))
    if [[ "$idx" -eq "$LEADER_IDX" ]]; then
      continue
    fi
    if kill -0 "$pid" 2>/dev/null && kill -0 "$BENCH_PID" 2>/dev/null; then
      echo "== also profiling follower node=$idx pid=$pid =="
      profile_one "$pid" "raft-node${idx}"
    fi
  done
fi

wait "$BENCH_PID" 2>/dev/null || true
BENCH_PID=""

bucket_collapsed() {
  local collapsed="$1"
  local summary="$2"
  python3 - "$collapsed" "$summary" <<'PY'
import sys
from collections import defaultdict

path, out = sys.argv[1], sys.argv[2]
totals = defaultdict(int)
grand = 0

def bucket(stack: str) -> str:
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
        or "SelectCoordinator" in stack
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
    if (
        "kotlinx/coroutines" in stack
        or "LockSupport.park" in stack
        or "__psynch_cvwait" in stack
        or "__psynch_cvsignal" in stack
    ):
        return "thread park / wait"
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
}

if [[ -f "$COLLAPSED_OUT" ]]; then
  bucket_collapsed "$COLLAPSED_OUT" "$SUMMARY_OUT"
fi

echo
echo "artifacts:"
echo "  html:       $HTML_OUT"
echo "  collapsed:  $COLLAPSED_OUT"
echo "  buckets:    $SUMMARY_OUT"
echo "  bench log:  $BENCH_OUT"
echo "  leader:     node=$LEADER_IDX pid=$LEADER_PID url=$LEADER_URL"
if [[ -f "$BENCH_OUT" ]]; then
  echo
  echo "=== bench summary ==="
  grep -E 'ops_total=|throughput_ok_rps=|wall_seconds=|\[append\]|\[get\]|ok=' "$BENCH_OUT" | head -20 || true
fi
echo "done."
