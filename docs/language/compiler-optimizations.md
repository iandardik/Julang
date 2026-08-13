# Compiler optimizations

Julay may **optimize action synchronization** at runtime so many common guards avoid a full Z3 solve. Optimizations are **transparent**: they do not change observational semantics, and application `.jul` sources (including Raft) do **not** need to be rewritten. Recompile with a newer `julayc` to pick them up.

Kotlin `Channel` rewrites for clear send/receive are **not** implemented yet.

## Named optimizations

| ID | Role |
|----|------|
| `eq-unify` | When guards are conjunctions of equalities, unify substitutions instead of calling the solver |
| `arg-ownership` | Arguments constrained on only one peer are bound from that peer’s equalities |
| `directed-eval` | At commit, evaluate functionally determined args (e.g. `resp = req + 1`) without reading a Z3 model |

**Residual Z3** is always available: unsupported shapes (relational guards, mixed eq + inequality on one action, string/collection embeddings, complex terms the fast path cannot evaluate) use today’s solver path. Local **enablement** checks also stay on Z3 for safety. Residual Z3 is not a disableable “optimization.”

Default: all three named optimizations are **on**.

## `--disable-opt`

On `julayc` compile:

```bash
# Disable every named sync optimization (classic Z3-only sync)
java -jar build/libs/julayc.jar --disable-opt path/to/file.jul

# Disable only some (use '=' so the source path is not consumed as the option value)
java -jar build/libs/julayc.jar --disable-opt=eq-unify path/to/file.jul
java -jar build/libs/julayc.jar --disable-opt=eq-unify,directed-eval path/to/file.jul
```

The chosen config is **baked into** the generated program JAR (`Program(..., SyncResolveConfig(...))`). There is no runtime env var; recompile with or without the flag to change behavior.

Unknown optimization ids are a usage error.

## `--verbose` sync-path summary

`julayc --verbose` prints a **compile-time** summary of generated Julay procs: which emit `SyncStepPlan.FastOnly` vs `NeedsZ3` (TS-level: every transition guard must lower to `BoolExprFast`), and separately how many actions get a residual `fastGuard` vs remain opaque. Kotlin library `TransitionSystem`s are not tallied. When all named opts are off, the banner notes that runtime still always uses NeedsZ3 even if codegen classified a proc as FastOnly.

## Correctness

Fast paths apply to **pairwise compatibility** and **commit-time argument binding**. Local action **enablement** still uses Z3 so mixed embeddings (string concat, collections) cannot falsely skip steps.

See also [Composition and actions](composition-and-actions.md) and [Tooling](../tooling.md).

## TLA+ emission optimizations

When compiling a `spec` (or `--compile-tla`), Julay may **project unread `obj` fields** out of generated TLA+ records and TLC argument domains. That shrinks `\E` binders (e.g. dropping `Node.url` when the spec only reads `.id`). Executable JAR codegen is unchanged.

This is a **projection**: values that differed only in omitted fields become equal, so `=` / `in` / `~=` on those records can diverge from the Julay program. If a field is omitted from a type that still appears in whole-record comparison or set containment, `julayc` warns and points at `--disable-tla-opt=unused-fields`.

Default: all named TLA+ optimizations are **on**.

### Named TLA+ optimizations

| ID | Role |
|----|------|
| `unused-fields` | Omit obj fields that the TLA-relevant fragment never projects (field access / struct patterns / comparison-operand literals) |

### `--disable-tla-opt`

Separate from `--disable-opt` (JAR sync). Does not affect generated program JARs.

```bash
# Disable every named TLA+ optimization
java -jar build/libs/julayc.jar --disable-tla-opt --compile RaftNodeSpec path/to/file.jul

# Disable only unused-field projection (use '=' so the source path is not consumed)
java -jar build/libs/julayc.jar --disable-tla-opt=unused-fields path/to/file.jul
```

Unknown TLA+ optimization ids are a usage error. `--disable-opt` and `--disable-tla-opt` may be passed together; they do not interact.
