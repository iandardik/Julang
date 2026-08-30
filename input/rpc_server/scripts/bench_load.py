#!/usr/bin/env python3
"""Load client for Julay toy HTTP servers (echo / inc / rpc_server).

Honest local smoke benchmark — RPS + latency only. Not a PGo/YCSB comparison.

Examples:
  ./bench_load.py --target rpc --ops 100 --mode mix --clients 2
  ./bench_load.py --target echo --ops 200
  ./bench_load.py --target inc --ops 100 --warmup 20
"""

from __future__ import annotations

import argparse
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from typing import List, Optional, Tuple


LIMITATIONS = """\
LIMITATIONS (toy HTTP load — not comparable to PGo / etcd / YCSB):
  - Single-node localhost only; no multi-replica fan-out.
  - Julay path: JDK HttpServer + SyncChannel; rpc-native: same HttpServer + mutex.
  - rpc_server uses pipe-delimited text bodies (Raft-style); echo/inc use raw bodies.
  - This harness reports rough RPS and latency only; no baselines baked in.
"""


TARGET_DEFAULTS = {
    # (path, body_builder) — body_builder(wid, i) -> bytes
    "echo": {
        "increment": ("/", lambda wid, i: f"echo-w{wid}-{i}".encode()),
        "get": ("/", lambda wid, i: b"ping"),
        "add": ("/", lambda wid, i: f"add-w{wid}-{i}".encode()),
    },
    "inc": {
        # Inc ignores body; all paths hit /
        "increment": ("/", lambda wid, i: b""),
        "get": ("/", lambda wid, i: b""),
        "add": ("/", lambda wid, i: b""),
    },
    "rpc": {
        "increment": ("/rpc/increment", lambda wid, i: b""),
        "get": ("/rpc/get", lambda wid, i: b""),
        "add": ("/rpc/add", lambda wid, i: b"delta=1"),
    },
}


@dataclass
class Sample:
    op: str
    ok: bool
    latency_ms: float
    code: int
    detail: str = ""


@dataclass
class WorkerStats:
    samples: List[Sample] = field(default_factory=list)


def percentile(sorted_vals: List[float], p: float) -> float:
    if not sorted_vals:
        return float("nan")
    if len(sorted_vals) == 1:
        return sorted_vals[0]
    k = (len(sorted_vals) - 1) * (p / 100.0)
    f = int(k)
    c = min(f + 1, len(sorted_vals) - 1)
    if f == c:
        return sorted_vals[f]
    return sorted_vals[f] + (sorted_vals[c] - sorted_vals[f]) * (k - f)


def http_post(url: str, body: bytes, timeout: float) -> Tuple[int, bytes]:
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "text/plain")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.getcode() or 200, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read() if e.fp else b""


def do_op(
    base: str,
    target: str,
    op: str,
    wid: int,
    i: int,
    timeout: float,
) -> Sample:
    path, body_fn = TARGET_DEFAULTS[target][op]
    url = base.rstrip("/") + path
    body = body_fn(wid, i)
    t0 = time.perf_counter()
    try:
        code, raw = http_post(url, body, timeout)
    except Exception as e:
        ms = (time.perf_counter() - t0) * 1000.0
        return Sample(op, False, ms, 0, str(e))
    ms = (time.perf_counter() - t0) * 1000.0
    text = raw.decode("utf-8", errors="replace")
    ok = code == 200
    if ok and target == "rpc":
        ok = text.startswith("v=")
    return Sample(op, ok, ms, code, text[:80])


def pick_op(mode: str, i: int) -> str:
    if mode == "increment":
        return "increment"
    if mode == "get":
        return "get"
    if mode == "add":
        return "add"
    # mix: ~80% mutating (add/increment), ~20% get — plan default
    r = i % 10
    if r < 4:
        return "add"
    if r < 8:
        return "increment"
    return "get"


def worker(
    wid: int,
    base: str,
    target: str,
    ops: int,
    mode: str,
    timeout: float,
    stats: WorkerStats,
    barrier: threading.Barrier,
) -> None:
    barrier.wait()
    for i in range(ops):
        op = pick_op(mode, i)
        stats.samples.append(do_op(base, target, op, wid, i, timeout))


def summarize(all_samples: List[Sample], wall_s: float, label: str) -> None:
    total = len(all_samples)
    oks = [s for s in all_samples if s.ok]
    fails = [s for s in all_samples if not s.ok]
    rps = (len(oks) / wall_s) if wall_s > 0 else 0.0

    print()
    print(f"=== {label} ===")
    print(f"  ops_total={total}  ok={len(oks)}  fail={len(fails)}")
    print(f"  wall_seconds={wall_s:.3f}  throughput_ok_rps={rps:.3f}")

    by_op: dict[str, List[Sample]] = {}
    for s in all_samples:
        by_op.setdefault(s.op, []).append(s)

    for op, samples in sorted(by_op.items()):
        ok_lat = sorted(s.latency_ms for s in samples if s.ok)
        n_ok = len(ok_lat)
        n_fail = sum(1 for s in samples if not s.ok)
        print(f"  [{op}] ok={n_ok} fail={n_fail}")
        if ok_lat:
            print(
                f"    latency_ms: min={ok_lat[0]:.1f}  "
                f"p50={percentile(ok_lat, 50):.1f}  "
                f"p90={percentile(ok_lat, 90):.1f}  "
                f"p99={percentile(ok_lat, 99):.1f}  "
                f"max={ok_lat[-1]:.1f}  "
                f"mean={statistics.fmean(ok_lat):.1f}"
            )

    if fails:
        print("  sample failures (up to 5):")
        for s in fails[:5]:
            print(f"    {s.op} code={s.code} {s.latency_ms:.1f}ms detail={s.detail!r}")

    print()
    print(LIMITATIONS)


def run_load(
    base: str,
    target: str,
    ops: int,
    clients: int,
    mode: str,
    timeout: float,
    label: str,
) -> Tuple[List[Sample], float]:
    workers: List[WorkerStats] = [WorkerStats() for _ in range(clients)]
    barrier = threading.Barrier(clients + 1)
    threads = [
        threading.Thread(
            target=worker,
            args=(i, base, target, ops, mode, timeout, workers[i], barrier),
            daemon=True,
        )
        for i in range(clients)
    ]
    for t in threads:
        t.start()
    print(
        f"running target={target} mode={mode} clients={clients} "
        f"ops_per_client={ops} ..."
    )
    t0 = time.perf_counter()
    barrier.wait()
    for t in threads:
        t.join()
    wall = time.perf_counter() - t0
    all_samples: List[Sample] = []
    for w in workers:
        all_samples.extend(w.samples)
    summarize(all_samples, wall, label)
    return all_samples, wall


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Julay toy HTTP load client (echo / inc / rpc_server)"
    )
    p.add_argument(
        "--url",
        default="http://127.0.0.1:8000",
        help="server base URL (default http://127.0.0.1:8000)",
    )
    p.add_argument(
        "--target",
        choices=("echo", "inc", "rpc"),
        default="rpc",
        help="which toy API shape to hit (default rpc)",
    )
    p.add_argument("--ops", type=int, default=50, help="ops per client (default 50)")
    p.add_argument(
        "--clients", type=int, default=1, help="concurrent clients (default 1)"
    )
    p.add_argument(
        "--mode",
        choices=("increment", "get", "add", "mix"),
        default="mix",
        help="workload (default mix: ~80%% mutate / ~20%% get)",
    )
    p.add_argument(
        "--warmup",
        type=int,
        default=10,
        help="warmup ops per client before timed run (default 10; 0 to skip)",
    )
    p.add_argument(
        "--timeout",
        type=float,
        default=30.0,
        help="per-request HTTP timeout seconds (default 30)",
    )
    return p.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    if args.ops < 1 or args.clients < 1:
        print("error: --ops and --clients must be >= 1", file=sys.stderr)
        return 2
    if args.warmup < 0:
        print("error: --warmup must be >= 0", file=sys.stderr)
        return 2

    base = args.url.rstrip("/")

    # Probe
    try:
        code, raw = http_post(base + "/", b"probe", min(args.timeout, 5.0))
        # echo/inc answer /; rpc may 404 on / — either means server is up
        _ = (code, raw)
    except Exception:
        # rpc default path isn't /; try rpc/get
        try:
            http_post(base + "/rpc/get", b"", min(args.timeout, 5.0))
        except Exception as e:
            print(f"error: cannot reach {base}: {e}", file=sys.stderr)
            return 1

    if args.warmup > 0:
        print(f"warmup ops_per_client={args.warmup} ...")
        run_load(
            base,
            args.target,
            args.warmup,
            args.clients,
            args.mode,
            args.timeout,
            label="warmup (discarded)",
        )

    samples, _wall = run_load(
        base,
        args.target,
        args.ops,
        args.clients,
        args.mode,
        args.timeout,
        label=f"results target={args.target}",
    )
    return 0 if all(s.ok for s in samples) else 1


if __name__ == "__main__":
    sys.exit(main())
