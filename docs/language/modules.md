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
```

```jul
import printer.Printer
import julay.proclib.HttpServer
import julay.funlib.parseInt
```

- User modules: `import path.to.Name` resolves to a `.jul` file on the search path (e.g. `printer.Printer` → `printer.jul` exporting `Printer`).
- Stdlib procs: `julay.proclib.*`
- Funlib (pure helpers and effectful functions): `julay.funlib.*` — see [Standard library](standard-library.md)

Same-file references never need `export`. Cross-file use without `export` on the defining declaration is a compile error.

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
