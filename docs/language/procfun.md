# Procfuns (removed)

Procfuns, apis, expression-style calls, and `return: e` have been removed. Use [parent-bound procs](children.md) instead: a dynamic process with a constructor (request) and a `return` action (reply), bound to its parent as `B<A>` under `||`.

HTTP handlers use `serveHttp(port, Class.ctor ~> Class.action)` — see [Parent-bound procs — HTTP handlers](children.md#http-handlers).
