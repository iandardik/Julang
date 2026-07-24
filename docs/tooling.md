# Tooling

## `julayc`

Build:

```bash
./gradlew shadowJar
# → build/libs/julayc.jar
```

Compile a Julay file (reads top-level `compile` targets, unless overridden with `--compile`):

```bash
java -jar build/libs/julayc.jar path/to/file.jul
java -jar build/libs/julayc.jar --compile EchoServer --compile EchoClient path/to/file.jul
java -jar build/libs/julayc.jar --compile-tla IncServer path/to/file.jul
```

### Useful flags

| Flag | Meaning |
|------|---------|
| `-L <dir>` | Add a module search directory |
| `--compile NAME` | Compile proc/spec `NAME` (repeatable; ignores source `compile` directives) |
| `--compile-tla NAME` | Emit TLA+ for proc `NAME` as `<true> NAME <true>` (repeatable; no extra invariants) |
| `--keep-build` | Keep generated `<name>-jul-build` dirs after success |
| `--allow-unindexed-spec` | Warn instead of error when a multi-instance proc appears unindexed in a spec |

### Environment

| Variable | Meaning |
|----------|---------|
| `JULAY_PATH` | Colon-separated extra module search dirs (after entry dir, `-L`, and embedded stdlib) |

## `julayc analyze`

Inspect composition and actions **without** codegen:

```bash
java -jar build/libs/julayc.jar analyze path/to/file.jul
java -jar build/libs/julayc.jar analyze -s NodeLogic --actions path/to/file.jul
java -jar build/libs/julayc.jar analyze -s Raft -s Client --procs path/to/file.jul
```

Common options (see `--help` for the full list):

| Option | Role |
|--------|------|
| `-s` / `--scope NAME` | Restrict to a proc or spec (repeatable) |
| `--tree` | Composition tree (default-oriented view) |
| `--actions` / `--actions-detail` | List actions (optionally with modifiers) |
| `--procs` / `--procs-detail` | List procs |
| `--intersect` / `--mutual` | How multiple scopes combine for action views |
| `--include-internal` | Include `internal` and composition-hidden synced actions; with `--actions-detail`, each occurrence and hidden `scope=` channel key is listed separately |
| `-L` | Module search path (same as compile) |

## Testing

```bash
./gradlew test
```

End-to-end cases: [regression/README.md](../regression/README.md).

## See also

- [Getting started](getting-started.md)
- [Modules](language/modules.md)
- [Specifications](language/specifications.md)
