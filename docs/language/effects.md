# Side effects

In Julay, side effects are not ambient language primitives. They come from the **standard library** in one of two ways:

1. **Library processes** that perform effects (for example `HttpServer`, `Timer`) — compose them into your system and synchronize on their actions.
2. **Library functions** that perform effects (for example `println()`, `readln()`, `exitProcess()`) — import them from `julay.funlib` and call them from action bodies (`before:` / `after:` for void effects; `transit:` RHS for value-returning IO).

Which procs and functions are effectful is listed in [Standard library](standard-library.md).

## Library functions: `before`, `transit`, and `after`

Effectful **functions** (and any other callable) may appear in action clauses as follows. Execution order after a successful sync is:

1. `error:` checks (pre-state)
2. **`before:`** calls
3. **`transit:`** assignments (RHS evaluated simultaneously against pre-transit state, then applied)
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

`after:` is the former `effect:` clause (renamed). Session teardown functions (`exitSession`, `killSessionPeer`) are **transition-only** and may appear in `before:` or `after:` (not constructors). Their argument is a bare leaf proc-class name. See [Sessions](sessions.md).

### IO in `transit:`

**`transit:`** is the only place for state assignments, and it **may** call value-returning IO functions on the RHS:

```jul
import julay.funlib.readln
import julay.funlib.readFile

transit:
    readMsg := readln()
    contents := readFile(path)
```

Void functions such as `println` cannot appear in transit expressions—typechecking rejects them because they return no value. Put those in `before:` / `after:`.

### TLA+ translation (IO havoc)

`before:` / `after:` do not appear in the TLA+ action (runtime-only).

When a **transit** assignment’s RHS involves IO such as `readln()` or `readFile(...)`, the TLA+ encoding **havocs** the target: the next value is chosen nondeterministically from the variable’s domain. For example:

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

See also [Specifications](specifications.md).

## See also

- [Standard library](standard-library.md) — which procs and functions have effects
- [Processes](processes.md)
- [Sessions](sessions.md)
- [Specifications](specifications.md)
