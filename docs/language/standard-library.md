# Standard library

Julay ships libraries under `julay.proclib` and `julay.funlib`. Import what you need; search order is described in [Modules](modules.md).

## Proclib (Julay source)

Embedded under `src/main/resources/stdlib/julay/proclib/`:

| Module | Import | Role |
|--------|--------|------|
| **Println** | `julay.proclib.Println` | `service println(msg)` → after `println` |
| **ExitSystem** | `julay.proclib.ExitSystem` | `service exitSystem()` → `exitProcess` |
| **Readln** | `julay.proclib.Readln` | Prompt / read line handshake |
| **Timer** | `julay.proclib.Timer` | Cancellable timer: `createTimer`, `startTimer`, `timeout`, `cancelTimer` (`killSessionPeer(TimerHelper)`); helper end uses `exitSession(TimerHelper)` |

Compose them into your system like any other proc, e.g. `Server \|\| Println \|\| HttpServer`.

## Proclib (Kotlin-native)

| Module | Import | Session-style API (summary) |
|--------|--------|------------------------------|
| **HttpServer** | `julay.proclib.HttpServer` | `createHttpServer`, `receiveRequest`, `sendResponse`, `closeHttpServer` |
| **HttpClient** | `julay.proclib.HttpClient` | `createHttpClient`, `sendRequest`, `receiveResponse`, `closeHttpClient` |

Builtin object types: `HttpServerRequest` / `HttpServerResponse`, `HttpClientRequest` / `HttpClientResponse`.

These are the backbone of the [echo](../examples/echo-server.md), [inc](../examples/inc-server.md), and [list](../examples/list-server.md) server examples.

## Funlib

Import as `julay.funlib.<name>`:

| Function | Role | OK in guards? |
|----------|------|----------------|
| `length` | Size of list, set, or map | Yes |
| `parseInt` | String → Int | Yes |
| `startsWith` | String prefix test | Yes |
| `readFile` | Read file contents | **No** |
| `split` | Split string | **No** |
| `trim` | Trim whitespace | **No** |
| `portFromUrl` | Parse port from URL string | **No** |

Guard-unsafe functions throw at compile/check time if used where Z3 encoding is required.

## See also

- [Effects](effects.md)
- [Modules](modules.md)
- [Sessions](sessions.md) — Timer cancel / session teardown
