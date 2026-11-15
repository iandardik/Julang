#!/usr/bin/env python3
"""Tier-0 Sharded KV load client: HTTP put/get with latency + RPS reporting.

Hits /kv/put and /kv/get (pipe-delimited bodies). Follows one 303 OWNER redirect
like ShardClient. Local smoke benchmark — not YCSB / not paper-comparable to
PGo Azure numbers (see LIMITATIONS).

Examples:
  ./bench_load.py --url http://127.0.0.1:6001 --ops 200 --mode put
  ./bench_load.py --url http://127.0.0.1:6001 --ops 200 --mode mix --clients 4
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
LIMITATIONS (Tier 0 — preliminary sanity check only):
  - Localhost 3-node SKV; in-memory maps; no durability; HTTP + pipe text protocol.
  - Ownership broadcast + optional load reshard on put (unique keys can trigger moves).
  - Not YCSB; not Azure; not a fair match to PGo-RaftKV (consensus + durable log).
  - Sanity claim: SKV should beat consensus Raft on same machine for simple Put/Get
    because it does not replicate every write through a leader quorum.
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


def follow_owner(
    seed_url: str, path: str, body: bytes, timeout: float
) -> Tuple[int, str, float]:
    """POST once; on 303 OWNER <url>, retry once against that URL. Returns code, text, ms."""
    target = seed_url.rstrip("/")
    t0 = time.perf_counter()
    try:
        code, raw = http_post(f"{target}{path}", body, timeout)
    except Exception as e:
        ms = (time.perf_counter() - t0) * 1000.0
        return 0, str(e), ms
    text = raw.decode("utf-8", errors="replace")
    if code == 303 and text.startswith("OWNER "):
        target = text.split(" ", 1)[1].strip().rstrip("/")
        try:
            code, raw = http_post(f"{target}{path}", body, timeout)
        except Exception as e:
            ms = (time.perf_counter() - t0) * 1000.0
            return 0, f"retry: {e}", ms
        text = raw.decode("utf-8", errors="replace")
    ms = (time.perf_counter() - t0) * 1000.0
    return code, text, ms


def do_put(url: str, key: str, value: str, timeout: float) -> Sample:
    body = f"k={key}|v={value}".encode("utf-8")
    code, text, ms = follow_owner(url, "/kv/put", body, timeout)
    return Sample("put", code == 200 and "ok" in text, ms, code, text[:80])


def do_get(url: str, key: str, timeout: float) -> Sample:
    body = f"k={key}".encode("utf-8")
    code, text, ms = follow_owner(url, "/kv/get", body, timeout)
    ok = code == 200 and text.startswith("v=")
    return Sample("get", ok, ms, code, text[:80])


def worker(
    wid: int,
    seed_url: str,
    ops: int,
    mode: str,
    timeout: float,
    key_prefix: str,
    stats: WorkerStats,
    barrier: threading.Barrier,
) -> None:
    barrier.wait()
    for i in range(ops):
        key = f"{key_prefix}-w{wid}-{i}"
        if mode == "put":
            stats.samples.append(do_put(seed_url, key, f"v-{wid}-{i}", timeout))
        elif mode == "get":
            # Expect caller to have primed keys; miss counts as fail.
            stats.samples.append(do_get(seed_url, key, timeout))
        elif mode == "putget":
            stats.samples.append(do_put(seed_url, key, f"v-{wid}-{i}", timeout))
            stats.samples.append(do_get(seed_url, key, timeout))
        else:  # mix: alternate put / get of prior key
            if i % 2 == 0:
                stats.samples.append(do_put(seed_url, key, f"v-{wid}-{i}", timeout))
            else:
                prev = f"{key_prefix}-w{wid}-{i - 1}"
                stats.samples.append(do_get(seed_url, prev, timeout))


def summarize(all_samples: List[Sample], wall_s: float) -> None:
    total = len(all_samples)
    oks = [s for s in all_samples if s.ok]
    fails = [s for s in all_samples if not s.ok]
    rps = (len(oks) / wall_s) if wall_s > 0 else 0.0

    print()
    print("=== Tier 0 SKV results ===")
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
    p = argparse.ArgumentParser(description="Tier-0 Sharded KV HTTP load client")
    p.add_argument("--url", default="http://127.0.0.1:6001", help="seed node URL")
    p.add_argument("--ops", type=int, default=100, help="ops per client (default 100)")
    p.add_argument("--clients", type=int, default=1, help="concurrent clients (default 1)")
    p.add_argument(
        "--mode",
        choices=("put", "get", "mix", "putget"),
        default="put",
        help="workload: put | get | mix | putget (default put)",
    )
    p.add_argument("--timeout", type=float, default=10.0, help="per-request timeout s")
    p.add_argument("--key-prefix", default="bench", help="key prefix")
    return p.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    if args.ops < 1 or args.clients < 1:
        print("error: --ops and --clients must be >= 1", file=sys.stderr)
        return 2

    seed = args.url.rstrip("/")
    if args.mode == "get":
        print("priming keys with puts ...")
        for c in range(args.clients):
            for i in range(args.ops):
                key = f"{args.key_prefix}-w{c}-{i}"
                s = do_put(seed, key, f"prime-{c}-{i}", args.timeout)
                if not s.ok:
                    print(f"error: prime put failed for {key}: {s}", file=sys.stderr)
                    return 1

    workers: List[WorkerStats] = [WorkerStats() for _ in range(args.clients)]
    barrier = threading.Barrier(args.clients + 1)
    threads = [
        threading.Thread(
            target=worker,
            args=(
                i,
                seed,
                args.ops,
                args.mode,
                args.timeout,
                args.key_prefix,
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
    barrier.wait()
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
