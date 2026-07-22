# Specifications

Julay is verification-aware: the same program text can declare **invariants** and **specs** that `julayc` compiles to TLA+ for TLC.

## Invariants

```jul
invariant NonNegative := Counter.counter >= 0
invariant AllInvs := NonNegative & CorrectCounter
```

Invariants are Boolean expressions over proc state. Quantifiers (`all`, `exists`) are common.

## Specs

Assume-guarantee form:

```jul
spec SafeInc := <Env> Counter <Bound>
```

- Left of the system (`<Env>`): assumption / environment
- Middle: system expression (procs, indexing, `||`)
- Right (`<Bound>`): guarantee invariant

Plain system form (no assume/guarantee brackets) is also allowed when you only want to emit the system.

### Indexed procs

Multiple instances of a class in a spec use indexing:

```jul
spec HandlerSpec := IncReqHandler[t : Int]
spec IncSpec := <true> Counter || HandlerSpec <AllInvs>
```

See [`input/inc_server/main.jul`](../../input/inc_server/main.jul) and [`regression/input/spec/`](../../regression/input/spec/).

## Compiling specs

```jul
compile IncServer, HandlerSpec, IncSpec
```

- `IncServer` (a proc) → JAR
- `HandlerSpec` / `IncSpec` (specs) → `HandlerSpec.tla` / `.cfg`, `IncSpec.tla` / `.cfg`

Primary how-to: [Getting started](../getting-started.md). CLI flag `--allow-unindexed-spec` softens indexing errors—see [Tooling](../tooling.md).

## Running TLC

Generated `.tla` / `.cfg` files are checked with TLC (the test suite downloads a pinned `tla2tools` jar). From the directory where files were emitted:

```bash
java -cp /path/to/tla2tools.jar tlc2.TLC SpecName
```

Exact classpath and options depend on your TLC install; project smoke tests live in `src/test/kotlin/julay/spec/`.

## What to expect

- Specs model the transition system and synchronization structure relevant to the written invariants.
- Session protocols are encoded with session-related predicates and integrity checks in the generated TLA+ (affinity / teardown behavior). Prefer learning sessions from [Sessions](sessions.md) and reading generated output for details.
- Effects such as real-time `delaySeconds` are not a faithful continuous-time model in TLC; treat timing as approximate or structure specs around discrete control state.

## See also

- [Inc server example](../examples/inc-server.md) — JAR + specs in one file
- [`regression/input/spec/safe-inc.jul`](../../regression/input/spec/safe-inc.jul) — small AG example
