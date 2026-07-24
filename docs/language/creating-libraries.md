# Creating libraries

Julay libraries are reusable procs and functions that programs `import`. There are two implementation styles:

1. **`.jul` modules** — ordinary Julay source, often shipped under the embedded stdlib or found via `JULAY_PATH` / `-L`
2. **Kotlin-native** — `TransitionSystem` implementations registered in `LibraryRegistry`

Callers of a library should not need to know which style it uses. Authors must, especially for Kotlin-native channel keys under occurrence-based `||`.

## `.jul` libraries

### Layout

Stdlib Julay modules live under `src/main/resources/stdlib/` with a package path matching the import, e.g.:

- Import: `julay.proclib.Timer`
- File: `stdlib/julay/proclib/Timer.jul`

Project-local libraries can sit on `JULAY_PATH` or `-L` the same way as any other module ([Modules](modules.md)).

### What to export

Write normal `proc` / `fun` / `spec` declarations. Use `exports` when the module should expose a stable public surface (see [Modules](modules.md)). Specs are useful for libraries that ship TLC checks (Timer’s `TimerSpec` is an example).

### Funlib-style functions

Pure or effectful functions in `.jul` are declared with `fun` and imported as `julay.funlib.<name>` when placed under that package path. Effectful functions must follow the same `before` / `transit` / `after` rules as the rest of the language ([Side effects](effects.md)).

### Example pattern (proclib)

See [`Timer.jul`](../../src/main/resources/stdlib/julay/proclib/Timer.jul): a `proc` with session constructors/transitions, funlib effects (`delaySeconds`, `exitSession`, …), and an optional `spec` for model checking.

## Kotlin-native libraries

Use Kotlin when the implementation needs JDK APIs (HTTP, files outside funlib, etc.). Current examples: `HttpServer`, `HttpClient` in `julay.program.library`.

### Checklist

1. **Implement `TransitionSystem`** (and usually a `companion object : JulLibrary`).
2. **Declare `SymbolicAction`s** for the alphabet and constructors.
3. **Implement `staticInfo(): TransitionSystemStaticInfo`** with that alphabet and constructor map.
4. **Register** the library in `LibraryRegistry` under its Julay import name (`julName`).
5. **Provide `actionDecls`** so the compiler can type-check and compose.
6. **Compare actions by name** in `transit` / `actions` (not full `SymbolicAction` equality): composition may assign per-occurrence `channelKey`s that differ from the companion defaults.
7. **Use `StaticInfo` / host occurrence keys at runtime** — do not assume companion singletons are what `Program` registered. `Proc.resolveSymbolicAction` remaps offers onto the bound StaticInfo. Nested spawns (e.g. per-request resources) should reuse `hostProc.occurrenceStaticInfo()` so they share composition-assigned channel keys.
8. **Support `withChannelKeys`** — codegen emits `Lib.staticInfo().withChannelKeys(mapOf(...))` for each leaf occurrence when keys differ from the public action name.

### Channel keys and duplicate occurrences

Parallel composition is occurrence-based. Composition-hidden syncs use private scoped `channelKey`s on **both** peers, including Kotlin-native library occurrences. Codegen emits one `TransitionSystemStaticInfo` per leaf (via `withChannelKeys` when keys differ from the public action name). Library `transit` / `actions` must use the bound StaticInfo alphabet (or remap companion offers with `Proc.resolveSymbolicAction`). Nested spawns should reuse `hostProc.occurrenceStaticInfo()` so they share the host occurrence’s keys.

### Wiring

- `JulLibrary` / `LibraryRegistry` — discovery and codegen expr (`JulHttpServer.staticInfo()`)
- Builtin obj types for HTTP requests/responses live beside the library
- Document the import and effects in [Standard library](standard-library.md)

## Choosing a style

| Need | Prefer |
|------|--------|
| Protocol logic, sessions, specs/TLC | `.jul` |
| JDK / OS APIs not in funlib | Kotlin-native |
| Both (Timer + delaySeconds) | `.jul` calling funlib effects |

## See also

- [Standard library](standard-library.md)
- [Composition and actions](composition-and-actions.md) — occurrence-based `||` and channel keys
- [Modules](modules.md)
- [Side effects](effects.md)
