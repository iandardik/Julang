#!/usr/bin/env python3
"""Tier-0 Raft load client: HTTP append/get with latency + RPS reporting.

Hits the existing /client/append and /client/get endpoints. Follows 303
LEADER redirects on append. This is an honest local smoke benchmark — not
comparable to PGo/etcd/YCSB (see LIMITATIONS printed at the end).

Examples:
  ./bench_load.py --url http://127.0.0.1:5001 --ops 20 --mode append
  ./bench_load.py --url http://127.0.0.1:5001 --ops 50 --mode mix --clients 2
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
LIMITATIONS (Tier 0 — not comparable to PGo / etcd / YCSB):
  - Election timeouts are ~1–2s (ms timers); heartbeats are ~200ms.
  - AppendEntries carries at most one entry; AE fan-out uses an outbound RPC pool.
  - Log / term state is in-memory only (no durability).
  - RPC is JDK HTTP with a pipe-delimited text protocol.
  - This harness reports rough RPS and latency only; no YCSB, no baselines.
"""


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


def discover_leader(
    seed_url: str, timeout: float, max_attempts: int = 30
) -> Optional[str]:
    """Learn the leader via /client/append.

    Hitting a follower returns 303 LEADER <url> without committing. Hitting the
    leader may commit a one-shot probe value (named clearly in the log).
    """
    base = seed_url.rstrip("/")
    probe = b"__bench_leader_probe__"
    for _ in range(max_attempts):
        try:
            code, raw = http_post(f"{base}/client/append", probe, timeout)
        except Exception:
            time.sleep(0.5)
            continue
        text = raw.decode("utf-8", errors="replace")
        if code == 303 and text.startswith("LEADER "):
            return text.split(" ", 1)[1].strip().rstrip("/")
        if code == 200:
            return base
        if code == 503:
            time.sleep(0.5)
            continue
        time.sleep(0.5)
    return None


def do_append(leader_url: str, value: str, timeout: float) -> Tuple[Sample, str]:
    """Append with one redirect retry. Returns (sample, current_leader_url)."""
    target = leader_url.rstrip("/")
    body = value.encode("utf-8")
    t0 = time.perf_counter()
    try:
        code, raw = http_post(f"{target}/client/append", body, timeout)
    except Exception as e:
        ms = (time.perf_counter() - t0) * 1000.0
        return Sample("append", False, ms, 0, str(e)), target

    text = raw.decode("utf-8", errors="replace")
    if code == 303 and text.startswith("LEADER "):
        target = text.split(" ", 1)[1].strip().rstrip("/")
        try:
            code, raw = http_post(f"{target}/client/append", body, timeout)
        except Exception as e:
            ms = (time.perf_counter() - t0) * 1000.0
            return Sample("append", False, ms, 0, f"retry: {e}"), target
        text = raw.decode("utf-8", errors="replace")

    ms = (time.perf_counter() - t0) * 1000.0
    ok = code == 200
    return Sample("append", ok, ms, code, text[:80]), target if ok else leader_url


def do_get(url: str, timeout: float) -> Sample:
    target = url.rstrip("/")
    t0 = time.perf_counter()
    try:
        code, raw = http_post(f"{target}/client/get", b"", timeout)
    except Exception as e:
        ms = (time.perf_counter() - t0) * 1000.0
        return Sample("get", False, ms, 0, str(e))
    ms = (time.perf_counter() - t0) * 1000.0
    text = raw.decode("utf-8", errors="replace")
    return Sample("get", code == 200, ms, code, text[:80])


def worker(
    wid: int,
    seed_url: str,
    leader_url: str,
    ops: int,
    mode: str,
    timeout: float,
    value_prefix: str,
    stats: WorkerStats,
    barrier: threading.Barrier,
) -> None:
    barrier.wait()
    current_leader = leader_url
    for i in range(ops):
        if mode == "append":
            sample, current_leader = do_append(
                current_leader, f"{value_prefix}-w{wid}-{i}", timeout
            )
            stats.samples.append(sample)
        elif mode == "get":
            stats.samples.append(do_get(seed_url, timeout))
        else:  # mix: alternate append / get
            if i % 2 == 0:
                sample, current_leader = do_append(
                    current_leader, f"{value_prefix}-w{wid}-{i}", timeout
                )
                stats.samples.append(sample)
            else:
                stats.samples.append(do_get(seed_url, timeout))


def summarize(all_samples: List[Sample], wall_s: float) -> None:
    total = len(all_samples)
    oks = [s for s in all_samples if s.ok]
    fails = [s for s in all_samples if not s.ok]
    rps = (len(oks) / wall_s) if wall_s > 0 else 0.0

    print()
    print("=== Tier 0 results ===")
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


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Tier-0 Julang Raft HTTP load client")
    p.add_argument(
        "--url",
        default="http://127.0.0.1:5001",
        help="seed node URL (followers redirect append to leader)",
    )
    p.add_argument("--ops", type=int, default=20, help="ops per client (default 20)")
    p.add_argument("--clients", type=int, default=1, help="concurrent clients (default 1)")
    p.add_argument(
        "--mode",
        choices=("append", "get", "mix"),
        default="append",
        help="workload: append | get | mix (default append)",
    )
    p.add_argument(
        "--timeout",
        type=float,
        default=60.0,
        help="per-request HTTP timeout seconds (default 60)",
    )
    p.add_argument(
        "--value-prefix",
        default="bench",
        help="prefix for appended values (default bench)",
    )
    p.add_argument(
        "--skip-leader-wait",
        action="store_true",
        help="do not probe for a leader before starting load",
    )
    return p.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    if args.ops < 1 or args.clients < 1:
        print("error: --ops and --clients must be >= 1", file=sys.stderr)
        return 2

    seed = args.url.rstrip("/")
    leader = seed
    if not args.skip_leader_wait:
        print(f"discovering leader via {seed} ...")
        found = discover_leader(seed, min(args.timeout, 10.0))
        if found is None:
            print(
                "error: could not discover a leader (is the cluster up and elected?)",
                file=sys.stderr,
            )
            return 1
        leader = found
        print(f"leader={leader}")

    workers: List[WorkerStats] = [WorkerStats() for _ in range(args.clients)]
    barrier = threading.Barrier(args.clients + 1)
    threads = [
        threading.Thread(
            target=worker,
            args=(
                i,
                seed,
                leader,
                args.ops,
                args.mode,
                args.timeout,
                args.value_prefix,
                workers[i],
                barrier,
            ),
            daemon=True,
        )
        for i in range(args.clients)
    ]
    for t in threads:
        t.start()

    print(
        f"running mode={args.mode} clients={args.clients} ops_per_client={args.ops} ..."
    )
    t0 = time.perf_counter()
    barrier.wait()  # release all workers together
    for t in threads:
        t.join()
    wall = time.perf_counter() - t0

    all_samples: List[Sample] = []
    for w in workers:
        all_samples.extend(w.samples)
    summarize(all_samples, wall)
    return 0 if all(s.ok for s in all_samples) else 1


if __name__ == "__main__":
    sys.exit(main())
