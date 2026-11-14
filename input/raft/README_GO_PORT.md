# Raft and the Go-backed language

Raft under `input/raft/` still uses the removed `provider` / `client` / `session`
keywords and the old `Timer` / `HttpServer` proclib APIs.

It is **out of scope** for the initial Go runtime port and needs a redesign:

- static hubs instead of provider/client
- `serveHttp` instead of `HttpServer` listen/close sessions
- `timer transition` + `setTimer` / `cancelTimer` instead of `julay.proclib.Timer`
