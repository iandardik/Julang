# Sessions

A **`session`** action marks an **exclusive pairwise** protocol between two procs of different classes. After first contact, follow-on session actions stick to that peer pair (**affinity**) until the session ends.

`session` cannot be combined with `provider` or `internal`. Every offer of an action must agree on the `session` tag.

## Lifecycle (user-facing)

1. **First contact** — peers synchronize on a session action; affinity is established.
2. **Sticky follow-ons** — later offers of session actions with that peer stay paired.
3. **End of session** — when a peer exits, or via `before:` / `after:` calls:

   - `exitSession(PeerClass)` — clear affinity with the named leaf proc class; **both** procs keep running (no-op if that affinity is absent)
   - `killSessionPeer(PeerClass)` — clear affinity with the named peer class and **cancel** that peer proc (no-op if absent)

`exitSession` and `killSessionPeer` may appear only on **transitions** (not constructors). The argument must be a leaf proc-class name. See [Effects](effects.md).

How affinity, teardown, and these effects appear in generated TLA+ (including what is omitted when unused) is documented under [Sessions in TLA+](specifications.md#sessions-in-tla).

## Session constructors

A **session constructor** creates a new proc instance as part of establishing a session (for example, one HTTP handler per `receiveRequest`). While affinity to a peer of that child class is live, attempting to spawn another of the same kind fails; after the session ends, a later spawn may succeed.

## Typical pattern: HTTP

[`input/echo_server/server.jul`](../../input/echo_server/server.jul) uses session actions with `HttpServer`:

```jul
session transition createHttpServer(port : Int) { ... }

session constructor receiveRequest(req : HttpServerRequest) { ... }

session transition sendResponse(resp : HttpServerResponse) { ... }
```

The handler instance is constructed per request and talks to the HTTP library through session actions.

## Timer

[`julay.proclib.Timer`](../../src/main/resources/stdlib/julay/proclib/Timer.jul) exposes `createTimer`, `startTimer`, `timeout`, and `cancelTimer`. Helper completion uses `exitSession(TimerHelper)` on `timerHelperEnd`; cancel uses `killSessionPeer(TimerHelper)` so the delaying helper can be stopped without blocking the controller.

A restart-after-timeout rebind bug in Timer was caught by `TimerSpec` + TLC; see [Bugs found with Julay](../examples/bugs-found-with-julay.md).

## See also

- [Composition and actions](composition-and-actions.md)
- [Standard library](standard-library.md)
- [Specifications](specifications.md) — especially [Sessions in TLA+](specifications.md#sessions-in-tla)
- Echo / inc / list examples under [Examples](../examples/README.md)
