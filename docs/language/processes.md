# Processes

A **proc** is a named transition system: local state plus constructors and transitions that engage with **actions**.

## Proc classes

```jul
proc Counter {
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

You can also name a composed system:

```jul
proc IncServer := Counter || Printer || ServerLogic
```

See [Composition and actions](composition-and-actions.md).

## Programs as sequences of actions

A Julay program is a **sequence of actions**. Procs participate by either:

| Form | Role |
|------|------|
| **Constructor** | Create / initialize a proc instance upon an action |
| **Transition** | Take a step of an existing instance upon an action |

Every **program** begins with the **`initially`** action. Entry procs declare:

```jul
constructor initially(args : List<String>) {
    transit: /* initialize state */
}
```

Compiled JARs pass command-line arguments into `args`. After `initially`, execution continues through user-defined actions. Procs may also declare **additional constructors** for spawning (for example session constructors that create a handler per request)—those run after the program has already started via `initially`.

## Transition anatomy

A transition (or constructor body) may include:

| Clause | Meaning |
|--------|---------|
| `guard:` | Boolean condition; must hold (with peers’ constraints) to take the step |
| `before:` | Calls before state updates (IO allowed; no assignments)—see [Before/after and IO](effects.md) |
| `transit:` | State updates for this proc (may include IO in expressions)—see [Before/after and IO](effects.md) |
| `error:` | Alternate updates when the step is taken in an error path (when used) |
| `after:` | Calls after state updates (IO allowed; no assignments)—see [Before/after and IO](effects.md) |

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

Omitted clauses default sensibly (`guard` true when absent, empty `before` / `transit` / `after` when absent).

## Choosing actions

At each step a proc offers the actions it can currently take (enabled by guards). Synchronization with peers decides which joint step occurs. The same action name can appear on different proc classes so they can sync; see [Composition and actions](composition-and-actions.md).

## See also

- [Language overview](README.md)
- [Effects](effects.md)
- [Inc server example](../examples/inc-server.md) — `Counter` as a service interface
