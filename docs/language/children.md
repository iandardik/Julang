# Parent-bound procs (`B<A>`)

Any dynamic proc may be given a parent at composition time with `B<A>`. Dynamic–dynamic synchronization is allowed only between a bound child occurrence and its parent (in both directions). This replaces the old `procfun` / `api` / expression-call model: request/response is an ordinary dynamic proc with a constructor (request) and a `return` action (reply).

## Declaring a dynamic proc

```jul
proc Worker {
    var acc : Int := 0

    constructor start(x : Int) {
        transit: acc := x
    }

    internal transition step() {
        guard: acc < 10
        transit: acc := acc + 1
    }

    return done(y : Int) {
        guard: acc >= 10 & y = acc
    }
}
```

- The declaration never names the parent. Parenthood is a property of the composition: `Worker<Boss>`.
- Only dynamic procs may be bound as `B<A>` (static procs cannot).
- Dynamic procs have no `constructor initially`. They exist only by being spawned.
- Every dynamic proc must have at least one `return` action. Static procs may have none.

## Binding the parent

```jul
static proc Boss {
    var last : Int := 0
    constructor initially(args : List<String>) {}
    transition start(x : Int) { guard: x = 1 }
    transition done(y : Int) { transit: last := y }
}

proc Sys := Boss || Worker<Boss>
```

A bare dynamic (`Boss || Worker`) is also legal: it has no parent link, so it may sync with statics (and act as a parent for others) but not dyn↔dyn with an unrelated dynamic.

Rules for `B<A>`:

- Intermediate compositions may be open: `proc D := Worker<Boss> || Other` is legal. A later `D || Boss` closes it.
- Each **compiled program** (each proc/spec that produces an artifact) must have every `B<A>` parent present as a leaf class in its flattened composition.
- One class may bind to several parents: `BroadcastClaim<Put> || BroadcastClaim<ApplyMoveBatch>`. Each binding is a separate occurrence (TLA leaves `BroadcastClaim_Put`, `BroadcastClaim_ApplyMoveBatch`).
- `C<B>` names the **class** `B` and covers every occurrence of it. There is no nested `C<B<A>>` syntax. Each `C` instance still links to the specific `B` instance that spawned it.

## Returns

```jul
return done(y : Int) {
    guard: y = acc
}

internal return finish() {
    guard: ready
}
```

- Named, with normal modifiers (`internal`, `client`, `service`, ordinary).
- Body: `guard:` and `before:` only — no `transit:` / `after:` (state is discarded).
- Taking a return terminates the instance and frees its TLA slot (`constructed` cleared).
- Replaces the old `exitProc()` effect. `exitProgram(code)` remains for whole-process exit.
- Only `static proc` may declare `constructor initially`.

## Dynamic–dynamic sync

Only between a parent-bound occurrence and its parent:

- Parent action ↔ child constructor (spawn).
- Parent action ↔ child transition / return.
- A bound child may still sync with **static** procs.
- Unbound dynamics sync with statics and with their own children only.
- Compile error if one parent action could pair with two different child classes (or a child class and a non-child partner).

Orphans are allowed: a child may outlive its parent. After the parent returns, actions shared with it are never enabled again.

## HTTP handlers (`~>`)

```jul
serveHttp(port, HandleRpc.req ~> HandleRpc.resp)
```

- `HandleRpc.req` is a constructor taking `HttpServerRequest`.
- `HandleRpc.resp` is an action (typically a `return`) taking `HttpServerResponse`.
- Per request, the runtime constructs a `HandleRpc` instance via `req` and takes the response when `resp` fires.
- TLA emits the property `HandleRpc.req ~> HandleRpc.resp` per slot.

## TLA

- Every dynamic leaf is indexed by `DynProc`: `X[i]` or `X[n][i]` under a create-index.
- Parent-bound classes get `<B>_parent` per slot. When the parent class has several occurrences, the link is `<<occurrence, slot>>`.
- `<Class>Terminates == \A slot : constructed[slot] ~> ~constructed[slot]`.
- Spec includes strong fairness (`SF`) for system-controlled actions. See [Composition and actions — Fairness](composition-and-actions.md#fairness).

## See also

- [Processes](processes.md) — static vs dynamic, `initially`, returns
- [Composition and actions](composition-and-actions.md) — shape rules, fairness
- [Side effects](effects.md) — `serveHttp`
