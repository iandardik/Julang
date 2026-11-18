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
- `ShardSpec.tla` / `ShardSpec.cfg`

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

- Shard source is identical in JAR and TLA; only the composition changes. JAR: `ShardNode := Store || ServerStarter || HttpWorkers || SendClaim` (`PutPath` / `ApplyMovePath` under `HttpWorkers`). TLA: `VerifNodeIndexed || PerfectNet` — node state create-indexed, PerfectNet unindexed beside it (`Verif*` names avoid decl clashes in `sys.jul`). `Put.put` / `putDone` stay unsynced (environment).
- Unindexed `PerfectNet := ClaimMsgs || MoveBatchMsgs || KeyCountReqs || KeyCountReplies` (shared bags; four `static spec` leaves so `also` works and the net stays out of `ShardNode`). Actions use `also (n : NodeSet)` to sync with the indexed node. `goClaim` / `goMoveBatch` are fire-and-forget; only `goKeyCount` has a reply. The peer that receives a move batch takes ownership and broadcasts claims.
- Invariants target `Store[n].store` / `Store[n].owners`. Each dynamic class gets a `Terminates` property under strong fairness.
