# Examples

Walkthroughs of the HTTP demos under [`input/`](../../input/). Suggested order:

1. [Echo server](echo-server.md) — separate server and client compile targets; `serveHttp` + `httpRequest`
2. [Inc server](inc-server.md) — counter as a `service` hub **plus** TLA+ specs
2b. [Sharded KV](sharded-kv.md) — multi-node ownership, redirects, reshard; `ShardNode` / `ShardClient` + `ShardSpec`
3. [List server](list-server.md) — list state behind `getAndAppend` / `getList`
4. [RPC server](rpc-server.md) — inc-like core with Raft-style path route + body parse

Each page points at the real `.jul` sources. Build `julayc` first ([Getting started](../getting-started.md)).

## Case studies

- [Bugs found with Julay](bugs-found-with-julay.md) — historical Timer / session bugs caught by specs + TLC (proclib Timer since replaced by builtins)

## Larger system

[`input/raft/`](../../input/raft/) is a larger multi-module example (Raft). It is **not yet ported** to the Go / `static proc` / `serveHttp` model; see [`input/raft/README_GO_PORT.md`](../../input/raft/README_GO_PORT.md). Use `julayc analyze` ([Tooling](../tooling.md)) on the smaller servers to explore composition.
