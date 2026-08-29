# Procfuns

A **procfun** is a process-backed blocking call: it looks like a function at the call site, but desugars to a short-lived process with local state and transitions until a synthetic return edge yields a value to the caller.

It is **not** the same as:

| Construct | Role |
|-----------|------|
| Pure `fun` | Expression-inlined helper (no state, no steps) |
| `proc` | Peer in `\|\|` composition, typically SyncChannel-started |

## Mental model: desugaring

A procfun is sugar over an ordinary proc shape. This is the most important way to read procfun code.

**Source:**

```jul
procfun clientGetRPC(req : HttpServerRequest) : HttpServerResponse {
    client transition getCommitted(stateMachine : List<String>) {
        return:
            HttpServerResponse {
                body := stateMachine + "",
                code := 200
            }
    }
}
```

**Desugars to (conceptual `proc`):**

```jul
proc clientGetRPC {
    const req : HttpServerRequest
    var retVal : HttpServerResponse          // TLA Init: retVal \in HttpServerResponse

    constructor clientGetRPC_call(req : HttpServerRequest) {
        transit:
            req := req
    }

    client transition getCommitted(stateMachine : List<String>) {
        transit:
            retVal := HttpServerResponse {
                body := stateMachine + "",
                code := 200
            }
    }

    transition clientGetRPC_ret(ret : HttpServerResponse) {
        guard:
            ret = retVal
    }
}
```

Rules of the desugar:

| Source | Becomes |
|--------|---------|
| Parameters | Implicit `const`s (plus bind in `F_call`) |
| `return: e` | `transit: retVal := e`; sync modifier unchanged (`client` / `session` / `internal` / bare). Completion is the synthetic `_ret` edge |
| — | Synthetic `F_call` constructor (spawn entry) |
| — | Synthetic `F_ret` completion edge (`guard: ret = retVal`) |

At **runtime**, a call still feels like one logical return: the caller spawns the instance from the call site (not via a SyncChannel peer in `||`), blocks until completion, then resumes with the value. The synthetic `_ret` is the completion edge in the IR; the caller does not manually sync on it.

### Alphabets in VS Code / `analyze --json`

- **Standalone procfun** (`analyze -s F`): user transitions appear under **external** (including steps tagged `internal` and bare-return edges). Synthetic `F_call` / `F_ret` are omitted.
- **Parent assembly** (e.g. `RpcIn`): every procfun **called** by a host under that assembly contributes its non-synthetic offers to the parent alphabet — **whether or not** the helper is listed in `||`. Source-internal stays out of the parent's external list; unmatched `client` / bare actions (e.g. `noLeader`) stay external on the parent.

## Spawn-and-await

Evaluating a procfun call suspends the caller's current action, runs a fresh instance until `_ret` completes, then resumes with the returned value. While the instance is live it can take `internal` steps alone and `client` / ordinary / `session` steps with peers in the surrounding program.

```mermaid
sequenceDiagram
  participant Caller as CallerProc
  participant Call as procfun_call
  participant Child as procfun_instance
  participant Prov as ProviderPeer
  Caller->>Call: evaluate clientGetRPC(req)
  Call->>Child: spawn args as consts
  loop until F_ret
    alt internal
      Child->>Child: solo step
    else client
      Child->>Prov: sync on provider action
    end
  end
  Child->>Call: retVal via F_ret
  Call->>Caller: resume with result
```

## Callable procfun references (`~>`)

Register a procfun as a handler or callback by type `A ~> R` (or `(A, B) ~> R` for multiple arguments). At runtime the value is the procfun name as a `String`; guards compare with `handler = myHandler`.

```jul
session transition listen(
    port : Int,
    handler : HttpServerRequest ~> HttpServerResponse
) {
    guard: ~started & port = 8000 & handler = echoHandler
    transit: started := true
}
```

See [Standard library](standard-library.md) for HttpServer + `httpRequest`.

## Syntax

```jul
procfun parseCfg(cfg : String) : Set<Node> {
    var rawLines : List<String> := split(readFile(cfg), "\n")
    var idx : Int := 0
    var clusterBuilder : Set<Node> := {}

    internal transition parseAndBuildCluster() {
        guard: idx < length(rawLines)
        transit:
            // update clusterBuilder / idx …
            idx := idx + 1
    }

    transition endParse() {
        guard: idx = length(rawLines)
        return: clusterBuilder
    }
}
```

### Rules

| Rule | Detail |
|------|--------|
| Args | Each parameter is an implicit `const`. Do not redeclare it as `var`/`const`. |
| Inline init | `var x : T := expr` / `const c : T := expr` is allowed. Init may use args and pure expressions, but **not** state variables (use a constructor + transit `let` instead). |
| Init XOR | A state name is initialized **either** inline **or** in the procfun's single optional constructor — never both. |
| Step modifiers | Any of bare / `internal` / `client` / `session`. **`provider` is forbidden.** |
| Reserved names | User transitions/ctors/vars cannot be named `initially`, `F_call`, `F_ret`, or `retVal`. |
| Return | `return: expr` on a transition (any allowed modifier). Mutually exclusive with `transit:` / `error:` / `after:`. Use `before:` for side effects on return steps (e.g. logging before the value is delivered). Desugars to `retVal := expr` without changing the transition's sync tag. Synthetic `_ret` is the completion edge (alphabet / TLA); runtime still delivers the value when the return-bearing step fires. ≥1 return required. |
| `exitProc` | **Compile error** inside procfun bodies. Procfuns must finish via `return:`; `exitProc` is only for ordinary procs. Without `return:`, a silent child exit would leave the host with `JulayException("… exited without return")` and TLA `Host_blocking` stuck. |
| Call sites | Only in **transit RHS** (like value-returning effectful funlib). Not in guards or pure `fun` bodies. |
| Recursion | Direct/mutual recursion among procfuns is rejected (loop with `internal` transitions instead). |

## Composition and TLA coupling

Procfun instances are **spawned from the caller** and block until completion — they are **not** SyncChannel peers.

**Procfuns cannot appear in `||`** (compile error). To couple a procfun in TLA+, list it in an [api](composition-and-actions.md#apis)'s `calls:` and include that api in the spec.

| Situation | TLA |
|-----------|-----|
| `F` listed in `calls` of an api in the composition | Full coupling: host `act_call` / `act_ret` + child occurrence |
| `F` called but not listed in any composed api's `calls` | Havoc return (`out' ∈ Ret`) + warning |
| `Main \|\| F` | **Compile error** |

**Alphabet (analyze / IDE):** If a host under assembly `P` calls procfun `F`, `F`'s non-synthetic offers fold into `P`'s alphabet automatically. Listing `F` in an api's `calls` is for TLA coupling and packaging, not for alphabet folding.

**TLA coupling shape:** Coupled call sites still need a whole-RHS form (`x := F(...)` or `x := Api.F(...)`) for the spawn-await split. Nested-only calls are enough for alphabet, but stay havoc for coupling.

### Soundness of havoc

Havocing an uncoupled procfun is a **weaker** model than including it via api `calls`. A proof on the havoced spec still implies correctness of the refined system. Prefer listing helpers you care about in an api's `calls:`.

## Caller example

```jul
proc RaftNodeMain {
    const self : Node
    const cluster : Set<Node>
    const listenPort : Int

    constructor initially(args : List<String>) {
        transit:
            let id : Int := parseInt(args[2])
            let cfg : CfgPair := ParseApi.parseCfg(args[1], id)
            self := cfg.me
            cluster := cfg.cluster
            listenPort := portFromUrl(cfg.me.url)
    }
}

api ParseApi {
    proc: RaftNodeMain
    calls: parseCfg
}

spec FullNode := ParseApi
```

Outside an api, call listed procfuns as `ApiName.fn(...)`. See [Composition and actions — APIs](composition-and-actions.md#apis).

## Client steps with return

`return:` may sit on a `client` (or bare / `session` / `internal`) transition — no need for a separate untagged return step:

```jul
procfun fetchAndInc() : Int {
    var n : Int := 0

    client transition getCounter(counterVal : Int) {
        guard: true
        return: counterVal + 1
    }
}
```

## Illegal examples

```jul
// ERROR: provider forbidden
provider transition serve() { transit: x := 1 }

// ERROR: return + transit together
transition done() {
    return: i
    transit: i := 0
}

// ERROR: reserved name
transition initially() { return: 0 }

// ERROR: orphan composition — use an api's calls: instead
proc Bad := Other || parseCfg

// ERROR: procfun in parallel composition
spec S := Main || countUp || countUp

// ERROR: compile a procfun as a JAR root is rejected; use compile for TLA/analyze only
// (procfuns are not SyncChannel-started top-level peers)
```

## Inline init vs constructor

```jul
procfun ok(x : Int) : Int {
    var a : Int := x + 1          // inline
    var b : Int                   // constructor
    constructor __user() {
        transit: b := 0
    }
    transition done() { return: a + b }
}
```

## TLA+ / verification

When a spec reaches a procfun call:

1. **Whitelist via `||`** — only procfuns listed in the composition are coupled; others havoc.
2. **Per-call-site occurrences** — each textual call is a distinct leaf when coupled.
3. **Parent index inheritance** — indexed hosts yield indexed occurrences.
4. **Spawn-and-await split** — host `act_call` starts the child and sets `Host_blocking`; after child `F_ret` sets `returnTo_act`, host `act_ret` writes `retVal` into the assign target and clears blocking.
5. **`terminated`** — boolean state; `F_ret` sets `terminated' = TRUE`. Further child steps require `~terminated`.
6. **Liveness** — `<Occ>Terminates == constructed ~> terminated` (universally quantified when indexed).

### Example: coupled `countUp`

```jul
procfun countUp(n : Int) : Int {
    var i : Int := 0
    var result : Int := 0

    internal transition step() {
        guard: i < n
        transit:
            result := i + 1
            i := i + 1
    }

    transition done() {
        guard: i = n
        return: result
    }
}

proc Main {
    var out : Int
    constructor initially(args : List<String>) {
        transit: out := countUp(2)
    }
}

api CountUpApi {
    proc: Main
    calls: countUp
}

spec CountUpSpec := CountUpApi
```

Schematic TLA:

```tla
\* initially calls the procfun countUp before executing
initially_call ==
  /\ ~Main_constructed
  /\ ~Main_blocking
  /\ call_countUp' = TRUE
  /\ Main_blocking' = TRUE
  /\ countUp_constructed' = TRUE
  /\ countUp_n' = 2
  /\ i' = 0
  /\ result' = 0
  /\ UNCHANGED <<out, countUp_terminated, returnTo_initially, Main_constructed, retVal>>

\* countUp steps (step / done / countUp_ret) — countUp_ret sets terminated' and returnTo_initially'

\* The guards for initially appear in initially_call
initially_ret ==
  /\ returnTo_initially
  /\ Main_blocking
  /\ out' = retVal
  /\ Main_constructed' = TRUE
  /\ Main_blocking' = FALSE
  /\ returnTo_initially' = FALSE
  /\ UNCHANGED <<countUp_constructed, countUp_terminated, countUp_n, i, result, call_countUp>>

countUpTerminates == countUp_constructed ~> countUp_terminated
```

### Example: havoc (uncomposed)

```jul
spec Coarse := Main   \* warning: countUp called but not composed
```

Host collapses to a single step, e.g. `out' \in Int` — no child vars, no blocking, no `Terminates` for that helper.

## See also

- [Processes](processes.md)
- [Composition and actions](composition-and-actions.md)
- [Side effects](effects.md)
- [Specifications](specifications.md)
- [Types and expressions](types-and-expressions.md) — pure `fun`
