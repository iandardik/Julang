# Julang

**Julay is a verification-aware language for building distributed systems correctly.**

This repository hosts the Julay language, its compiler (`julayc`), and examples. From the same Julay source you can emit a runnable program (JAR) and, where you write a `spec`, a TLA+ model for checking with TLC.

## What is Julay?

A Julay program is a **sequence of actions**. Processes (**procs**) engage with actions by **constructing** or **transitioning** on them. Every program begins with the `initially` action; after that, execution continues through user-defined actions.

Peers coordinate by **synchronizing on shared actions**—not by sharing memory. Processes do not share resources; a process may act as an **interface** to a resource, and values exchanged in a sync are always **by copy**, never by reference. See the [language guide](docs/language/README.md) for the full model.

## Status

Julay is a research / prototype language. Core concurrency, types, HTTP libraries, and TLA+ emission are in active use; sessions and the Timer stdlib are evolving areas.

## Requirements

- A JDK suitable for the Gradle Kotlin JVM build in this repo
- [Gradle Wrapper](gradlew) (included)
- [Z3](https://github.com/Z3Prover/z3) for the Julay runtime (used when running compiled programs)
- TLC / `tla2tools` for checking generated specs (pulled in by tests)

## Quick start

Build the compiler:

```bash
./gradlew shadowJar
```

Compile a small example:

```bash
java -jar build/libs/julayc.jar regression/input/basic/test1.jul
```

Then run the generated JAR for the `compile` target (here `TermTest1`).

For a fuller walkthrough, see [Getting started](docs/getting-started.md).

## Documentation

- **[Documentation index](docs/README.md)**
- **[Language guide](docs/language/README.md)**
- **[Examples](docs/examples/README.md)** — echo, inc, and list servers
- **[Tooling](docs/tooling.md)** — `julayc` and `julayc analyze`
- **[VS Code extension](vscode-julay/README.md)** — highlighting, alphabet panel, go-to-def
- **[Regression tests](regression/README.md)** — end-to-end harness for contributors

## Repository layout

| Path | Role |
|------|------|
| `src/` | Compiler, runtime, and embedded stdlib |
| `input/` | Larger demos (echo / inc / list servers, Raft, …) |
| `vscode-julay/` | VS Code / Cursor extension |
| `regression/` | End-to-end test cases and harness |
| `docs/` | Language guide and example walkthroughs |

## License

This project is licensed under the [MIT License](LICENSE).

## Author

Ian Dardik is the main author — [https://iandardik.github.io/](https://iandardik.github.io/).
