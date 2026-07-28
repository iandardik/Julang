# Standard library

Julay ships libraries under `julay.proclib` and `julay.funlib`. Import what you need; search order is described in [Modules](modules.md).

Side effects in Julay come only from this catalog: **proclib processes** and **funlib functions** marked as effectful below. See [Side effects](effects.md) for how function-style IO fits into `before` / `transit` / `after`.

## Proclib

Compose these like any other proc, e.g. `Server || HttpServer` or `Driver || Timer`.

| Module | Import | Role | Effects? |
|--------|--------|------|----------|
| **Timer** | `julay.proclib.Timer` | Cancellable timer (`createTimer`, `startTimer`, `timeout`, `cancelTimer`); uses `delaySeconds`, `exitSession`, `killSessionPeer` | Yes (delay, session teardown) |
| **HttpServer** | `julay.proclib.HttpServer` | `createHttpServer`, `receiveRequest`, `sendResponse`, `closeHttpServer` | Yes (network I/O; Kotlin-native implementation) |
| **HttpClient** | `julay.proclib.HttpClient` | `createHttpClient`, `sendRequest`, `receiveResponse`, `closeHttpClient` | Yes (network I/O; Kotlin-native implementation) |

Builtin object types for HTTP: `HttpServerRequest` / `HttpServerResponse`, `HttpClientRequest` / `HttpClientResponse`.

These HTTP procs are the backbone of the [echo](../examples/echo-server.md), [inc](../examples/inc-server.md), and [list](../examples/list-server.md) server examples.

## Funlib

Import as `julay.funlib.<name>`. Bare names are not available without an import.

| Function | Role | Effects / IO? | OK in guards? |
|----------|------|---------------|----------------|
| `length` | Size of list, set, or map | No | Yes |
| `map` | Apply a unary named `fun` or lambda to each element of a list or set (`map(xs, f)`); returns `List`/`Set` of results. Prefer method form [`.map`](higher-order-functions.md) | No | **No** (unless only concrete state; see HOF docs) |
| `parseInt` | String → Int | No | Yes |
| `startsWith` | String prefix test | No | Yes |
| `split` | Split string | No | **No** |
| `trim` | Trim whitespace | No | **No** |
| `portFromUrl` | Parse port from URL string | No | **No** |
| `readFile` | Read file contents → `String` | **Yes** (file I/O; TLA+ havocs) | **No** |
| `println` | Print a line (void) | **Yes** (stdout) | **No** |
| `readln` | Read a line → `String` | **Yes** (stdin; TLA+ havocs) | **No** |
| `exitProcess` | Halt the process (void) | **Yes** | **No** |
| `delaySeconds` | Sleep for N seconds (void) | **Yes** | **No** |
| `exitSession` | End session with named peer class (void; transition-only) | **Yes** | **No** |
| `killSessionPeer` | End session and cancel peer (void; transition-only) | **Yes** | **No** |

Void effectful functions belong in `before:` / `after:`. Value-returning IO may appear on `transit:` RHS. Details: [Side effects](effects.md).

Guard-unsafe functions throw at compile/check time if used where Z3 encoding is required.

## See also

- [Creating libraries](creating-libraries.md) — how to add `.jul` or Kotlin-native procs/funs
- [Side effects](effects.md)
- [Modules](modules.md)
- [Sessions](sessions.md) — Timer cancel / session teardown
