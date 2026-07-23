# Effects

**Effects** run after a successful synchronized step. They perform side effects (I/O, delays, session teardown). They are distinct from `transit`, which only updates local state.

```jul
internal transition println(msg : String) {
    guard: print & (msg = x + "")
    transit: print := false
    effect: println(msg)
}
```

## Builtin effects

| Builtin | Signature | Notes |
|---------|-----------|--------|
| `println` | `println(String)` | Print a line |
| `readln` | `readln()` → `String` | Read a line (used via assign in effect position) |
| `exitProcess` | `exitProcess()` | Halt the process / program |
| `delaySeconds` | `delaySeconds(Int)` | Sleep for N seconds |
| `exitSession` | `exitSession(PeerClass)` | End session affinity with the named leaf proc class; both keep running. No-op if that affinity is absent. |
| `killSessionPeer` | `killSessionPeer(PeerClass)` | End session with the named peer class and cancel that peer. No-op if that affinity is absent. |

`exitSession` and `killSessionPeer` are **transition-only** (not allowed on constructors). The argument is a bare leaf proc-class name (e.g. `TimerHelper`), not a composition alias. See [Sessions](sessions.md).

## Effects vs funlib

| | Effects | `julay.funlib` |
|--|---------|----------------|
| Where | `effect:` clause | Expressions (`guard`, `transit`, …) |
| Purpose | Imperative side effects | Pure-ish helpers / string & collection ops |
| Example | `println(msg)` | `parseInt(s)`, `length(list)` |

Some funlib functions cannot appear in guards (no Z3 encoding)—see [Standard library](standard-library.md).

## Stdlib wrappers

Many effects are exposed as **service** or **session** procs in the stdlib (e.g. `julay.proclib.Println`) so other procs synchronize on `println` rather than calling the builtin directly. Prefer the stdlib pattern in larger systems.

## See also

- [Processes](processes.md)
- [Standard library](standard-library.md)
- [Sessions](sessions.md)
