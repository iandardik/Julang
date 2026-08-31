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
| Julay rpc (opts on) | ~3120 | ~1.1 ms |
| Kotlin native       | ~4870 | ~0.8 ms |

Julay is about **~1.56×** lower throughput on this short bench (noisy; Phase 5 was ~1.45×). Sustained load (`--ops 5000`, same clients/warmup): Julay **~4910** vs native **~5830** RPS (**~1.19×**). Phase 5 cold was Jul~2990** vs **~4400** (~1.47×).

Phase 6 reused multi-offer Select setup + opportunistic all-size-2 lead; sustained band unchanged (see [`PROFILE.md`](PROFILE.md) Phase 6). Remaining gap is SyncChannel rendezvous vs native mutex.

CPU/alloc flamegraphs and a bucketed hotspot table: [`PROFILE.md`](PROFILE.md).

## Runtime work implied by these numbers

Already landed and validated: named sync FastOnly path (huge win vs Z3-only).

Still open (do **not** need a new distributed system yet):

1. Close the remaining **~1.2–1.5×** Julay vs Kotlin-native gap on `rpc_server` — SyncChannel rendezvous depth after Phase 6 setup reuse ([`PROFILE.md`](PROFILE.md) Phase 6–7).
2. Residual Z3 on opaque session `listen` shapes (startup only today for rpc).
3. Quieter echo (harness log redirect — done in `bench_toys.sh`) so the HTTP floor is not println-bound when watching a TTY.

## Decision (decide-next)

**Stay on toys.** Sync opts already move the needle dramatically; `rpc_server` + Kotlin twin is the right loop. Do **not** build TOB/chain yet. Return to Raft after the Julay↔native gap is in a credible band.
