# Parent/child inference

Parenthood is **inferred** at each binary `||` step. There is no `Child<Parent>` or `parentof` syntax: nesting *is* the binding. Dynamic–dynamic synchronization is allowed only between an inferred child occurrence and its parent (in both directions). This replaces the old `procfun` / `api` / expression-call model: request/response is an ordinary dynamic proc with a constructor (request) and a `return` action (reply).

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

- The declaration never names the parent. Parenthood comes from who the child syncs with under `||`.
- Dynamic procs have no `constructor initially`. They exist only by being spawned.
- Every dynamic proc must have at least one `return` action. Static procs may have none.

## Inferring the parent

```jul
static proc Boss {
    var last : Int := 0
    constructor initially(args : List<String>) {}
    transition start(x : Int) { guard: x = 1 }
    transition done(y : Int) { transit: last := y }
}

proc Sys := Boss || Worker
```

At the compose step where `start` / `done` meet, the compiler orients the edge:

1. **Constructor** on exactly one class → that class is the **child**; the peer is the **parent**.
2. Else **return** on exactly one class → that class is the **child**.
3. Else reuse a parent already inferred for that occurrence.
4. Constructor↔constructor, or ≥2 dynamics with no orientation → **error**.

Nesting disambiguates multi-parent reuse:

```jul
(Put || BroadcastClaim) || (ApplyMoveBatch || BroadcastClaim)
```

Each `BroadcastClaim` seals `broadcastClaim` / `broadcastDone` with its own parent; the two leaves never compete. A later `SendClaim` can still meet every still-external `BroadcastClaim` action (covers-all occurrences of the parent class).

Rules:

- Open intermediate assemblies are fine; edges form when the child meets its parent under `||`.
- Each **compiled program** must include every inferred parent class as a leaf in its flattened composition.
- Same class under several parents → separate occurrences (TLA leaves `BroadcastClaim_Put`, `BroadcastClaim_ApplyMoveBatch`).
- A child of class `B` that syncs with class `C` covers **every** open occurrence of `C` at that step.

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

Only between an inferred child occurrence and its parent:

- Parent action ↔ child constructor (spawn).
- Parent action ↔ child transition / return.
- A child may still sync with **static** procs.
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

- Every dynamic leaf is indexed by `DynProc`: `X[i]` or `X[n][i]` under a create-index. If the create-index omits `DynProc := { … }`, TLC gets `{"dp1", "dp2"}` (or `dpp1`… when those strings are already used elsewhere in the model).
- Invariants may read dual-index state as `X[n][i].var` with `forall i : DynProc` (requires `type DynProc`). Comparisons desugar to conjoin `X_constructed[n][i]`.
- Parent-bound classes get `<B>_parent` per slot. When the parent class has several occurrences, the link is `<<occurrence, slot>>`.
- `<Class>Terminates == \A slot : constructed[slot] ~> ~constructed[slot]` (always emitted in the `.tla`).
- Spec includes strong fairness (`SF`) over every `Next` action. See [Composition and actions — Fairness](composition-and-actions.md#fairness).
- **AG Obls:** the `.cfg` uses `SpecSafety` and omits `PROPERTY *Terminates` — open env injectors make termination meaningless for the default safety campaign. See [Specifications — AG Obl `.cfg`](specifications.md#ag-obl-cfg-safety-by-default-no-terminates).

## See also

- [Processes](processes.md) — static vs dynamic, `initially`, returns
- [Composition and actions](composition-and-actions.md) — shape rules, fairness
- [Side effects](effects.md) — `serveHttp`
