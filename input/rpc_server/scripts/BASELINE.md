# Toy HTTP baselines (echo / inc / rpc_server)

Local smoke numbers from `bench_toys.sh` on 2026-08-29 (macOS, JDK 18).
Not comparable to PGo/YCSB.

## Results (opts default vs `--disable-opt`)

Config: `--ops 80 --clients 2 --warmup 20 --mode mix`.

| Target | RPS (opts on) | p50 (opts on) | RPS (opts off) | p50 (opts off) |
|--------|---------------|---------------|----------------|----------------|
| echo   | ~1780         | ~1.1 ms       | ~125           | ~16 ms         |
| inc    | ~1100         | ~1.7 ms       | ~22            | ~90 ms         |
| rpc    | ~1370         | ~1.4 ms       | ~27            | ~74 ms         |

Notes:

- Named sync opts (`eq-unify`, `arg-ownership`, `directed-eval`) give roughly **50×** on inc/rpc.
- `rpc_server` is FastOnly on Protocol + handlers (`julayc --verbose`); only `listen` stays NeedsZ3 (startup).
- `rpc` can beat `inc` on this mix because each RPC is **one** core sync (value returned on the same action); `inc` does `increment` then `getCounter`, and also runs a Printer peer.
- Echo still pays HttpServer/session machinery; with opts off even echo drops hard (residual/enablement Z3 on the HTTP path).

## Julay rpc vs Kotlin-native twin

Config: `--targets rpc,rpc-native --ops 200 --clients 4 --warmup 40 --mode mix`.
Same wire API and JDK `HttpServer`; native uses a mutex instead of SyncChannel
([`native/`](../native/)).

| Implementation | ok RPS | p50 latency |
|----------------|--------|-------------|
| Julay rpc (opts on) | ~2320 | ~1.6 ms |
| Kotlin native       | ~5320 | ~0.7 ms |

Julay is about **~2.3×** lower throughput and **~2×** higher median latency than the
hand-written twin on this machine/load. That gap is the current runtime floor target
for `rpc_server` work (close toward 1×, or at least within ~2× as a soft bar).

CPU/alloc flamegraphs and a bucketed hotspot table: [`PROFILE.md`](PROFILE.md).

## Runtime work implied by these numbers

Already landed and validated: named sync FastOnly path (huge win vs Z3-only).

Still open (do **not** need a new distributed system yet):

1. Close the **~2×** Julay vs Kotlin-native gap on `rpc_server` — profiling points at **procfun spawn (`invokeProcFun`) + SyncChannel**, not Z3 ([`PROFILE.md`](PROFILE.md)).
2. Residual Z3 on opaque session `listen` shapes (startup only today for rpc).
3. Quieter echo (harness log redirect — done in `bench_toys.sh`) so the HTTP floor is not println-bound when watching a TTY.

## Decision (decide-next)

**Stay on toys.** Sync opts already move the needle dramatically; `rpc_server` + Kotlin twin is the right loop. Do **not** build TOB/chain yet. Return to Raft after the Julay↔native gap is in a credible band.
