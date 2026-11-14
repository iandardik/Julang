# Compiler optimizations

Executable programs target **Go**: the compiler emits Go sources, runs `go build`, and produces a native binary. There is **no Z3** (or other SMT solver) on the Go run path, and no runtime sync “fast path” vs residual solver.

## Compile-time channel direction

For each ordinary or `service`/`client` sync group, `julayc` runs **compile-time direction analysis** (guard/body binding) to decide sender vs receiver, which args travel on the forward message, and which are **reply args**. Illegal or ambiguous shapes fail at compile time. Details: [Composition and actions — Sender / receiver deduction](composition-and-actions.md#sender--receiver-deduction).

`internal` and `timer` actions stay occurrence-local and do not use directed sync channels.

## Retired JAR sync opts

Former Kotlin/JAR flags `--disable-opt` / ids `eq-unify`, `arg-ownership`, `directed-eval`, `procfun-fuse` are **accepted as no-ops** for CLI compatibility. `--verbose` no longer prints a FastOnly / NeedsZ3 sync-path summary.

## TLA+ emission optimizations

When compiling a `spec` (or `--compile-tla`), Julay may rewrite generated `.tla` / `.cfg` so TLC `\E` quantification is smaller. **Go codegen is unchanged.** Default: all named TLA+ optimizations are **on**. Disable with `--disable-tla-opt` / `--disable-tla-opt=ID,...` (`=` is required when passing ids).

Some of these are **equivalent rewrites** of the same transition relation. Others are **projections**: they exclude values the Julay type would otherwise allow, so `=` / `in` / `~=` can diverge from the program.

TLC `.cfg` `Int` / `String` universes are sized from literals in the emitted spec (open `\E` / index / havoc sites). Cfg `Int` is `{0, …, max(highest non-negative literal, MaxListLen)}`. That projection is always on and is not a `--disable-tla-opt` id. `MaxListLen` stays `3`.

The compiler also infers a TLC `CONSTRAINT StateConstraint` (not an `INVARIANT`): each Julay-`Int` state variable must inhabit that cfg `Int`, union any negative literals already in the model (so `votedFor = -1` is allowed at Init), and each list state variable has `Len(x) <= MaxListLen`. Parameterized leaves use `\A n \in NodeSet : currentTerm[n] \in Int`. Successors that leave those bounds are discarded. Always-on; not a `--disable-tla-opt` id. Do not add a user `constraint` keyword.

TypeOK still uses `TypeOKInt` (`Int ∪ Nat ∪ extras`) so membership stays pointwise; CONSTRAINT is what keeps reachable Int state inside cfg `Int`. That also makes `\A i \in Int` complete for properties whose bound is an Int state var (Raft `StateMachineSafety` vs `commitIndex`).

Always-on (not named opt ids): singleton `init:` that uniquely determines a const-global list emits `xs = <<1, 2, …>>` instead of `\in BoundedSeq` plus those filters; a const-global set with length plus covering `1..n` membership emits `xs = {1, 2, …}` instead of `\in SUBSET`; TypeOK may add `Len(x[n]) = Len(cluster)` for `cluster.map` lists, `DOMAIN x[n] = cluster` for `cluster.associateWith` maps, `x[n] \subseteq Range(cluster)` for id sets from a sequence, and `x[n] \subseteq cluster` when the source is already a set; the `.cfg` omits a named invariant whose formula is only `&` of other invariants already in the closure (the TLA operator is still emitted); `IF P THEN FALSE ELSE TRUE` emits `~P` and `IF P THEN TRUE ELSE FALSE` emits `P`.

### Named TLA+ optimizations

| ID | Role | Kind |
|----|------|------|
| `unused-fields` | Omit obj fields that the TLA-relevant fragment never projects (field access / struct patterns / comparison-operand literals) | Projection |
| `unused-vars` | Omit state vars/consts the TLA-relevant fragment never reads (emitted-action guards/transits and the guarantee). Not TLA+ liveness (`WF`/`SF`). | Equivalent rewrite (of the emitted spec) |
| `determined-args` | Drop `\E` for action args fixed by `arg = expr` or `arg <=> expr`; substitute with `LET`. A `|` of `&`-branches that determines the same arg on every arm binds a `LET` of `IF`/`CASE` and skips the per-branch equalities (not the rest of the `|` tree) | Equivalent rewrite |
| `from-collection` | Quantify remaining args from a state collection: `a in S` on a set, `S[a.f] = a` on a list (index binder `i \in 1..Len(S)`), a struct literal `in` a set, or `S.filter(x -> x.f = a).length > 0` (`\E a \in { x.f : x \in S }`; a `|` of bags unions the projections) | Equivalent rewrite |
| `literal-domains` | Per-site finite `{…}` for String/Int that only use a closed literal set, including TypeOK var/field ranges and const-global Init `\in` domains. Does **not** by itself shrink the global `String` CONSTANT (so e.g. `Entry.value` stays open); the cfg String/Int models are still the residual open-site literals | Projection (when it excludes values the type would otherwise allow) |
| `unwrap-singletons` | After unused-fields, an obj with one remaining field emits as that field’s type (Raft `Node` → `Int`) | Equivalent rewrite |
| `unused-lets` | Drop expression and transit `LET` bindings that the `IN` body (and later kept inits) never read; walk inner-to-outer so dropping `voteCondition` then drops `logOk` | Equivalent rewrite |

If a field is omitted from a type that still appears in whole-record comparison or set containment, `julayc` warns and points at `--disable-tla-opt=unused-fields`. Values that differed only in omitted fields become equal.

### `--disable-tla-opt`

Does not affect generated Go binaries.

```bash
# Disable every named TLA+ optimization
java -jar build/libs/julayc.jar --disable-tla-opt --compile RaftNodeSpec path/to/file.jul

# Disable only unused-field projection (use '=' so the source path is not consumed)
java -jar build/libs/julayc.jar --disable-tla-opt=unused-fields path/to/file.jul

# Keep unread state vars in TLA (e.g. Raft knownLeader)
java -jar build/libs/julayc.jar --disable-tla-opt=unused-vars path/to/file.jul

# Disable a mix
java -jar build/libs/julayc.jar --disable-tla-opt=determined-args,from-collection path/to/file.jul
```

Unknown TLA+ optimization ids are a usage error. `--disable-opt` and `--disable-tla-opt` may be passed together; they do not interact.

See also [Composition and actions](composition-and-actions.md) and [Tooling](../tooling.md).
