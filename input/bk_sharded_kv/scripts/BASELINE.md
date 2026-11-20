# SKV Tier-0 baseline (sanity check)

Localhost microbench on one machine. **Not** a paper figure — protocols, transport, and durability differ. Goal: confirm Julang SKV is faster than Raft-based KVs because it does not quorum-replicate every put.

## Numbers (2026-11-14, same host)

| System | Workload | Clients | Ops | Throughput (ok RPS) | Notes |
|--------|----------|---------|-----|---------------------|--------|
| **Julang SKV** | put | 1 | 200 | **~680** | HTTP pipe; in-memory; p50 put ~1.0 ms |
| **Julang SKV** | put | 4 | 800 | **~1100–1160** | concurrent clients |
| **Julang SKV** | put+get | 1 | 100+100 | **~1050** overall | get p50 ~0.4 ms |
| **PGo raftkvs** | put | 1 | 200 | **~45** | DistCompiler/pgo; TCP + Badger; wall includes client start |
| **Julang Raft** | append | 1 | 20 | **~0.7** | JAR + election timers; much heavier path |

Rough ratios (same sequential put shape where possible):

- SKV (1c) vs PGo raftkvs: **~15×**
- SKV (4c) vs PGo: **~25×** (extra concurrency on SKV)
- SKV vs Julang Raft: **orders of magnitude** (different op + runtime)

## How to reproduce

```bash
# SKV
BENCH_OPS=200 BENCH_CLIENTS=1 BENCH_MODE=put input/sharded_kv/scripts/bench_tier0.sh
BENCH_OPS=200 BENCH_CLIENTS=4 BENCH_MODE=put input/sharded_kv/scripts/bench_tier0.sh

# PGo raftkvs (needs tools/pgo clone + built binaries; see bench script)
BENCH_OPS=200 input/sharded_kv/scripts/bench_pgo_raftkvs.sh

# Combined summary log
input/sharded_kv/scripts/compare_sanity.sh
```

PGo build (once): clone [DistCompiler/pgo](https://github.com/DistCompiler/pgo) under `tools/pgo`, then `cd tools/pgo/systems/raftkvs && GOWORK=off make build` (or use the repo’s `go.work`). Config used: `scripts/pgo/local-3-bench.yaml`.

## Why this is only a sanity check

- **SKV**: ownership + redirect; optional load reshard; no durable log; no quorum on put.
- **PGo raftkvs**: Raft leader quorum + Badger persistence + TCP mailboxes — different wire and failure model.
- **Julang Raft**: append-only JAR path with election; not the same API as SKV put.
- All runs are localhost; no WAN, no YCSB, no Azure.

Sanity claim that held here: **SKV ≫ PGo raftkvs ≫ Julang Raft JAR** on simple sequential puts, consistent with “simpler protocol → less work per write.”
