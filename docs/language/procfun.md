# Procfuns (removed)

Procfuns, apis, expression-style calls, and `return: e` have been removed. Use [parent/child inference](children.md) instead: a dynamic process with a constructor (request) and a `return` action (reply), nested under its parent with `||`.

HTTP handlers use `serveHttp(port, Class.ctor ~> Class.action)` — see [Parent/child inference — HTTP handlers](children.md#http-handlers).
