# RPC server

**Sources:** [`input/rpc_server/`](../../input/rpc_server/)

| File | Role |
|------|------|
| [`main.jul`](../../input/rpc_server/main.jul) | Composition + `compile` |
| [`protocol.jul`](../../input/rpc_server/protocol.jul) | Counter core (`Protocol` `service` hub RPCs) |
| [`server.jul`](../../input/rpc_server/server.jul) | `serveHttp` + path router + per-RPC handlers |
| [`lib.jul`](../../input/rpc_server/lib.jul) | `fieldValue` for pipe-delimited bodies |

## Intent

Same sync model as the [inc server](inc-server.md), but HTTP ingress looks like Raft’s inbound RPC path: **route by path → parse the body → sync into a core protocol proc**. Use it as a runtime / load-test toy between echo/inc and full Raft.

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

`RpcIn` is `ServerInitializer` (`static proc` + `serveHttp(8000, handleRpc)`). `handleRpc` dispatches:

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
./RpcServer
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
```

Kotlin twin sources: [`native/`](../../input/rpc_server/native/). Profiling notes under [`PROFILE.md`](../../input/rpc_server/scripts/PROFILE.md) still describe the former JVM path; treat them as historical.

## Compare

| | Echo | Inc | RPC (Julay) |
|--|------|-----|-------------|
| HTTP | yes (`serveHttp`) | yes | yes |
| `service`/`client` hub sync | no | yes | yes |
| Path route + body parse | no | no | yes |

## See also

- [Inc server](inc-server.md) — simpler counter without RPC dispatch
- [Echo server](echo-server.md) — HTTP floor
- Raft port notes: [`input/raft/README_GO_PORT.md`](../../input/raft/README_GO_PORT.md)
