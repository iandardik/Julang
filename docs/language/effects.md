# Side effects

In Julay, side effects are not ambient language primitives. They come from:

1. **Builtins** for HTTP serving and timers — `serveHttp`, `setTimer`, `cancelTimer` (import as `julay.funlib.<name>`).
2. **Library functions** that perform effects — for example `println()`, `readln()`, `exitProgram(code)`, and outbound HTTP via `doHttpRequest` (or the stdlib `proc HttpRequest` bound as `HttpRequest<Parent>`). Call funlib from action bodies (`before:` / `after:` for void effects; `transit:` RHS for value-returning IO).

Which functions are effectful is listed in [Standard library](standard-library.md). There is no `julay.proclib`; former `HttpServer` / `Timer` library procs are replaced by the builtins below.

## Builtins: HTTP and timers

| Builtin | Role | Notes |
|---------|------|--------|
| `serveHttp(port, Class.ctor ~> Class.action)` | Start an HTTP server; constructs `Class` per request via `ctor`, takes the response when `action` fires | Typically in a static starter’s `after:` — see [Parent-bound procs](children.md#http-handlers) |
| `setTimer(name, ms)` | Arm a named timer; `name` must match a `timer transition` | Allowed in constructor `after:` |
| `cancelTimer(name)` | Cancel a named timer | **Transition-only** (`before:` / `after:`) |

```jul
import julay.funlib.serveHttp

static proc ServerStarter {
    constructor initially(args : List<String>) {
        after:
            serveHttp(8000, Echo.req ~> Echo.resp)
    }
}
```

```jul
import julay.funlib.setTimer

static proc Driver {
    constructor initially(args : List<String>) {
        transit: step := "wait"
        after:
            setTimer("timeout", 50)
    }

    timer transition timeout() {
        guard: step = "wait"
        transit: step := "done"
    }
}
```

`timer transition` offers are occurrence-local. When the timer fires, the run loop selects that transition (subject to its guard).

Session teardown builtins (`exitSession`, `killSessionPeer`) are **removed**.

## Library functions: `before`, `transit`, and `after`

Effectful **functions** (and any other callable) may appear in action clauses as follows. Execution order after a successful sync is:

1. `error:` checks (pre-state)
2. **`before:`** calls (transitions only — constructors cannot have `before:`)
3. **`transit:`** (statement `let`s top-to-bottom against pre-state, then assignment RHSs against pre-state + in-scope lets, then simultaneous apply). In **constructors**, neither let initializers nor assignment RHSs may reference any state variable (including self-updates) — use args / pure exprs, or earlier lets that do not read state.
4. **`after:`** calls

You must import the function first, e.g. `import julay.funlib.println`.

### `before:` and `after:`

Both blocks:

- May call imported funlib functions (including IO) and user `fun`s.
- Do **not** allow state assignments (`x := …`). Only bare calls.
- If a call returns a value, the value is **discarded**.

```jul
import julay.funlib.println

internal transition step() {
    before:
        println("about to step")
    transit:
        x := x + 1
    after:
        println("stepped")
}
```

`after:` is the former `effect:` clause (renamed).

Dynamic procs terminate with a named **`return`** action (not `exitProc`, which is removed). See [Parent-bound procs](children.md#returns).

### IO in `transit:`

**`transit:`** is the only place for state assignments, and it **may** call value-returning IO functions on the RHS (and in statement `let` initializers):

```jul
import julay.funlib.readln
import julay.funlib.readFile

transit:
    readMsg := readln()
    contents := readFile(path)
```

Void functions such as `println` cannot appear in transit expressions—typechecking rejects them because they return no value. Put those in `before:` / `after:`.

### Transit statement `let`

Inside `transit:`, you may declare temporary bindings that are **not** process state:

```jul
transit:
    let id : Int := parseInt(args[2])
    let cfg : CfgPair := parseCfg(args[1], id)
    self := cfg.me
    theCluster := cfg.cluster
    listenPort := portFromUrl(cfg.me.url)
```

Semantics:

1. Each `let` initializer is evaluated top-to-bottom against **pre-transit** state, plus earlier lets in this block. In **transitions**, lets **may** read state variables; in **constructors**, they must not (state is still undefined).
2. Every assignment RHS is evaluated against pre-transit state plus lets textually **above** it.
3. All assignments are then applied **simultaneously** (later lines do not observe earlier assigns).
4. **In constructors only:** neither a let initializer nor an assignment RHS may reference any state variable (including self-updates — no `x := x + 1` or `let t := x`). Use constructor args, pure expressions, or earlier lets that do not read state. Inline `var`/`const` initializers likewise must not reference state.

```jul
constructor initially(args : List<String>) {
    transit:
        let n : Int := 0
        x := n
        y := n + 1
}
```

Ordinary **transitions** may still read any state in assign RHSs (`log := log + …`).

Rules:

- Lets are scoped to this transit only; they do not persist as `var`/`const`.
- Lets are not assignable targets (`t := …` after `let t …` is an error).
- A let may shadow a state var or arg; the initializer may still refer to the outer same-named symbol (as with [expression `let`](types-and-expressions.md#expression-let)).
- Prefer statement `let` when one value must feed **several** assigns; use expression `let` for a binding inside a single expression.

### TLA+ translation (IO havoc)

`before:` / `after:` do not appear in the TLA+ action (runtime-only).

When a **transit** assignment’s RHS involves IO such as `readln()` or `readFile(...)` (including via a substituted transit `let`), the TLA+ encoding **havocs** the target: the next value is chosen nondeterministically from the variable’s domain. For example:

```jul
import julay.funlib.readln

transition x() {
    transit: stateVar := readln()
}
```

```tla
x ==
  /\ stateVar' \in String
```

`readFile(path)` is treated the same way (file contents are unknown, so any `String`). Nested IO inside a larger RHS (e.g. `trim(readln())`) also havocs the assign target at its type.

### TLA+ translation (`error:`)

`error:` arms still throw at runtime. TLA+ encodes the **negation** of each condition as an assumption conjunct **before** `\* … logic`, so the action is enabled only on the non-error path. Top-level `~in` / `~=` / `~` flip to `\in` / `=` / the inner formula; other conditions are wrapped with `~`. List membership uses a generated `Range` helper (`me \in Range(cluster)`). Constructors still cannot have `guard:`; `error:` is how constructor arguments are constrained in the spec.

## See also

- [Standard library](standard-library.md) — builtins and funlib
- [Processes](processes.md)
- [Types and expressions](types-and-expressions.md) — expression `let`
- [Specifications](specifications.md)
