# Profiling Julay `rpc_server`

How to capture flamegraphs and what we learned from profiling (macOS arm64, JDK 18).

## How to run

```bash
# Auto-fetches async-profiler into tools/async-profiler/ if needed.
./input/rpc_server/scripts/profile_rpc.sh --variant julay --mode cpu --duration 15 --clients 4
./input/rpc_server/scripts/profile_rpc.sh --variant julay --mode wall --duration 12 --clients 4
./input/rpc_server/scripts/profile_rpc.sh --variant julay --mode alloc --duration 12 --clients 4
./input/rpc_server/scripts/profile_rpc.sh --variant native --mode cpu --duration 12 --clients 4
./input/rpc_server/scripts/profile_rpc.sh --variant native --mode alloc --duration 12 --clients 4
```

Artifacts land in [`profile-out/`](profile-out/) (gitignored):

| File | Meaning |
|------|---------|
| `*-cpu.html` / `.collapsed` | On-CPU samples + flamegraph |
| `*-wall.html` / `.collapsed` | Wall-clock (includes blocked time) |
| `*-alloc.html` / `.collapsed` | Allocation samples |
| `*-buckets.txt` | Auto-bucketed share table |

Open the `.html` files in a browser. Width ≈ share of samples.

**Modes:** prefer **`alloc`** and **busy-only `cpu`** for optimization decisions. Raw **`wall`/`cpu`** are dominated by thread park/wait under this load (healthy server waiting for work / rendezvous), which hides Julay logic unless you exclude park.

## What the numbers mean

Each stack is assigned to **one** bucket (most-specific match wins). Shares are approximate profiler samples, not stopwatch timings.

**Busy-only** = exclude `thread park / wait` and `JVM idle / helper threads`, so the table answers “when the process is actually doing work, where?”

## After HTTP bridge fix (2026-08-30)

**Change:** `JulHttpServer.handle` no longer uses `runBlocking`. It calls `Program.invokeProcFunBlocking`, which launches `invokeProcFun` on the long-lived `godScope` and blocks the JDK thread with `CompletableFuture.get()`. Request/response I/O uses `readAllBytes` / byte writes.

**`bench_toys.sh --targets rpc,rpc-native --ops 200 --clients 4`**

| | Julay | Kotlin native | Ratio |
|--|------:|--------------:|------:|
| RPS | ~2110 | ~4930 | ~2.3× |

Under sustained load during profiling (`--ops 8000`), Julay reached **~4830 RPS** on the same machine — much closer to native and up from the pre-fix ~2320 short-bench baseline (variance and warmup affect the toy harness).

**Allocation (`profile_rpc.sh --variant julay --mode alloc --duration 12 --clients 4`)**

| Area | Before (runBlocking) | After (bridge) |
|------|---------------------:|---------------:|
| HTTP / JulHttpServer / bridge | ~45% | **~19%** |
| JDK HttpServer / NIO | ~26% | ~38% |
| SyncChannel / Select | ~19% | ~28% |
| invokeProcFun / Proc | ~7% | ~10% |
| SyncResolveFast | ~2% | ~4% |
| Z3 / Context | ~0% | ~0% |

Collapsed alloc stacks no longer show `runBlocking`, `BlockingCoroutine`, `EventLoopImplBase`, or `ThreadLocalEventLoop.createEventLoop`. The bridge bucket is now `JulHttpServer.handle` → `invokeProcFunBlocking` → `CompletableFuture.get` plus string/body copies.

**Next target:** per-request **`invokeProcFun` + SyncChannel / Select`** (~38% alloc combined), not residual bridge overhead.

## Baseline — before bridge fix (2026-08-29)

### Busy-only CPU (`-e cpu`)

| Area | Julay | Kotlin native |
|------|------:|--------------:|
| JDK HttpServer / NIO | ~27% | ~86% |
| SyncChannel / Select | ~22% | — |
| HTTP / JulHttpServer / runBlocking | ~17% | — |
| invokeProcFun / Proc | ~9% | — |
| SyncResolveFast | ~3% | — |
| Z3 / Context / SyncResolveZ3 | ~0% | — |
| GC / JIT + Other | ~21% | ~14% |

Julay spends a large fraction of **on-CPU busy** time in SyncChannel + JulHttpServer/`runBlocking` + Proc spawn. Native is almost entirely JDK HTTP. **Z3 is not on the steady-state opts-on path.**

### Allocation (`-e alloc`)

| Area | Julay | Kotlin native |
|------|------:|--------------:|
| HTTP / JulHttpServer / runBlocking | ~45% | — |
| JDK HttpServer / NIO | ~26% | ~100% |
| SyncChannel / Select | ~19% | — |
| invokeProcFun / Proc | ~7% | — |
| SyncResolveFast | ~2% | — |
| Z3 / Context | ~0% | — |

Alloc makes the Julay tax even clearer: per-request **procfun invoke + SyncChannel** allocate heavily vs native’s thin handler.

## Interpretation vs RPS gap

Native is faster because each request is “parse → mutex → respond” on the HTTP thread. Julay still pays:

1. `Program.invokeProcFun` (spawn a Proc per request) — **next optimization target**
2. SyncChannel / Select rendezvous with `Protocol` — **next optimization target**
3. SyncResolveFast (small once opts are on)

The HTTP bridge (`runBlocking` per request) was removed 2026-08-30; see **After HTTP bridge fix** above.

Named sync opts already removed Z3 from this workload; further wins are **procfun lifecycle / IPC**, not the solver.

## Next optimization candidate

**Reduce per-request `invokeProcFun` + SyncChannel cost** (lighter procfun spawn, reuse, or a Channel-style fast path for clear client/provider pairs — see [compiler-optimizations.md](../../../docs/language/compiler-optimizations.md) “Kotlin Channel rewrites … not implemented yet”). Do **not** prioritize residual Z3 for `rpc_server` opts-on until that gap shrinks.

## Flamegraph index (this machine)

- [julay-cpu.html](profile-out/julay-cpu.html) · [julay-alloc.html](profile-out/julay-alloc.html) · [julay-wall.html](profile-out/julay-wall.html)
- [native-cpu.html](profile-out/native-cpu.html) · [native-alloc.html](profile-out/native-alloc.html) · [native-wall.html](profile-out/native-wall.html)
