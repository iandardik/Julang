# Julay documentation

Julay is a verification-aware language for building distributed systems correctly. This tree is the user-facing documentation for the language and toolchain in the Julang repository.

## For language users

1. [Getting started](getting-started.md) — build `julayc`, compile, and run
2. [Language guide](language/README.md) — concepts, syntax, and semantics
3. [Examples](examples/README.md) — walkthroughs of `echo_server`, `inc_server`, `list_server`, and `rpc_server`
4. [Bugs found with Julay](examples/bugs-found-with-julay.md) — real bugs caught by specs + TLC

## Tooling

- [Tooling](tooling.md) — `julayc`, `julayc analyze`, `--json`, module search path
- [VS Code / Cursor extension](../vscode-julay/README.md) — highlighting, alphabet panel, go-to-definition

## For contributors

- Build and test notes are in the [root README](../README.md)
- End-to-end harness: [regression/README.md](../regression/README.md)
