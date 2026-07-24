# Examples

Walkthroughs of the HTTP demos under [`input/`](../../input/). Suggested order:

1. [Echo server](echo-server.md) — separate server and client compile targets; HTTP session flow
2. [Inc server](inc-server.md) — counter as a provider interface **plus** TLA+ specs
3. [List server](list-server.md) — list state behind `getAndAppend` / `getList`

Each page points at the real `.jul` sources. Build `julayc` first ([Getting started](../getting-started.md)).

## Case studies

- [Bugs found with Julay](bugs-found-with-julay.md) — stdlib Timer session-ctor rebind caught by `TimerSpec` + TLC

## Larger system

[`input/raft/`](../../input/raft/) is a larger multi-module example (Raft). It is not covered in detail here yet; use `julayc analyze` ([Tooling](../tooling.md)) to explore its composition.
