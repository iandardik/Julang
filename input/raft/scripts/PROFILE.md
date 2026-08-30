# Profiling Julay Raft (Tier-0 localhost)

Capture alloc/cpu/wall on the **leader** JVM under `bench_load.py` traffic.

## How to run

```bash
# Rebuilds RaftNode.jar, starts 3-node cluster, profiles leader during load.
./input/raft/scripts/profile_raft.sh --mode alloc --duration 15 --clients 1 --ops 800
./input/raft/scripts/profile_raft.sh --mode cpu --duration 15 --clients 1 --ops 400 --bench-mode mix
./input/raft/scripts/profile_raft.sh --skip-build --mode alloc   # reuse existing jar
```

Prefer **`--clients 1`** for a stable leader under today’s Tier-0 timers; multi-client runs often hit `NO_LEADER` / election churn.

Artifacts in [`profile-out/`](profile-out/):

| File | Meaning |
|------|---------|
| `raft-leader-*.html` / `.collapsed` | Leader flamegraph / stacks |
| `raft-leader-*-buckets.txt` | Auto-bucketed alloc/cpu shares |
| `raft-leader-*-bench.txt` | Client RPS / latency |

Optional `--profile-all` also samples followers while load is still running.

**LIMITATIONS:** Tier-0 localhost only (see `bench_load.py`). Not comparable to PGo / etcd / YCSB. No Kotlin-native Raft twin in-repo. Append latency is often ~0.5–2s with ~1–2s election timers, so sample counts are much lower than `rpc_server` profiles.

## Snapshot — 2026-08-30 (post Select park-once + race)

**Harness:** `profile_raft.sh --mode alloc --duration 15 --clients 1 --ops 800` (leader node).

| Bucket (alloc) | Share |
|----------------|------:|
| Z3 / Context / SyncResolveZ3 | **~58%** |
| invokeProcFun / Proc | **~33%** |
| SyncChannel / Select | **~5%** |
| Other / park | ~3% |

Warmup (20 appends, 1 client) completed at **~0.84 RPS** (p50 ~1.1s). The long 800-op window mostly failed after election churn (`ok=29 fail=771`) — treat the flamegraph as “leader under Raft/Z3 load,” not a clean sustained-RPS peer to rpc_server’s ~5k RPS.

**Contrast with rpc_server (same day):** rpc_server sustained alloc is ~0% Z3 and ~22% SyncChannel/Select; Raft’s hot path is still **Z3 + Proc**, not Select.
