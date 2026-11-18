# Julay language guide

**Julay is a verification-aware language for building distributed systems correctly.**

From the same source you can build an executable system (native Go binary) and, where you write a `spec`, emit TLA+ for model checking with TLC.

## Execution model

A Julay **program is a sequence of actions**.

- Procs may **construct** upon an action or **transition** upon an action (subject to synchronization with peers; transitions may also use guards—constructors cannot).
- Every program **begins with the `initially` action**. After that, execution continues through **user-defined** actions—transitions and any further constructors used when spawning dynamic procs.
- Use **`static proc`** for hubs (at most one occurrence; `initially` only) and ordinary **`proc`** for dynamic peers.

## Mental model

- A **proc** is a transition system with local state (`var` / `const`).
- Parallel composition (`||`) lists the members of a system and scopes ordinary syncs.
- Peers coordinate by **synchronizing on a shared action** — `service`/`client` on a public hub channel, or ordinary peers that become composition-private under `||`. When they sync, arguments are exchanged **by copy**, never by reference. Parallel composition itself is **occurrence-based** (`A || A` is two occurrences); see [Composition and actions](composition-and-actions.md).

```mermaid
sequenceDiagram
  participant A as Proc A
  participant Foo as action foo
  participant B as Proc B
  A->>Foo: offer
  B->>Foo: offer
  A<<->>B: synchronized step
```

## Philosophy

- Processes **do not share resources**.
- A process may act as an **interface** to a resource: other procs synchronize with it to use that resource.
- Synced objects (action arguments) are always **by copy, not by reference**.

Two different instances of the **same proc class never synchronize** with each other on ordinary or `internal` actions. Only distinct classes can pair. Details: [Composition and actions](composition-and-actions.md).

## `compile`

Top-level `compile Name, ...` selects what `julayc` emits:

- **Proc** names → native binaries (error if the assembly reaches an uninterpreted type)
- **Spec** names (composition or leaf) → `.tla` / `.cfg` (uninterpreted types and typedefs allowed)

How-to: [Getting started](../getting-started.md). Specs: [Specifications](specifications.md).

## Chapters

1. [Processes](processes.md) — state, `initially`, `static proc` vs dynamic, constructors vs transitions, returns
2. [Composition and actions](composition-and-actions.md) — `||`, **service/client**, ordinary composition-private syncs, action shapes, fairness
3. [Parent/child inference](children.md) — nested `||`, returns, HTTP `~>`
4. [Compiler optimizations](compiler-optimizations.md) — compile-time direction analysis; TLA+ `--disable-tla-opt` / `--opt-tla`
5. [Types and expressions](types-and-expressions.md)
6. [Collections](collections.md) — `List` / `Map` / `Set`, methods, lambdas
7. [Modules](modules.md) — `import` and search path
8. [Side effects](effects.md) — `serveHttp` / timers / funlib; `before` / `after` / transit IO
9. [Standard library](standard-library.md) — builtins and funlib (including which have effects)
10. [Creating libraries](creating-libraries.md) — authoring `.jul` libs
11. [Specifications](specifications.md) — invariants, specs, TLC
12. [Reference](reference.md) — keywords, operators, builtins

## Examples

Worked demos: [Examples](../examples/README.md).
