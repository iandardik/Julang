# Sharded KV

Multi-node key–value store under [`input/sharded_kv/`](../../input/sharded_kv/). Each node owns a shard of keys, broadcasts ownership on first put, redirects gets/puts to the owner, and may reshard under load.

## Layout

| Path | Role |
|------|------|
| `cluster.conf` | Node URLs (ids `1..n` by line order), ports **6001–6003** |
| `node/` | `Store` hub + Shard workers + `rpc_in` / `rpc_out` children; JAR nests workers under `HandleRpc` / peers |
| `client/` | CLI with one `303 OWNER` redirect retry |
| `specs/` | Same Shard children re-parented: unsynced `Put` + `PerfectNet` (`ClaimMsgs \|\| …` four bag leaves) + `DynProc` slots |
| `scripts/` | `compile.sh`, `start_cluster.sh`, `stop_cluster.sh`, `smoke_test.sh`, `reshard_test.sh` |

## Build

From the repo root:

```bash
./gradlew shadowJar
input/sharded_kv/scripts/compile.sh
```

That compiles `sys.jul` and places under `input/sharded_kv/`:

- `ShardNode`
- `ShardClient`
- `ShardEnvSpec.tla` / `.cfg` and `StoreSpec.tla` / `.cfg`

(`julayc` still emits into the cwd first; `compile.sh` moves the artifacts here.)

## Run cluster

```bash
input/sharded_kv/scripts/start_cluster.sh
input/sharded_kv/scripts/smoke_test.sh
input/sharded_kv/scripts/reshard_test.sh   # longer; forces load-based moves
input/sharded_kv/scripts/stop_cluster.sh
```

Or manually:

```bash
input/sharded_kv/ShardNode "$(pwd)/input/sharded_kv/cluster.conf" 1
input/sharded_kv/ShardClient http://127.0.0.1:6001 put myKey myVal
input/sharded_kv/ShardClient http://127.0.0.1:6002 get myKey    # may 303 then retry
input/sharded_kv/ShardClient http://127.0.0.1:6001 owners
```

## Protocol sketch

- **Put** on owner / unowned key: store locally, broadcast `/rpc/claim`; may reshard 25% of keys to a lighter peer first (at most once per put).
- **Put/Get** when another node owns the key: `303 OWNER <url>`.
- **Owners**: local `key->ownerUrl` directory.

## TLA+ sketch

- Shard source is identical in JAR and TLA; only the composition changes. JAR: `ShardNode := Store || ServerStarter || HttpWorkers || SendClaim`.
- **Monolithic smoke:** `System := <true> (ShardEnv || IndexedStore) <StoreInvs>` → `System.tla` / `.cfg`.
- **Compositional AG (sound Rest cut for `System ⊢ StoreInvs` at bound B):** see [`SOUNDNESS.md`](../../input/sharded_kv/SOUNDNESS.md).
  - **Circular pair:** `ShardEnvInv |= <true, StoreIface> ShardEnv <StoreAsm>` × `StoreInv |= <StoreAsm> IndexedStore <StoreIface, StoreInvs>` → `CircSide` SC1/SC2. `StoreIface` is fluent reply honesty (`MeTold`/`OwnerTold` vs `Held`/`ClaimSeen`).
  - **Rest-internal RG (scaffolds):** Put/Inbound Obls; ShardEnvObl is the soundness-critical left premise (Put∥Inbound⇒ShardEnv lemma eliminated).
  - **Apalache:** `./scripts/apalache_inductive.sh` + `./scripts/apalache_circside.sh` after `JULAYC_OPTS='--opt-tla=apalache' ./scripts/compile.sh`.
  - **TLC:** `./input/sharded_kv/scripts/tlc_obls.sh` (20m, `-workers auto`; Store/Inbound Key `{a,b}`; Put/ShardEnv `{a..e}`). Pass = no `Error:` in the window. CircSide SC1 strips `INVARIANT TypeOK` (BoundedSeq of TypeOKInt×String is not TLC-enumerable).
  - **Local Inv → Apalache:** `LocalInv |= <A> C <G>` in `protocol.jul` (`StoreInv` / `PutInv` / `InboundInv` / `ShardEnvInv`; each `LocalInv := Inv1 & …` with strengthenings). Recompile with `JULAYC_OPTS='--opt-tla=apalache'`, then `./input/sharded_kv/scripts/apalache_obls.sh`. Outer loop: CE that falsifies the AG story → redesign contracts; missing Inv conjuncts only → strengthen Inv.
  - Sorts trimmed (2 nodes, 5 keys for reshard `/4`, 2 values). `goKeyCount` stays open under Inbound (without it the Obl is vacuous); that makes `KeyCountTerminates` fail under fairness even when `StoreAsm` holds — so AG Obl `.cfg`s use `SPECIFICATION SpecSafety` and omit `PROPERTY *Terminates`. See [AG Obl `.cfg`](../language/specifications.md#ag-obl-cfg-safety-by-default-no-terminates) and [Compiler optimizations — Apalache](../language/compiler-optimizations.md#opt-in---opt-tla-default-off). `sys.jul` compiles System + Put/Inbound/Store/ShardEnv Obls (+ CircSide).
- Unindexed `PerfectNet := ClaimMsgs || MoveBatchMsgs || KeyCountReqs || KeyCountReplies` (shared bags; four `static spec` leaves). Actions use `also (n : NodeSet)` to sync with the indexed workers.
- Store invariants target `Store[n].store` / `Store[n].owners`.
