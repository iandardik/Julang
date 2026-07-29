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
java -jar build/libs/julayc.jar analyze -s TermTest1 --json regression/input/basic/test1.jul
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
| `--json` | Emit machine-readable alphabet JSON for the scope: `compositionGraph` (top-level `||` children, called / api-listed procfuns, sync edges with action labels, and unlabeled call edges), `external`, `sourceInternal`, and `compositionHidden` sync groups. For a **procfun** scope, `external` lists user actions (synthetics omitted). Parent scopes include called procfuns' non-synthetic actions. **Api** scopes analyze like proc assemblies (including `calls:`). Used by the VS Code extension; suppresses human-readable views |
| `-L` | Module search path (same as compile) |

## `julayc check`

Type-check without codegen (same cost profile as `analyze`). Used by the VS Code extension for on-save Problems/squiggles:

```bash
java -jar build/libs/julayc.jar check path/to/file.jul
java -jar build/libs/julayc.jar check --json path/to/file.jul
```

| Option | Role |
|--------|------|
| `--json` | Emit machine-readable diagnostics JSON (`severity`, `message`, `file`, `startLine`, `endLine`, optional `related`) |
| `-L` | Module search path (same as compile) |
| `--allow-unindexed-spec` | Warn instead of error for unindexed multi-instance procs in specs |

Exit code **1** if any error diagnostics; **0** if clean or warnings-only (including soft sync-peer warnings).

## VS Code / Cursor extension

The [`vscode-julay/`](../vscode-julay/) extension adds `.jul` highlighting, snippets, compile tasks, go-to-definition, hover alphabets, a panel with a top-level composition sync diagram plus external / source-internal / composition-hidden actions, and **on-save diagnostics** (Problems panel + squiggles via `julayc check --json`). See [vscode-julay/README.md](../vscode-julay/README.md) for install and settings.

## Testing

```bash
./gradlew test
```

End-to-end cases: [regression/README.md](../regression/README.md).

### `GradleWorkerMain` / ClassNotFoundException

If `./gradlew test` fails with `Could not find or load main class worker.org.gradle.process.internal.worker.GradleWorkerMain`, the Gradle test-worker cache is broken. Cursor often sets `GRADLE_USER_HOME` to a sandbox under `/var/folders/.../cursor-sandbox-cache/`, which is where this shows up.

Repair (forces `~/.gradle`):

```bash
./scripts/fix-gradle-worker.sh
export GRADLE_USER_HOME="$HOME/.gradle"
./gradlew test
```

Or manually:

```bash
export GRADLE_USER_HOME="$HOME/.gradle"
./gradlew --stop
rm -rf ~/.gradle/caches/8.5/workerMain
./gradlew test --tests julay.compiler.ParseTreeSmokeTest --rerun-tasks
```

## See also

- [Getting started](getting-started.md)
- [Modules](language/modules.md)
- [Specifications](language/specifications.md)
- [VS Code extension](../vscode-julay/README.md)