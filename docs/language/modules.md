# Modules

## Imports and exports

Top-level declarations are **file-private by default**. Mark names that other files may import with `export`:

```jul
export proc Server { ... }
export api RpcOut {
    proc: RpcOutClient || HttpClient
    calls: rpcOutClientCaller
}
export fun add(x : Int, y : Int) : Int = x + y
export type NodeSet
export spec Net[n : NodeSet] { ... }   // leaf or composition specs; pin NodeSet := { … } in the body / create-index
```

```jul
import printer.Printer
import julay.proclib.HttpServer
import julay.funlib.parseInt
import node.protocol.NodeSet
```

- User modules: `import path.to.Name` resolves to a `.jul` file on the search path (e.g. `printer.Printer` → `printer.jul` exporting `Printer`).
- Stdlib procs: `julay.proclib.*`
- Funlib (pure helpers and effectful functions): `julay.funlib.*` — see [Standard library](standard-library.md)
- **Types** (including uninterpreted domains) may be `export`ed and imported like other decls; pin delayed models on the compiling spec, not on the export (see [Specifications](specifications.md#delayed-models)).

Same-file references never need `export`. Cross-file use without `export` on the defining declaration is a compile error.

Importing an **api** (`import path.Api`) is enough to compose it and to call every procfun listed in its `calls:` as `Api.fn(...)`. Those listed procfuns need not be exported; exporting the api is the public surface. Standalone procfuns (not reached via an api) still require their own `export` to be imported by name.

## Search order

When compiling `file.jul`, modules are searched in this order:

1. Directory of the entry file
2. `-L` directories (CLI)
3. Embedded stdlib in `julayc.jar`
4. `JULAY_PATH` (colon-separated directories)

Details: [Getting started](../getting-started.md), [Tooling](../tooling.md).

## What belongs where

| Kind | Example | Notes |
|------|---------|--------|
| Your modules | `server.ServerLogic` | Next to the entry file or on `-L` / `JULAY_PATH` |
| Apis | `node.rpc_out.RpcOut` | Composition units: resident `proc:` + `calls:` procfuns; see [Composition and actions](composition-and-actions.md#apis) |
| Proclib | `julay.proclib.Timer`, `HttpServer`, … | Process APIs; some are Kotlin-native under the hood |
| Funlib | `julay.funlib.length`, `julay.funlib.println`, … | Functions; effectful ones require import like the rest |

## See also

- [Standard library](standard-library.md)
- Negative cases: [`regression/input/imports/`](../../regression/input/imports/)
