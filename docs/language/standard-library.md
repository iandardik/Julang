# Standard library

Julay ships **builtins** and **funlib** helpers under `julay.funlib`. Import what you need; search order is described in [Modules](modules.md).

There is **no** `julay.proclib`. Former library procs `HttpServer` and `Timer` are replaced by builtins (`serveHttp`, `setTimer` / `cancelTimer` + `timer transition`). See [Side effects](effects.md).

## HTTP

### Serving (`serveHttp`)

| API | Role |
|-----|------|
| `import julay.funlib.serveHttp` | `serveHttp(port, handler)` — start HTTP on `port`; `handler` is a procfun name |

```jul
import julay.funlib.serveHttp

procfun echoHandler(req : HttpServerRequest) : HttpServerResponse {
    internal transition respond() {
        return: HttpServerResponse { body := req.body + "!", code := 200 }
    }
}

static proc ServerStarter {
    constructor initially(args : List<String>) {
        after:
            serveHttp(8000, echoHandler)
    }
}

export proc Server := ServerStarter
```

Handler procfuns live in the same module. Request/response types are builtins: `HttpServerRequest` / `HttpServerResponse`.

### Client (`httpRequest`)

| API | Role |
|-----|------|
| `import julay.funlib.httpRequest` | Blocking procfun: `httpRequest(req : HttpClientRequest) : HttpClientResponse` |

```jul
let resp : HttpClientResponse := httpRequest(HttpClientRequest {
    url := "http://localhost:8000",
    method := "POST",
    body := "hello"
})
```

Builtin object types: `HttpClientRequest` / `HttpClientResponse`.

## Timers

| API | Role |
|-----|------|
| `import julay.funlib.setTimer` | `setTimer(name, ms)` — arm timer; `name` matches a `timer transition` |
| `import julay.funlib.cancelTimer` | `cancelTimer(name)` — cancel; **transition-only** |

Declare `timer transition name() { … }` on the same proc. Example: [`regression/input/timer/timer-smoke.jul`](../../regression/input/timer/timer-smoke.jul).

These HTTP/timer pieces are the backbone of the [echo](../examples/echo-server.md), [inc](../examples/inc-server.md), [list](../examples/list-server.md), and [rpc](../examples/rpc-server.md) server examples (servers use `serveHttp`; no Timer proclib in those demos).

## Funlib

Import as `julay.funlib.<name>`. Bare names are not available without an import.

| Function / procfun | Role | Effects / IO? | OK in guards? |
|----------|------|---------------|----------------|
| `serveHttp` | Start HTTP server with procfun handler | **Yes** (network) | **No** |
| `setTimer` | Arm named timer | **Yes** | **No** |
| `cancelTimer` | Cancel named timer (transition-only) | **Yes** | **No** |
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

Void effectful functions belong in `before:` / `after:`. Value-returning IO may appear on `transit:` RHS or procfun `return:`. Details: [Side effects](effects.md).

Guard-unsafe functions throw at compile/check time if used where a pure guard encoding is required.

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

- [Creating libraries](creating-libraries.md) — how to add `.jul` libs
- [Procfuns](procfun.md) — handler registration, `httpRequest`, liveness `constructed ~> terminated`
- [Side effects](effects.md)
- [Modules](modules.md)
