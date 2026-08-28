# Sessions

A **`session`** action marks an **exclusive pairwise** protocol between two procs of different classes. After first contact, follow-on session actions stick to that peer pair (**affinity**) until the session ends.

`session` cannot be combined with `provider` or `internal`. Every offer of an action must agree on the `session` tag.

`session` tags a **peer-pair protocol**: if two proc classes share any session action, they must not also share an **ordinary** (untagged) action. Session with one peer class does not constrain non-session interaction with a *different* peer class, and a session peer may still use a `provider` / `client` API with the same hub.

### Allowed: session with B, ordinary with C

[`session-ordinary-other-peer.jul`](../../regression/input/session/session-ordinary-other-peer.jul) — `A` and `B` share `session x`; `A` and `C` share ordinary `y` (`B ≠ C`). `A || B || C` compiles:

```jul
proc A {
    session transition x() { guard: false }
    transition y() { guard: false }
}
proc B {
    session transition x() { guard: false }
}
proc C {
    transition y() { guard: false }
}
```

### Allowed: session with B, provider API for B and C

[`session-provider-with-peer-client.jul`](../../regression/input/session/session-provider-with-peer-client.jul) — `A` and `B` share `session x`; `A` provides `y` while `B` and `C` are clients. `A || B || C` compiles:

```jul
proc A {
    session transition x() { guard: false }
    provider transition y() { guard: false }
}
proc B {
    session transition x() { guard: false }
    client transition y() { guard: false }
}
proc C {
    client transition y() { guard: false }
}
```

### Forbidden: session and ordinary with the same peer

[`session-ordinary-pair-mix.jul`](../../regression/input/syntax-errors/session-ordinary-pair-mix.jul) — `A` and `B` share both `session x` and ordinary `y`. This is a compile error:

```jul
proc A {
    session transition x() { guard: false }
    transition y() { guard: false }
}
proc B {
    session transition x() { guard: false }
    transition y() { guard: false }
}
```

## Lifecycle (user-facing)

1. **First contact** — peers synchronize on a session action; affinity is established.
2. **Sticky follow-ons** — later offers of session actions with that peer stay paired.
3. **End of session** — when a peer exits, or via `before:` / `after:` calls:

   - `exitSession(PeerClass)` — clear affinity with the named leaf proc class; **both** procs keep running (no-op if that affinity is absent)
   - `killSessionPeer(PeerClass)` — clear affinity with the named peer class and **cancel** that peer proc (no-op if absent)

`exitSession` and `killSessionPeer` may appear only on **transitions** (not constructors). The argument must be a leaf proc-class name. See [Effects](effects.md).

How affinity, teardown, and these effects appear in generated TLA+ (including what is omitted when unused) is documented under [Sessions in TLA+](specifications.md#sessions-in-tla).

## Session constructors

A **session constructor** creates a new proc instance as part of establishing a session (for example, a `TimerHelper` spawned by `createTimer`). While affinity to a peer of that child class is live, attempting to spawn another of the same kind fails; after the session ends, a later spawn may succeed.

## Typical pattern: Timer

[`julay.proclib.Timer`](../../src/main/resources/stdlib/julay/proclib/Timer.jul) exposes `createTimer`, `startTimer`, `timeout`, and `cancelTimer`. Helper completion uses `exitSession(TimerHelper)` on `timerHelperEnd`; cancel uses `killSessionPeer(TimerHelper)` so the delaying helper can be stopped without blocking the controller.

A restart-after-timeout rebind bug in Timer was caught by `TimerSpec` + TLC; see [Bugs found with Julay](../examples/bugs-found-with-julay.md).

### HTTP (lifecycle only)

[`HttpServer`](standard-library.md#httpserver) still uses **session** actions for `listen` / `close` (pairing `ServerStarter` with the Kotlin library). Per-request handling is **not** session-based: handlers are procfuns invoked via `Program.invokeProcFun`. See [Standard library](standard-library.md) — do not use `receiveRequest` / `sendResponse` (removed).

## See also

- [Composition and actions](composition-and-actions.md)
- [Standard library](standard-library.md)
- [Specifications](specifications.md) — especially [Sessions in TLA+](specifications.md#sessions-in-tla)
- Echo / inc / list examples under [Examples](../examples/README.md)
