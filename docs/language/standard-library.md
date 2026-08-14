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
| `listOf` | List constructor (`listOf(a, b)` / typed `listOf()`) | No | Yes |
| `setOf` | Set constructor (`setOf(a, b)` / typed `setOf()`) | No | Yes |
| `mapOf` | Map constructor (`mapOf(k to v, …)` / typed `mapOf()`) | No | Yes |
| `splice` | List slice `splice(xs, start, end)` — 1-based inclusive; clamp `end` to `length`; `end=0` or `start` past end → empty (see [Collections](collections.md#splicing)) | No | Yes |
| `allDistinct` | `true` iff every list element is unique (empty list is `true`; short-circuits on the first duplicate) | No | **No** (unless only concrete state; see [Collections](collections.md#guards)) |
| `length` | Size of list, set, or map | No | Yes |
| `map` | Apply a unary named `fun` or lambda to each element of a list or set (`map(xs, f)`); returns `List`/`Set` of results. Prefer method form [`.map`](collections.md#methods-and-lambdas) | No | **No** (unless only concrete state; see [Collections](collections.md#guards)) |
| `max` | Larger of two `Int`s | No | Yes |
| `min` | Smaller of two `Int`s | No | Yes |
| `parseInt` | String → Int | No | Yes |
| `startsWith` | String prefix test | No | Yes |
| `split` | Split string | No | **No** |
| `trim` | Trim whitespace | No | **No** |
| `portFromUrl` | Parse port from URL string | No | **No** |
| `readFile` | Read file contents → `String` | **Yes** (file I/O; TLA+ havocs) | **No** |
| `println` | Print a line (void) | **Yes** (stdout) | **No** |
| `readln` | Read a line → `String` | **Yes** (stdin; TLA+ havocs) | **No** |
| `exitProgram` | Halt the program with an `Int` return code (void) | **Yes** | **No** |
| `exitProc` | Cancel the calling proc’s coroutine (void); **not allowed in procfuns (compile error)** | **Yes** | **No** |
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
