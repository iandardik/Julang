# Before, after, and IO

Actions may include **`before:`**, **`transit:`**, and **`after:`** clauses (plus `guard:` / `error:`). Execution order after a successful sync is:

1. `error:` checks (pre-state)
2. **`before:`** calls
3. **`transit:`** assignments (RHS evaluated simultaneously against pre-transit state, then applied)
4. **`after:`** calls

## `before:` and `after:`

Both blocks:

- May perform **IO** and call any function/effect builtin (e.g. `println`, `delaySeconds`, `readln`, `readFile`, user `fun`s).
- Do **not** allow state assignments (`x := …`). Only bare calls.
- If a call returns a value, the value is **discarded**.

```jul
internal transition step() {
    before:
        println("about to step")
    transit:
        x := x + 1
    after:
        println("stepped")
}
```

`after:` is the former `effect:` clause (renamed). Session teardown builtins (`exitSession`, `killSessionPeer`) remain transition-only and may appear in `before:` or `after:`.

## IO in `transit:`

**`transit:`** is the only place for state assignments, and it **may perform IO** in expressions. Value-returning IO (and other value-returning effects) can appear on the RHS:

```jul
transit:
    readMsg := readln()
    contents := readFile(path)
```

Void builtins such as `println` cannot appear in transit expressions—typechecking rejects them because they return no value. Put those in `before:` / `after:`.

## Builtin effects

| Builtin | Signature | Notes |
|---------|-----------|--------|
| `println` | `println(String)` | Print a line (`before` / `after`) |
| `readln` | `readln()` → `String` | Read a line (typically assigned in `transit`) |
| `exitProcess` | `exitProcess()` | Halt the process / program |
| `delaySeconds` | `delaySeconds(Int)` | Sleep for N seconds |
| `exitSession` | `exitSession(PeerClass)` | End session affinity with the named leaf proc class; both keep running. No-op if that affinity is absent. |
| `killSessionPeer` | `killSessionPeer(PeerClass)` | End session with the named peer class and cancel that peer. No-op if that affinity is absent. |

`exitSession` and `killSessionPeer` are **transition-only** (not allowed on constructors). The argument is a bare leaf proc-class name (e.g. `TimerHelper`), not a composition alias. See [Sessions](sessions.md).

## TLA+ translation (IO havoc)

`before:` / `after:` do not appear in the TLA+ action (runtime-only), same as the old effect encoding.

When a **transit** assignment’s RHS involves IO such as `readln()` or `readFile(...)`, the TLA+ encoding **havocs** the target: the next value is chosen nondeterministically from the variable’s domain. For example:

```jul
transition x() {
    transit: stateVar := readln()
}
```

```tla
x ==
  /\ stateVar' \in String
```

`readFile(path)` is treated the same way (file contents are unknown, so any `String`). Nested IO inside a larger RHS (e.g. `trim(readln())`) also havocs the assign target at its type.

See also [Specifications](specifications.md) for how specs become TLA+.

## Effects vs funlib

| | Effect builtins / IO | `julay.funlib` |
|--|---------|----------------|
| Where | `before:` / `after:` statements; value-returning forms also in `transit:` exprs | Expressions (`guard`, `transit`, …) |
| Purpose | Imperative side effects and external input | Helpers / string & collection ops |
| Example | `println(msg)`, `readln()` | `parseInt(s)`, `length(list)` |

Some funlib functions cannot appear in guards (no Z3 encoding)—see [Standard library](standard-library.md). Note that `readFile` is funlib but is IO: usable in transit (and havoc’d in TLA+ like `readln`).

## Stdlib wrappers

Many effects are exposed as **service** or **session** procs in the stdlib (e.g. `julay.proclib.Println`) so other procs synchronize on `println` rather than calling the builtin directly. Prefer the stdlib pattern in larger systems.

## See also

- [Processes](processes.md)
- [Standard library](standard-library.md)
- [Sessions](sessions.md)
- [Specifications](specifications.md)
