# RPC server

**Sources:** [`input/rpc_server/`](../../input/rpc_server/)

| File | Role |
|------|------|
| [`main.jul`](../../input/rpc_server/main.jul) | Composition + `compile` |
| [`protocol.jul`](../../input/rpc_server/protocol.jul) | Counter core (`Protocol` provider RPCs) |
| [`server.jul`](../../input/rpc_server/server.jul) | HTTP listen + path router + per-RPC handlers |
| [`lib.jul`](../../input/rpc_server/lib.jul) | `fieldValue` for pipe-delimited bodies |

## Intent

Same sync model as the [inc server](inc-server.md), but HTTP ingress looks like Raft’s inbound RPC path ([`rpc_in.jul`](../../input/raft/node/rpc_in.jul)): **route by path → parse the body → `client` sync into a core protocol proc**. Use it as a runtime / load-test toy between echo/inc and full Raft.

No TLA+ specs in the first cut (execution-focused, like the [list server](list-server.md)).

## API

Paths are Julay HTTP paths (leading `/` stripped). Responses are `v=<int>` on success.

| Path | Body | Core action |
|------|------|-------------|
| `POST /rpc/increment` | ignored | `increment(newVal)` |
| `POST /rpc/get` | ignored | `getCounter(counterVal)` |
| `POST /rpc/add` | `delta=<int>` | `add(delta, newVal)` |
| other | — | `404 NOT_FOUND` |

Pipe-delimited `key=value` fields match Raft’s wire style (`fieldValue` / `split`).

## Composition

```jul
proc RpcServer := Protocol || RpcIn
```

`RpcIn` is `ServerInitializer || HttpServer`. `handleRpc` dispatches:

```jul
return: when (req.path) {
    "rpc/increment" -> inIncrementRPC(req)
    "rpc/get" -> inGetRPC(req)
    "rpc/add" -> inAddRPC(req)
    else -> HttpServerResponse { body := "NOT_FOUND", code := 404 }
}
```

Each handler parses (if needed), syncs with `Protocol`, then returns an `HttpServerResponse`.

## How to run

```bash
./gradlew shadowJar
java -jar build/libs/julayc.jar input/rpc_server/main.jul
java -jar RpcServer.jar
```

```bash
curl -s -X POST http://localhost:8000/rpc/increment
curl -s -X POST -d 'delta=5' http://localhost:8000/rpc/add
curl -s -X POST http://localhost:8000/rpc/get
```

Sample responses:

```text
v=1
v=6
v=6
```

## Load test

```bash
# One target against an already-running server:
./input/rpc_server/scripts/bench_load.py --target rpc --ops 100 --mode mix --clients 2

# Julay rpc vs Kotlin-native twin (same wire API):
./input/rpc_server/scripts/bench_toys.sh --targets rpc,rpc-native --ops 200 --clients 4 --warmup 40

# Compile + run echo / inc / rpc (opts on; add --with-disable-opt for A/B):
./input/rpc_server/scripts/bench_toys.sh --ops 80 --clients 2 --warmup 20 --with-disable-opt
```

Kotlin twin sources: [`native/`](../../input/rpc_server/native/). Server stdout is redirected so echo’s per-request `println` does not dominate the floor.

### Profiling

```bash
./input/rpc_server/scripts/profile_rpc.sh --variant julay --mode alloc --duration 12 --clients 4
./input/rpc_server/scripts/profile_rpc.sh --variant native --mode alloc --duration 12 --clients 4
```

See [`PROFILE.md`](../../input/rpc_server/scripts/PROFILE.md) for flamegraphs, the bucketed hotspot table, and the next optimization candidate.

## Compare

| | Echo | Inc | RPC (Julay) | RPC (Kotlin native) |
|--|------|-----|-------------|---------------------|
| HTTP | yes | yes | yes | yes (same JDK server) |
| User SyncChannel | no | yes | yes | no (mutex) |
| Path route + body parse | no | no | yes | yes |

## See also

- [Inc server](inc-server.md) — simpler counter without RPC dispatch
- [Echo server](echo-server.md) — HTTP floor
- Raft [`rpc_in.jul`](../../input/raft/node/rpc_in.jul) — full multi-RPC ingress this toy miniaturizes
