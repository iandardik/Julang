# Specifications

Julay is verification-aware: the same program text can declare **invariants** and **specs** that `julayc` compiles to TLA+ for TLC.

## Invariants

```jul
invariant NonNegative := Counter.counter >= 0
invariant AllInvs := NonNegative & CorrectCounter
```

Invariants are Boolean expressions over proc state. Quantifiers (`all`, `exists`) are common.

## Specs

Three surface forms (same TLA pipeline):

```jul
spec Plain := Counter                      // no assume, no guarantee  (== <true> Counter <true>)
spec WithInv := Counter |= Bound           // no assume; guarantee Bound (== <true> Counter <Bound>)
spec SafeInc := <Env> Counter <Bound>       // full assume-guarantee
```

- Left of the system (`<Env>`): assumption / environment (`true` means none)
- Middle: system expression (procs, indexing, `||`)
- Right / `|=` side: guarantee — a named invariant, an inline Boolean formula, or `true` (no guarantee)

Plain `spec Name := System` is also what `--compile-tla Name` synthesizes for a proc.

### Indexed procs

Multiple instances of a class in a spec use indexing:

```jul
spec HandlerSpec := IncReqHandler[t : Int]
spec IncSpec := <true> Counter || HandlerSpec <AllInvs>
```

Finite domains can be declared with `sort` and used as the index type (and in quantifiers). TLC gets an exact `CONSTANT` assignment in the `.cfg`:

```jul
sort Node := {"n1", "n2", "n3"}
spec S := Counter[n : Node]
```

Elements must be homogeneous String, non-negative Int, or Boolean literals. Sorts are not allowed in proc bodies (state / args / `obj` fields).

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

## Sessions in TLA+

When a spec includes **two-sided** session actions (both peer classes appear as SpecLeaves), `julayc` encodes affinity and teardown in the generated module. One-sided specs (only one peer in the composition) omit the affinity helpers.

**Always emitted for two-sided pairs:**

- `session_A_B` — affinity flag (Init `FALSE`)
- `CanStartSession_A_B` — whether a new session may start (exclusivity)
- Sticky enablement on transition-only session actions: `(session \/ CanStart)` under `\* Session connection semantics`
- `*_dead` / `EndSession_*` — clear affinity when a peer’s actions are naturally disabled (or the peer was killed, when applicable)

**Per SpecLeaf (any spec):**

- `*_constructed` — always emitted; constructors require `~constructed`, transitions require `constructed`

**Omitted when unused:**

- `*_killed` — only for leaves that are a `killSessionPeer(Peer)` peer target somewhere in the module. Those leaves also get `~killed` enablement gates, and their `*_dead` includes a kill disjunct. Specs with no `killSessionPeer` omit all `*_killed` variables.
- `sessionException` and `SessionIntegrity == ~sessionException` (checked in the `.cfg`) — only when a **session constructor** action exists (models rebind `JulayException`). Transition-only or exit-only session specs omit them. A stdlib Timer bug caught by `SessionIntegrity` is written up under [Bugs found with Julay](../examples/bugs-found-with-julay.md).

**Effect mapping:**

| Effect | TLA+ |
|--------|------|
| `exitSession(Peer)` | `IF anyLive THEN session /\ session_*' = FALSE ELSE UNCHANGED` |
| `killSessionPeer(Peer)` | `IF anyLive THEN session /\ clear and peer `*_killed' = TRUE ELSE UNCHANGED` |

The peer leaf is taken from the effect argument (caller ↔ named peer among SpecLeaves).

Examples under [`regression/input/spec/`](../../regression/input/spec/): `session-pair.jul` (affinity only), `session-exit.jul`, `session-kill.jul`, `session-spawn-rebind.jul` (session ctor + `SessionIntegrity`).

## What else to expect

- Specs model the transition system and synchronization structure relevant to the written invariants.
- Effects such as real-time `delaySeconds` are not a faithful continuous-time model in TLC; treat timing as approximate or structure specs around discrete control state.

## See also

- [Inc server example](../examples/inc-server.md) — JAR + specs in one file
- [Bugs found with Julay](../examples/bugs-found-with-julay.md) — Timer `SessionIntegrity` case study
- [`regression/input/spec/safe-inc.jul`](../../regression/input/spec/safe-inc.jul) — small AG example
