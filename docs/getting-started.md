# Getting started

This guide builds the Julay compiler (`julayc`), compiles a small program, and runs it. For concepts, see the [language guide](language/README.md).

## Build the compiler

From the repository root:

```bash
./gradlew shadowJar
```

This produces `build/libs/julayc.jar`.

## Your first program

A Julay file declares processes and a top-level `compile` target. Every runnable program begins with the `initially` action.

A minimal example lives at [`regression/input/basic/test1.jul`](../regression/input/basic/test1.jul). It composes two procs (`S` and `T`) that synchronize on `increment`, then prints and exits:

```jul
proc TermTest1 := S || T

compile TermTest1
```

Compile it:

```bash
java -jar build/libs/julayc.jar regression/input/basic/test1.jul
```

`julayc` emits a JAR named after each **proc** listed in `compile` (here `TermTest1`). Run that JAR from the directory where it was written (usually the current working directory).

## What `compile` does

The top-level declaration:

```jul
compile Name1, Name2, ...
```

selects build targets:

| Target kind | Output |
|-------------|--------|
| A **proc** (including a parallel composition) | Runnable JAR |
| A **spec** | `.tla` and `.cfg` for TLC |

You can mix both in one file. See [Specifications](language/specifications.md) for the verification path, and [Tooling](tooling.md) for CLI flags.

Override source targets from the command line with `--compile` (repeatable); when any `--compile` is given, source `compile` directives are ignored:

```bash
java -jar build/libs/julayc.jar --compile TermTest1 regression/input/basic/test1.jul
```

## Module search path

When resolving `import`, `julayc` searches, in order:

1. The directory of the entry `.jul` file
2. Directories passed with `-L`
3. The embedded stdlib inside `julayc.jar`
4. Directories in the `JULAY_PATH` environment variable (colon-separated)

Example:

```bash
java -jar build/libs/julayc.jar -L input/echo_server input/echo_server/main.jul
```

(Often the entry file’s directory is enough, as with the servers under [`input/`](../input/).)

## Next steps

- [Language guide](language/README.md)
- [Examples](examples/README.md) — echo, inc, and list HTTP servers
- [Tooling](tooling.md) — `julayc analyze` and more flags
