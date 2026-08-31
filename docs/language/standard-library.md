# Standard library

Julay ships libraries under `julay.proclib` and `julay.funlib`. Import what you need; search order is described in [Modules](modules.md).

Side effects in Julang come only from this catalog: **proclib processes**, **procfun I/O**, and **funlib functions** marked as effectful below. See [Side effects](effects.md) for how function-style IO fits into `before` / `transit` / `after`.

## HTTP (canonical)

### HttpServer (Kotlin-native proclib — JAR only, not in TLA specs)

Compose as `ServerStarter || HttpServer`. The user proc registers a **procfun handler** at startup; the library invokes it per request via `invokeProcFun` (no session pairing per request). `listen` installs a cached thread pool on the JDK `HttpServer` so concurrent requests run in parallel (default JDK behavior serializes handlers on one thread).

| Action | Role |
|--------|------|
| `listen(port, handler : Req ~> Resp)` | Start JDK server on `port`; `handler = myHandler` in the guard wires the procfun |
| `close()` | Stop the server |

- Handler procfuns live in the same module, are **not** exported, and are **not** listed in `api calls:`.
- `listen` / `close` are **session** transitions (pair `ServerStarter` with the library at startup).
- Removed: `receiveRequest`, `sendResponse`, handler procs with session constructors.

```jul
procfun echoHandler(req : HttpServerRequest) : HttpServerResponse {
    internal transition respond() {
        return: HttpServerResponse { body := req.body + "!", code := 200 }
    }
}

proc ServerStarter {
    session transition listen(
        port : Int,
        handler : HttpServerRequest ~> HttpServerResponse
    ) {
        guard: ~started & port = 8000 & handler = echoHandler
        transit: started := true
    }
}

export proc Server := ServerStarter || HttpServer
```

### HttpClient (replaced by procfun)

There is **no** `julay.proclib.HttpClient` and **no** `|| HttpClient` composition.

| API | Role |
|-----|------|
| `import julay.funlib.httpRequest` | Blocking procfun: `httpRequest(req : HttpClientRequest) : HttpClientResponse` |
| `doHttpRequest` (internal) | Kotlin effect used by the stdlib procfun; one fresh connection per call |

```jul
let resp : HttpClientResponse := httpRequest(HttpClientRequest {
    url := "http://localhost:8000",
    method := "POST",
    body := "hello"
})
```

Builtin object types: `HttpServerRequest` / `HttpServerResponse`, `HttpClientRequest` / `HttpClientResponse`.

**No longer true:** `EchoHandler` session proc, `ClientLogic || HttpClient` session cycle, HTTP as the canonical [Sessions](sessions.md) example.

## Proclib

Compose these like any other proc, e.g. `Server || HttpServer` or `Driver || Timer`.

| Module | Import | Role | Effects? |
|--------|--------|------|----------|
| **Timer** | `julay.proclib.Timer` | Cancellable timer (`createTimer`, `startTimer`, `timeout`, `cancelTimer`); `startTimer` takes milliseconds; uses `delayMillis`, `exitSession`, `killSessionPeer` | Yes (delay, session teardown) |
| **HttpServer** | `julay.proclib.HttpServer` | Lifecycle only: `listen(port, handler)`, `close` | Yes (network I/O; Kotlin-native) |

These HTTP pieces are the backbone of the [echo](../examples/echo-server.md), [inc](../examples/inc-server.md), [list](../examples/list-server.md), and [rpc](../examples/rpc-server.md) server examples.

## Funlib

Import as `julay.funlib.<name>`. Bare names are not available without an import.

| Function / procfun | Role | Effects / IO? | OK in guards? |
|----------|------|---------------|----------------|
| `httpRequest` | Blocking HTTP client procfun | **Yes** (network; one connection per call) | **No** |
| `listOf` | List constructor (`listOf(a, b)` / typed `listOf()` / `listOf<T>()`) | No | Yes |
| `setOf` | Set constructor (`setOf(a, b)` / typed `setOf()` / `setOf<T>()`) | No | Yes |
| `mapOf` | Map constructor (`mapOf(k to v, …)` / typed `mapOf()` / `mapOf<K, V>()`) | No | Yes |
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
| `exitProc` | Cancel the calling proc's coroutine (void); **not allowed in procfuns (compile error)** | **Yes** | **No** |
| `delaySeconds` | Sleep for N seconds (void) | **Yes** | **No** |
| `delayMillis` | Sleep for N milliseconds (void) | **Yes** | **No** |
| `exitSession` | End session with named peer class (void; transition-only) | **Yes** | **No** |
| `killSessionPeer` | End session and cancel peer (void; transition-only) | **Yes** | **No** |

Void effectful functions belong in `before:` / `after:`. Value-returning IO may appear on `transit:` RHS or procfun `return:`. Details: [Side effects](effects.md).

Guard-unsafe functions throw at compile/check time if used where Z3 encoding is required.

## Optional

`import julay.optional.Optional`, `julay.optional.some`, and `julay.optional.none`. Julang has no sum types, so `Optional<T>` is a product:

```jul
obj Optional<T> {
    present : Boolean
    value : T
}
```

- `some(v)` — `present = true`, `value = v`
- `none(placeholder)` — `present = false`; `placeholder` is required because fields cannot be omitted. Only read `.value` when `.present` is true.

`.present` / `.value` are ordinary field reads (guard-safe). Equality is structural: `some(5) ~= none(0)` even when the placeholder is `0`.

See also [Types and expressions](types-and-expressions.md) (polymorphic objs, callable ref types `~>`).

## See also

- [Creating libraries](creating-libraries.md) — how to add `.jul` or Kotlin-native procs/funs
- [Procfuns](procfun.md) — handler registration, `httpRequest`, liveness `constructed ~> terminated`
- [Side effects](effects.md)
- [Modules](modules.md)
- [Sessions](sessions.md) — Timer cancel / session teardown (HTTP no longer uses sessions)
