# Processes

A **proc** is a named transition system: local state plus constructors and transitions that engage with **actions**.

## Proc classes

```jul
static proc Counter {
    var counter : Int

    constructor initially(args : List<String>) {
        transit: counter := 0
    }

    service transition increment() {
        transit: counter := counter + 1
    }
}
```

- `var` — mutable local state
- `const` — assigned once (typically in a constructor) and then fixed
- Fields are private to the proc instance

### `static proc` vs `proc`

| Kind | Role |
|------|------|
| **`static proc`** | At most one occurrence in a composition. Exactly one constructor, named `initially`. Cannot be spawned by another class’s constructor. Typical **`service` hub** for a public API, or one side of a **two-static** ordinary sync. |
| **`proc`** (dynamic) | May appear multiple times; may declare additional constructors used for spawning after `initially`. Often a **`client`** of a static service hub; dynamic↔dynamic sync on ordinary actions is illegal. |

You can also name a composed system:

```jul
proc IncServer := Counter || Printer || ServerLogic
```

See [Composition and actions](composition-and-actions.md).

**Leaf specs** use the same curly-body shape as a proc class (`spec Env { … }`) but are not executable processes: they compile only to TLA+ and must not appear in `proc Name := …` assemblies. See [Specifications — Leaf specs](specifications.md#leaf-specs).

## Programs as sequences of actions

A Julay program is a **sequence of actions**. Procs participate by either:

| Form | Role |
|------|------|
| **Constructor** | Create / initialize a proc instance upon an action |
| **Transition** | Take a step of an existing instance upon an action |

Every **program** begins with the **`initially` action**. Entry procs declare:

```jul
constructor initially(args : List<String>) {
    transit: /* initialize state */
}
```

Compiled binaries pass command-line arguments into `args`. After `initially`, execution continues through user-defined actions. Dynamic procs may declare **additional constructors** for spawning (for example a handler constructed per request)—those run after the program has already started via `initially`. Static procs may not be spawned that way.

There are **no sessions**: sticky pairwise session affinity and session teardown builtins are gone. Use **`service` / `client`** for hub APIs that stay in the external alphabet, and **ordinary** (untagged) syncs for composition-private peer meetings under `||`. Keep `static proc`, `internal`, and `timer`.

## Transition anatomy

A **transition** body may include:

| Clause | Meaning |
|--------|---------|
| `guard:` | Boolean condition; must hold (with peers’ constraints) to take the step |
| `before:` | Calls before state updates (IO allowed; no assignments)—see [Before/after and IO](effects.md) |
| `transit:` | State updates and optional statement `let`s (may include IO in expressions)—see [Before/after and IO](effects.md) |
| `error:` | Pre-state checks: at runtime, if a condition holds, throw; no transit / `after:`. In TLA+, each condition is negated and emitted as an enabling assumption (guard). |
| `after:` | Calls after state updates (IO allowed; no assignments)—see [Before/after and IO](effects.md) |

Modifiers: `service` (hub; stays external), `client` (syncs only with a service), `internal` (occurrence-local solo step), `timer` (local timer fire; see [Effects](effects.md)), or none (ordinary sync — composition-private when peers meet under `||`).

**Constructors cannot have `guard:` or `before:`.** A constructor body may only use `transit:`, `error:`, and `after:`. Spawning is gated by peers’ transition guards, not by a guard on the constructor itself. Put setup IO in `after:` (after state is assigned) or in a following transition. Constructor transit `let` initializers and assignment RHSs must not reference any state variable (including self-updates); use args or earlier lets that do not read state — see [Effects](effects.md).

Example (from [`regression/input/basic/test1.jul`](../../regression/input/basic/test1.jul)):

```jul
internal transition println(msg : String) {
    guard:
        print & (msg = x + "")
    transit:
        print := false
    after:
        println(msg)
}
```

Omitted clauses default sensibly (`guard` true when absent on a transition, empty `before` / `transit` / `after` when absent).

## `this` vs action args

Inside a constructor or transition body, a bare name prefers an **action arg** (or transit `let`) when it shares a name with state. Use **`this.x`** to mean the process state variable `x` instead:

```jul
transition set(counter : Int) {
    guard: this.counter ~= counter
    transit: this.counter := counter
}
```

`this.x.f` walks fields on a state `obj` (or collection properties like `this.xs.length`). Bare `this` is illegal. `this` is only allowed in proc / procfun / leaf-spec action bodies—not in `fun` bodies or invariants.

## Choosing actions

At each step a proc offers the actions it can currently take (enabled by guards). Synchronization with peers decides which joint step occurs. The same action name can appear on different proc classes so they can sync; see [Composition and actions](composition-and-actions.md).

## See also

- [Language overview](README.md)
- [Effects](effects.md)
- [Inc server example](../examples/inc-server.md) — `Counter` as a static hub
