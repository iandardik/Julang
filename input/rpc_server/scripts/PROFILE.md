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

# No-HTTP SyncChannel / Select deep dive (fine buckets + keywords):
./input/rpc_server/scripts/profile_rendezvous.sh --mode syncfast --event alloc --duration 12
./input/rpc_server/scripts/profile_rendezvous.sh --mode select3 --event alloc --duration 12
# Re-bucket any .collapsed:
python3 input/rpc_server/scripts/bucket_collapsed.py path.collapsed out.txt --fine --keywords
```

Artifacts land in [`profile-out/`](profile-out/) (gitignored):

| File | Meaning |
|------|---------|
| `*-cpu.html` / `.collapsed` | On-CPU samples + flamegraph |
| `*-wall.html` / `.collapsed` | Wall-clock (includes blocked time) |
| `*-alloc.html` / `.collapsed` | Allocation samples |
| `*-buckets.txt` | Coarse auto-bucketed share table |
| `*-buckets-fine.txt` | SyncChannel/Select sub-stage split + keyword hits |
| `rendezvous-*` | No-HTTP microbench profiles (`syncfast` / `select3`) |

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

## Phase 1 — EnablementBool (2026-08-30)

**Change:** `SyncResolveFast.groundGuard` returns `null` for disabled enablement instead of throwing `EnablementFalse` (which allocated exception + stack trace on every disabled offer in generated `syncStepPlan`). Codegen already skipped null grounded guards; no `.jul` or codegen change required.

**Allocation (`profile_rpc.sh --variant julay --mode alloc --duration 12 --clients 4`)**

| Area | Post-bridge | After Phase 1 |
|------|------------:|--------------:|
| SyncResolveFast | ~4% | **~2%** |
| SyncChannel / Select | ~28% | ~28% |
| invokeProcFun / Proc | ~10% | ~10% |

`EnablementFalse` / `fillInStackTrace` **gone** from hot alloc stacks. Remaining SyncResolveFast samples are legitimate grounding (`evalTermLocals`, `BoolExprFast.Eq`).

## Phase 2 — SyncChannel / syncFast (2026-08-30)

**Changes:**

1. **`SyncChannel.syncFast(constraint, anticonstraint)`** — no-Select entry for single-offer FastOnly steps; wired from `Proc.runOneStepFast` (size==1) and `Select.SyncCase.syncDirect`.
2. **`Participant.valueGate`** — `CompletableDeferred` replaces capacity-1 `Channel` for follower delivery (less BufferedChannel alloc).
3. **`DecisionBuf`** — reuse one decision buffer per `sync` call instead of allocating `SyncDecision` every pick/finish.
4. **Lazy `rejectedPeers`** — allocate the set only on first empty-compute rejection.
5. **`runOneStepFast` single-offer path** — calls `syncFast` directly (no `Select.SyncCase` list/map).
6. **NonCancellable** — kept on pick/finish/cleanup (required: cancel mid-`selectsCommit` leaked `pairing=true` and deadlocked); removed only from SAT value delivery (now non-suspending `complete`).

**Allocation (`profile_rpc.sh --variant julay --mode alloc --duration 12 --clients 4`)**

| Area | After Phase 1 | After Phase 2 |
|------|--------------:|--------------:|
| SyncChannel / Select | ~28% | **~24%** |
| invokeProcFun / Proc | ~10% | ~11% |
| HTTP / JulHttpServer / bridge | ~20% | ~21% |
| JDK HttpServer / NIO | ~39% | ~41% |

Collapsed stacks show `runOneStepFast → syncFast → sync` on client/internal steps. Plan target ≤18% not fully met: **`Protocol` still offers all three provider actions every step → `Select.run` with 3 cases** (BufferedChannel + cancel losers) remains the bulk of the SyncChannel bucket.

**`bench_toys.sh --targets rpc,rpc-native --ops 200 --clients 4`:** Julay ~2360 RPS, native ~5370 (~2.3×). Sustained profile load ~4850 RPS.

**Next:** Phase 3 — cut double procfun spawn (`handleRpc` → `in*RPC`); optionally shrink Protocol’s always-on multi-offer Select.

## Phase 3 — procfun-fuse (2026-08-30)

**Change:** compile-time opt `procfun-fuse` (default on; `--disable-opt=procfun-fuse` / bare `--disable-opt` off). Nested procfun calls are inlined into the caller `TransitionSystem` with `__julayFuse` blocking state and composition `channelKey` disambiguation. Top-level HTTP still `invokeProcFunBlocking("handleRpc")`.

**`bench_toys.sh --targets rpc,rpc-native --ops 200 --clients 4`:** Julay **~2160 RPS**, native ~4890 (~2.3×). Short-bench variance dominates; fusion’s win is alloc shape, not this toy RPS.

**Allocation (`profile_rpc.sh --variant julay --mode alloc --duration 12 --clients 4`)**

| Area | After Phase 2 | After Phase 3 (fuse) |
|------|--------------:|---------------------:|
| JDK HttpServer / NIO | ~41% | ~36% |
| SyncChannel / Select | ~24% | ~19% |
| HTTP / JulHttpServer / bridge | ~21% | ~18% |
| SyncResolveFast | ~2–4% | ~13% |
| invokeProcFun / Proc | ~11% | ~13% |

Collapsed stacks: **`handleRpc.transit → invokeProcFun` for nested `in*RPC` is gone**. Remaining `invokeProcFun` / `Proc` samples are the **top-level** HTTP spawn (`invokeProcFunBlocking` → `handleRpc`). SyncResolveFast share rose because the fused `handleRpc` offers more actions per step (router + all `in*RPC` slices) under one FastOnly plan.

**Spot-check:** rebuild with `--disable-opt=procfun-fuse` still correct (nested spawn path).

## Phase 4 — HttpServer thread pool (2026-08-30)

**Change:** `JulHttpServer.finishConstruction` sets `server.executor = Executors.newCachedThreadPool()` (matches Kotlin native twin). Default JDK `HttpServer` runs `handle` on a single server thread when unset, serializing concurrent requests while `invokeProcFunBlocking` blocks. Executor is shut down on `close`.

**`bench_toys.sh --targets rpc,rpc-native --ops 200 --clients 4 --warmup 40`**

| | Julay | Kotlin native | Ratio |
|--|------:|--------------:|------:|
| RPS | ~2525 | ~3975 | ~1.6× (was ~2.3×) |

**Sustained (`--ops 5000`, same clients/warmup):** Julay **~4544** vs native **~5454** RPS (~1.2×).

**Cold (`--warmup 0`, ops=200):** Julay **~2266** vs native **~4077** RPS (~1.8×).

Serial handler was a major amplifier on multi-client short benches. Remaining gap is top-level per-request Proc spawn + SyncChannel + JVM JIT on Julay’s deeper stack.

## Phase 5 — pooled HTTP handler proc (2026-08-31)

**Change:** `JulHttpServer` starts a `ProcFunHandlerPool` at `listen` (default size `max(2, availableProcessors())`). Long-lived handler worker procs loop on `HandlerWork`: fresh TS via procfun factory + `finishConstruction` per request, then existing select/transit until `return:`. `invokeProcFunBlocking` remains for non-HTTP callers.

**`bench_toys.sh --targets rpc,rpc-native --ops 200 --clients 4 --warmup 40`**

| | Julay | Kotlin native | Ratio |
|--|------:|--------------:|------:|
| RPS | ~3550 | ~5140 | ~1.45× (was ~1.6× post Phase 4) |

**Sustained (`--ops 5000`, same clients/warmup):** Julay **~4910** vs native **~5870** RPS (~1.19×).

**Cold (`--warmup 0`, ops=200):** Julay **~2990** vs native **~4400** RPS (~1.47×).

Top-level per-request Proc spawn removed from HTTP path; remaining gap is SyncChannel/Select + fresh TS factory per request.

### Post–Phase 5 re-profile (2026-08-31)

**Allocation (`profile_rpc.sh --variant julay --mode alloc --duration 12 --clients 4`)** — sustained mix load during profile ≈ **5090 RPS** Julay vs **5850 RPS** native (~1.15×).

| Area | Phase 3 (pre-pool) | After Phase 5 |
|------|-------------------:|--------------:|
| JDK HttpServer / NIO | ~36% | **~43%** |
| HTTP / JulHttpServer / bridge | ~18% | **~21%** |
| SyncChannel / Select | ~19% | **~21%** |
| invokeProcFun / Proc | ~13% | **~11%** |
| SyncResolveFast | ~13% | **~3%** |
| Z3 / Context | ~0% | ~0% |

**Native alloc (same harness):** ~100% JDK HttpServer / NIO (handler is thin mutex + respond).

**Proc-bucket sub-breakdown (Julay):** mostly pooled handler work — `runHttpHandlerLoop` ~66%, `ProcFunHandlerPool` ~10%, `runOneStep` ~8%. Literal `invokeProcFun` spawn is gone from the HTTP hot path.

**SyncChannel/Select sub-breakdown:** `SelectCoordinator` ~52%, `syncFast` ~38% — Protocol’s always-on multi-offer Select dominates Julay-only alloc.

**Keyword shares (all samples):** `runHttpHandlerLoop` ~17%, `SelectCoordinator` ~11%, `syncFast` ~10%, `readAllBytes` ~10%, `finishConstruction` / TS factory ~0.3%.

## Phase 6 — multi-offer Select setup reuse + opportunistic lead (2026-08-31)

**Change (no `.jul`):** FastOnly multi-offer steps call `SelectCoordinator.runOffers` with recycled `SelectCaseOffer` slots on the `Proc` (no per-step `Select.SyncCase` / `toTypedArray` remapping). `SelectCoordinator` prunes/scrambles with less list churn. For all-size-2 Selects (Protocol), the first scrambled case uses `parkOnly=false` so an already-waiting client can `NeedCompute` during `parkPass`.

**`bench_toys.sh --targets rpc,rpc-native --ops 200 --clients 4 --warmup 40`**

| | Julay | Kotlin native | Ratio |
|--|------:|--------------:|------:|
| RPS | ~3120 | ~4870 | ~1.56× (short-bench noise; Phase 5 was ~1.45×) |

**Sustained (`--ops 5000`, same clients/warmup):** Julay **~4910** vs native **~5830** RPS (~1.19×).

**Allocation (`profile_rpc.sh --variant julay --mode alloc --duration 12 --clients 4`)** — profile load ≈ **5060 RPS** Julay vs **5840 RPS** native (~1.15×).

| Area | After Phase 5 | After Phase 6 |
|------|--------------:|--------------:|
| JDK HttpServer / NIO | ~43% | **~42%** |
| HTTP / JulHttpServer / bridge | ~21% | **~23%** |
| SyncChannel / Select | ~21% | **~20%** |
| invokeProcFun / Proc | ~11% | **~11%** |
| SyncResolveFast | ~3% | **~3%** |

**Keyword shares:** `SelectCoordinator` ~10% (was ~11%), `syncFast` ~11%, `runOffers` ~8% (new FastOnly entry; SyncCase remapping gone from this path).

Modest alloc/shape win; sustained RPS band unchanged. Remaining Julay tax is still SyncChannel rendezvous + handler/bridge work vs native’s mutex.

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

## Rendezvous deep dive (2026-08-31)

Goal: split the opaque **SyncChannel / Select** alloc bucket into sub-stages so we can see **multiple** contributing costs (not chase a single #1).

Harness: [`bucket_collapsed.py`](bucket_collapsed.py) (`--fine --keywords`), [`profile_rendezvous.sh`](profile_rendezvous.sh) (`syncfast` / `select3`), and `profile_rpc.sh` (now writes `*-buckets-fine.txt`).

### rpc_server alloc — among SyncChannel/Select (~21% of all samples)

Fine named share **~95%** (almost no “other”).

| Sub-stage | Share of SyncChannel/Select |
|-----------|----------------------------:|
| Select setup (`runOffers` / `SelectCoordinator` / `SelectGroup`) | **~29%** |
| syncFast path | **~18%** |
| Compute / payload (`runSelectCompute`, `SyncPayload`, …) | **~15%** |
| Select park / offer | **~13%** |
| Select lead / wake | **~13%** |
| Match under lock (`pickCandidateLocked`, …) | **~7%** |
| Participant / wait + other | ~5% |

Keyword hits (all samples): `SelectCoordinator` ~12%, `syncFast` ~10%, `runOffers` ~9%, `SyncPayload` ~4%, `parkPass` ~3%, `pickCandidateLocked` ~1.5%.

**Read:** under real Protocol Select + handler syncFast, cost is **spread**: residual Select shell (~29%), handler syncFast (~18%), payload/compute (~15%), and park+lead together (~26%).

### No-HTTP microbench alloc (rendezvous dominates samples)

**`syncfast`** (pairwise size-2): SyncChannel ~98% of samples.

| Sub-stage | Share of SyncChannel/Select |
|-----------|----------------------------:|
| syncFast path | **~58%** |
| Match under lock | **~20%** |
| Compute / payload | **~16%** |
| Participant / wait | ~3% |

**`select3`** (3-case `runOffers` vs one sync peer):

| Sub-stage | Share of SyncChannel/Select |
|-----------|----------------------------:|
| Select setup | **~29%** |
| Select park / offer | **~24%** |
| Select lead / wake | **~20%** |
| SyncChannel other | ~17% |
| Compute / payload | ~5% |
| Match under lock | ~4% |

### CPU note

Raw rpc **cpu** is still mostly `thread park / wait` + JDK NIO. Among the small SyncChannel slice, the same multi-factor shape appears (setup / syncFast / compute). Prefer **alloc** + microbench cpu (busy SyncChannel share higher) for rendezvous work.

### Actionable optimization targets (plural)

No single bucket is “the” problem. Concrete code levers suggested by the breakdown:

1. **Select setup alloc** (~29% of SC on rpc + select3) — `SelectGroup` / `CompletableDeferred` nudge machinery, scramble/handle lists still allocated per Select even after Phase 6 offer reuse. Shrink or recycle Select shell objects.
2. **syncFast + match-under-lock** (~58%+20% on syncfast microbench; ~18%+7% on rpc SC) — `Participant` construction, `pickCandidateLocked` peer scan, Optional/constraint wrappers on the single-offer path. This is the handler half of every RPC.
3. **Compute / payload** (~15–16%) — `SyncPayload` / extract path after SAT; worth auditing for per-rendezvous object churn once (1)/(2) move.
4. **Select park + lead/wake** (~26% rpc SC, ~44% select3) — coordinator parkPass / tryLeadParked / awaitCompletionOrNudge loop for Protocol’s always-on 3-offer wait. Complementary to (1); same Select lifecycle.

## Interpretation vs RPS gap

Native is faster because each request is “parse → mutex → respond” on the HTTP thread. Julay still pays:

1. `Program.invokeProcFun` (spawn a Proc per request, often **two** — `handleRpc` + `in*RPC`) — **Phase 3**
2. SyncChannel / Select — improved in Phase 2; residual is multi-factor rendezvous (see deep dive above), not only Select setup
3. SyncResolveFast (small once opts are on)

## Next optimization candidate

**Phase 7 — attack several rendezvous factors**, starting with whichever is easiest to land safely: Select shell/`SelectGroup` reuse (setup), then `syncFast`/`Participant`/`pickCandidateLocked` alloc, then park/lead wake overhead. Re-run `profile_rpc.sh` alloc + `profile_rendezvous.sh` after each change; expect SyncChannel/Select coarse share and the fine sub-tables to move together.


## Flamegraph index (this machine)

- [julay-cpu.html](profile-out/julay-cpu.html) · [julay-alloc.html](profile-out/julay-alloc.html) · [julay-wall.html](profile-out/julay-wall.html)
- [native-cpu.html](profile-out/native-cpu.html) · [native-alloc.html](profile-out/native-alloc.html) · [native-wall.html](profile-out/native-wall.html)
