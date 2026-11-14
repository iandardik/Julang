# Creating libraries

Julay libraries are reusable procs and functions that programs `import`. Prefer **`.jul` modules** under the embedded stdlib or found via `JULAY_PATH` / `-L`. Effectful OS/network behavior that used to be Kotlin-native proclib (`HttpServer`, `Timer`) is now **builtins** (`serveHttp`, `setTimer` / `cancelTimer`) implemented in the Go runtime — see [Standard library](standard-library.md) and [Side effects](effects.md).

## `.jul` libraries

### Layout

Stdlib Julay modules live under `src/main/resources/stdlib/` with a package path matching the import, e.g.:

- Import: `julay.funlib.max` (from `math.jul`)
- File: `stdlib/julay/funlib/math.jul`

Project-local libraries can sit on `JULAY_PATH` or `-L` the same way as any other module ([Modules](modules.md)).

### What to export

Write normal `proc` / `static proc` / `api` / `fun` / `spec` declarations. Use `export` when the module should expose a stable public surface (see [Modules](modules.md)). Prefer an **api** when callables (procfuns) need resident peers — see [Composition and actions — APIs](composition-and-actions.md#apis). Specs are useful for libraries that ship TLC checks.

### Funlib-style functions

Pure or effectful functions in `.jul` are declared with `export fun` under `stdlib/julay/funlib/` and imported as `julay.funlib.<funName>`. A module may export several funs (e.g. [`math.jul`](../../src/main/resources/stdlib/julay/funlib/math.jul) exports `max` / `min`); callers still import each by function name. Effectful functions must follow the same `before` / `transit` / `after` rules as the rest of the language ([Side effects](effects.md)).

Builtins such as `serveHttp` / `setTimer` / `cancelTimer` are registered in the compiler (`FunBuiltinRegistry`) and imported as `julay.funlib.<name>` the same way; they are not ordinary `.jul` funs.

### Optional

[`optional.jul`](../../src/main/resources/stdlib/julay/optional.jul) is a multi-export stdlib module: import `julay.optional.Optional`, `julay.optional.some`, and `julay.optional.none` (same name-per-import rule as funlib). See [Standard library — Optional](standard-library.md#optional).

## Go runtime helpers

Native binaries embed Go sources from `src/main/resources/goruntime/julay/` (`runtime.go`, `http.go`, `timer.go`, collections, …). Extend those packages when a new builtin needs runtime support; wire the name through `FunBuiltinRegistry` and Go codegen emit.

## Choosing a style

| Need | Prefer |
|------|--------|
| Protocol logic, specs/TLC | `.jul` |
| HTTP serve / timers | Builtins (`serveHttp`, `setTimer` / `cancelTimer`) |
| Pure helpers | `.jul` funlib or `export fun` |

## See also

- [Standard library](standard-library.md)
- [Modules](modules.md)
- [Side effects](effects.md)
