# Echo server

**Sources:** [`input/echo_server/`](../../input/echo_server/)

| File | Role |
|------|------|
| [`main.jul`](../../input/echo_server/main.jul) | Aliases and `compile` |
| [`server.jul`](../../input/echo_server/server.jul) | HTTP server + procfun handler |
| [`client.jul`](../../input/echo_server/client.jul) | Client dispatcher + per-request logic |

## Intent

A small end-to-end HTTP demo: a server echoes a transformed body (`req.body + " is a good point!"`), and a client issues many POSTs to `http://localhost:8000`. Server and client are **separate compile targets** so you can run them as two processes.

## Layout and composition

```jul
proc EchoServer := Server
proc EchoClient := Client

compile EchoServer, EchoClient
```

### Server (`server.jul`)

```jul
procfun echoHandler(req : HttpServerRequest) : HttpServerResponse { ... }

proc Server := ServerStarter || HttpServer
```

- **`echoHandler`** — procfun registered at startup via `handler = echoHandler` on `listen`; each request invokes the handler directly (no per-request session proc).
- **`ServerStarter`** — `initially` then session `listen(port, handler)` with `port = 8000` and `handler = echoHandler`.
- **`HttpServer`** — Kotlin proclib ([Standard library](../language/standard-library.md)).

Lifecycle `listen` / `close` remain **session** actions between `ServerStarter` and `HttpServer` so startup pairs correctly; request handling is procfun-based.

### Client (`client.jul`)

```jul
proc Client := ClientDispatcher || ClientLogic
```

- **`ClientDispatcher`** — `initially` reads an optional count from `args` (`parseInt` / `length` from funlib); offers `startReq` / `gotResp`; exits when all responses arrive.
- **`ClientLogic`** — constructed on `startReq(num)`; calls `httpRequest` (stdlib procfun) to POST and print the response body.

Spawning `ClientLogic` via a **named constructor** (`startReq`) after program `initially` shows the “construct upon an action” pattern from [Processes](../language/processes.md).

## How to run

```bash
./gradlew shadowJar
java -jar build/libs/julayc.jar input/echo_server/main.jul
```

This writes `EchoServer.jar` and `EchoClient.jar` in the current directory. Start the server, then the client (optional request-count argument; default is 25). The server listens on port **8000**.

```bash
java -jar EchoServer.jar          # terminal 1
java -jar EchoClient.jar 3        # terminal 2
```

**Server** sample output:

```text
Req: 'ReqNum: 1', I responded: 'ReqNum: 1 is a good point!'
Req: 'ReqNum: 2', I responded: 'ReqNum: 2 is a good point!'
Req: 'ReqNum: 3', I responded: 'ReqNum: 3 is a good point!'
```

**Client** sample output:

```text
response: ReqNum: 1 is a good point!
response: ReqNum: 2 is a good point!
response: ReqNum: 3 is a good point!
```

Request order can interleave differently across runs.

## Language features showcased

- Parallel composition and modules (`import server.Server`, …)
- Procfun handler registration (`HttpServerRequest ~> HttpServerResponse`)
- Session lifecycle sync with `HttpServer` (`listen` / `close`)
- `httpRequest` stdlib procfun for the client
- `obj` literals (HTTP request/response types)
- Funlib (`length`, `parseInt`, `println`)
- Multiple `compile` targets in one entry file

## See also

- [Composition and actions](../language/composition-and-actions.md)
- [Procfun](../language/procfun.md)
- [Standard library](../language/standard-library.md) — HttpServer and `httpRequest`
- Next: [Inc server](inc-server.md)
