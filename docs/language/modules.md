# Modules

## Imports

```jul
import printer.Printer
import julay.proclib.HttpServer
import julay.funlib.parseInt
```

- User modules: `import path.to.Name` resolves to a `.jul` file on the search path (e.g. `printer.Printer` → `printer.jul` exporting `Printer`).
- Stdlib procs: `julay.proclib.*`
- Expression builtins: `julay.funlib.*`

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
| Julay proclib | `julay.proclib.Println`, `Timer`, … | Shipped as `.jul` inside the compiler jar |
| Kotlin proclib | `julay.proclib.HttpServer`, `HttpClient` | Native libraries + request/response `obj` types |
| Funlib | `julay.funlib.length` | Expression builtins |

## See also

- [Standard library](standard-library.md)
- Negative cases: [`regression/input/imports/`](../../regression/input/imports/)
