# Specifications

Julay is verification-aware: the same program text can declare **invariants** and **specs** that `julayc` compiles to TLA+ for TLC.

## Invariants

```jul
invariant NonNegative := Counter.counter >= 0
invariant AllInvs := NonNegative & CorrectCounter
invariant Bound := forall k : Int, Counter.n <= 3
```

Invariants are Boolean expressions over proc state and/or **fluents**. Quantifiers (`forall`, `exists`) are common; multiple binders may share a type (`forall n1, n2 : NodeSet, …` ≡ nested `forall n1 : NodeSet, forall n2 : NodeSet, …`). Indexed state uses `Leaf[i].var`; under create-index plus dynamic children, dual-index `Leaf[n][i].var` is allowed (quantify `i` with `forall i : DynProc` when `type DynProc` is declared and the create-index supplies `DynProc := {…}`). Dynamic leaf field reads in comparisons desugar to require the instance is live (`Leaf_constructed[n][i] /\ …`). Collection properties and nested record fields are allowed (`Leaf[i].log.length`, `Leaf[i].log[j]`).

## Fluents

Ghost history relations for compositional assume–guarantee. They are not JAR/runtime state.

```jul
fluent Claimed(k : Key, owner : String) {
    initially false
    on applyClaim[n](claimKey, claimOwner) := true
}

fluent BadMove {
    initially false
    on putFromMove[n](moveKey, _, moveOwner) := ~Claimed(moveKey, moveOwner)
}

invariant StoreAsm := ~BadMove
spec StoreSpec := <StoreAsm> IndexedStore <StoreInvs>
```

- **`initially`**: Boolean formula over fluent params and non-fluent state/domains. It **may not** mention fluents.
- **`on Act(…)` / `on Act[n](…)`**: update when that alphabet action fires. Binders are action arg names; `_` ignores. Use `[n]` iff the action is create-indexed in the AG system (mismatch is a compile error). An `on` action absent from this AG system's alphabet is a no-op (frame) so parallel Obls can share one fluent formula.
- **Targets (v1):** `true`, `false`, `Fluent(args)`, `~Fluent(args)`.
- Fluents appear only in invariants / other fluent targets — not in proc bodies.
- Formula assume (`<StoreAsm> Sys <G>`): fluents become TLA `VARIABLES`; actions update them; a post-state operator `StoreAsmOK` (fluents primed) is conjoined on steps (not a cfg `INVARIANT`). Guarantee `Sys |= StoreAsm` still lists `StoreAsm` as `INVARIANT`.

## Specs

Two kinds of `spec` declaration:

1. **Composition specs** — assume–guarantee (or plain) systems over procs / apis / leaf specs
2. **Leaf specs** — proc-class-shaped bodies that exist only for TLA+ (never JAR)

### Composition specs

Three surface forms (same TLA pipeline):

```jul
spec Plain := Counter                      // no assume, no guarantee  (== <true> Counter <true>)
spec WithInv := Counter |= Bound           // no assume; guarantee Bound (== <true> Counter <Bound>)
spec SafeInc := <Env> Counter <Bound>       // full assume-guarantee
spec UnderAsm := <StoreAsm> Store <Inv>  // formula assume (named invariant / fluents)
spec Obl := LocalInv |= <Asm> Sys <G>    // local inductive Inv; contract A/G stay on the AG
```

- Left of the system (`<Env>` / `<StoreAsm>`): assumption — `true`, a **system**, or a **named invariant** (formula assume)
- Middle: system expression (procs, **apis**, **leaf specs**, indexing, `||`)
- Right / trailing `|=` on a bare system: guarantee — a named invariant, an inline Boolean formula, or `true` (no guarantee)
- **`LocalInv |= <A> C <G>`**: [Local inductive invariant](#local-inductive-invariant-localinv--a-c-g) — `LocalInv` is checked; A/G are the AG contract names

Invariant assume/guarantee formulas are type-checked against the AG **system** leaves (missing leaf state or wrong indexing is a compile error). Formula assumes do not compose env leaves into the TLC model.

#### Explicit AG Obl `.cfg`: safety by default (no `*Terminates`)

An **explicit** assume–guarantee composition written with angle brackets (`<Asm> Sys <G>`, circular `<P, A> C <G>` / `<G> C <A, Safe>`, or `LocalInv |= <A> C <G>`) is an obligation (Obl) whose default TLC campaign is **safety only**. Trailing `Sys |= G` and plain systems are not treated that way: they keep fairness and `PROPERTY *Terminates` in the `.cfg` (including when the Spec lists `unsynced` open-env actions).

For every **explicit** AG Obl, `julayc` therefore emits:

| Artifact | Behavior |
|----------|----------|
| `.tla` | Still defines `Fairness`, `Spec == Init /\ [][Next]_vars /\ Fairness`, `SpecSafety == Init /\ [][Next]_vars`, and each dynamic leaf’s `<Class>Terminates`. |
| `.cfg` | `SPECIFICATION SpecSafety` (no fairness). Lists guarantee `INVARIANT`s. **Does not** list `PROPERTY *Terminates`. |
| `.apalache.cfg` | Only with `--opt-tla=apalache`: INIT/NEXT safety cfg (plus Snowcat `@type` in the `.tla`). Off by default. |

`Sys |= G`, plain systems, and leaf specs keep `SPECIFICATION Spec` and `PROPERTY <Class>Terminates` for each dynamic leaf.

#### Circular AG (comma slots)

Pair two obligations so each assumes what the other guarantees:

```jul
export spec Obl1 := I1 |= <true, A> C1 <G>       // LocalInv I1; assume P∧A, guarantee G
export spec Obl2 := I2 |= <G>      C2 <A, Safe> // LocalInv I2; assume G, guarantee A∧Safe
```

- Comma (not `&`) marks circular slots. Ordinary `<Asm> Sys <G>` is unchanged.
- Optional `LocalInv |=` prefixes the AG (same as ordinary Obls); TLC/Apalache check `LocalInv`.
- **A** and **G** must be **fluent** invariants (no leaf state in the formula closure).
- **P** / **Safe** may be fluent or ordinary state invariants (`true` for P via `<true, A>`).
- Specs are paired when Obl1/Obl2 share the same A and G names.

Rule (informal): if `I₁ |= ⟨P, A⟩ C₁ ⟨G⟩` and `I₂ |= ⟨G⟩ C₂ ⟨A, Safe⟩`, then under side conditions `I₁ ∧ I₂ |= ⟨P⟩ C₁∥C₂ ⟨Safe⟩`.

#### Local inductive invariant (`LocalInv |= <A> C <G>`)

Separate the **contract** (`A`, `C`, `G` / circular commas) from the **inductive strengthening** checked by TLC/Apalache:

```jul
/* LocalInv ⇒ G (or ⇒ A ∧ Safe on the circular right). Conjuncts strengthen G. */
invariant LocalInv := Inv1 & Inv2
invariant Inv1 := ...
invariant Inv2 := ...

export spec Obl := LocalInv |= <Asm> Sys <G>
export spec CircL := LocalInvL |= <true, A> C1 <G>
export spec CircR := LocalInvR |= <G> C2 <A, Safe>
```

- Without `LocalInv |=`, the Obl checks the guarantee slot as before (`G`, or `A`/`Safe` on the circular right).
- With `LocalInv |=`, the cfg lists `LocalInv` as `INVARIANT`; CircSide still pairs on the A/G **names** in the AG.

When both premises are compiled, `julayc` also emits **`CircSide.tla`** for the product `C₁ ∥ C₂`:

| Check | Encoding | Cfg |
|-------|----------|-----|
| SC1: `Init ∧ P ⇒ A ∧ G` | `SpecSC1` (Init + stuttering) + `INVARIANT SC1` | `CircSide.cfg` |
| SC2: `Next ⇒ A′ ∨ G′` | `PROPERTY SC2Prop == [][Next => SC2]_vars` | `CircSide_SC2.cfg` |

Side conditions are TLC modules, not unchecked `ASSUME`. See [`regression/input/spec/circular-ag.jul`](../../regression/input/spec/circular-ag.jul) and sharded KV `ShardEnvObl` / `StoreObl`.

Plain `spec Name := System` is also what `--compile-tla Name` synthesizes for a proc or api. Apis work like procs in all three forms; procfuns listed in an api's `calls:` are coupled in TLA+ (see [Composition and actions — APIs](composition-and-actions.md#apis)).

### Leaf specs

A leaf spec looks like a proc class but is **verification-only**: `compile` emits `.tla` / `.cfg`, never a JAR. Use them for environments and other stubs that should not be executable. Lifecycle matches procs: **`static spec`** has exactly one `constructor initially` and no `return`; plain **`spec`** (dynamic) has no `initially`, must have a spawning constructor and at least one `return`.

```jul
static spec Env {
    var ready : Boolean := false
    constructor initially(args : List<String>) { transit: ready := false }
    transition mark() { transit: ready := true }
}

spec Sys := Env
compile Env, Sys
```

Optional **declaration parameters** bind an immutable name usable in guards and transit. They do **not** lift leaf state to functions of that index (unlike create-index on a composition leaf):

```jul
type Node
static spec Net[n : Node] {
    Node := { "n1", "n2" }
    var lastDest : String := ""
    constructor initially(args : List<String>) {}
    transition send() { transit: lastDest := n }   // n is an aux binder; lastDest stays scalar
}
spec Ag := <Net> Peer <true>
```

When the parameter type is an **uninterpreted** type, the body treats `n` as that type’s element view (e.g. `String` for a string-backed model); the TLA domain remains the uninterpreted name (`\in Node`). Assigning to a leaf-spec parameter is an error. If the action body mentions the decl param, TLA emits it as a leading auxiliary action parameter (`\E n \in Node: …`), except under `with (n : …)` where it shares that binder instead of a second `\E`.

Leaf-spec actions may also declare explicit auxiliaries with `also (…)` (leaf specs only — illegal on ordinary `proc` / `procfun`):

```jul
transition observe(target : String) also (m : Node) {
    guard: Peer[m].self = target
    transit: lastSeen := n
}
```

TLA action parameter order: **decl param (if used), then `also` args, then used sync args**.

**Cross-leaf state reads** in leaf-spec bodies (same shape as invariants): `Peer.var` or `Peer[idx].var`. Read-only; not allowed in ordinary proc bodies. Compiling a system that uses such a read requires that peer to appear in the expanded system with matching indexing (`Peer[idx].var` ⇒ create-indexed peer; `Peer.var` ⇒ unindexed).

**Not allowed in proc assemblies.** Leaf specs may appear in composition specs, but not under `proc Name := …` or an api’s `proc:`:

```jul
static spec A { constructor initially(args : List<String>) {} }
static proc Peer { constructor initially(args : List<String>) {} }

spec C := A            // ok
proc B := A            // error
proc E := A || Peer    // error
```

Fixtures: [`regression/input/spec/leaf-plain-env.jul`](../../regression/input/spec/leaf-plain-env.jul), [`leaf-param-net.jul`](../../regression/input/spec/leaf-param-net.jul), [`also-peer-with.jul`](../../regression/input/spec/also-peer-with.jul).

### Indexes: create, `with`, and apply

Three roles:

| Role | Syntax | Meaning |
|------|--------|---------|
| **Create index** | `Name[v : Type]` | Lift `Name`’s state to functions of a new index |
| **Global vars** | `Name[v : T] { global Leaf.x, Leaf.y }` | Leave listed vars unindexed (one TLA VARIABLE for all `v`) |
| **Init constraints** | `Name[v : T] { global Leaf.x; init: expr }` | Extra Init conjuncts over frozen globals / uninterpreted `.length` |
| **Delayed model** | `Name[v : T] { NodeSet := { … } }` | Finite set for an uninterpreted or typedef name (see below) |
| **Shared binder** | `with (v : Type) { system }` | Scope where `v` may be applied; one `\E v` for the group |
| **Apply index** | `Name[v]` | Use binder `v` from an enclosing `with` (no type). Does not create an index |

Hard rule: inside `with`, **create-index `Name[n : T]` is illegal** — create outside, then apply. Apply-index outside `with` is illegal. Apply-index does not take a `{ global … }` block (globality is a property of lifting).

```jul
spec PeerIndexed := Peer[n : NodeSet]          // create (lift state)

spec ClusterSpec := RaftProtocol[n : NodeSet] {
  global RaftProtocol.cluster
  NodeSet := { "n1", "n2", "n3" }
  init: cluster.length = NodeSet.length
  init: forall i : Int, (i >= 1 & i <= cluster.length) =>
      (exists node : Node, node in cluster & node.id = i)
  init: forall n1 : NodeSet, forall n2 : NodeSet,
      (RaftProtocol[n1].self = RaftProtocol[n2].self) => (n1 = n2)
}

spec Sys := with (n : NodeSet) {
    PeerIndexed[n] || Net[n]                   // apply only
}
```

**`global` is model state only.** TLA+ emits listed variables as scalars (not functions of the index): TypeOK / reads omit `[n]`, and `Peer[i].cluster` in an invariant becomes the unindexed `cluster` when that name is unique across leaves (a true two-leaf clash is `{var}_{leaf}`, e.g. `cluster_RaftProtocol`). JAR codegen is unchanged — each runtime instance still has its own copy; this is not shared memory.

Names must be **proc-qualified** (`Leaf.var`): bare `cluster` is a parse error. The leaf must be an indexed leaf of that create-index; only that leaf’s copy is marked global.

When a listed `global` name is a proc **`const`**, TLA also **freezes** it (Init `\in` domain / singleton, no prime, UNCHANGED). When it is a **`var`**, it stays a mutable scalar. Prefer putting truly shared immutable topology **outside** the create-index with composition (`(Store || ShardLogic)[n] || ClusterInfo`) so the leaf is never lifted.

Unknown leaves/vars, duplicates, and synthetic bookkeeping vars (`constructed` / `killed` / `terminated`) are compile errors. On shorthand `(Store || ShardLogic)[n : T] { global ClusterInfo.cluster }`, only the `ClusterInfo` leaf is marked (not every child that happens to declare `cluster`).

**TLA+ for frozen globals (`global` + proc `const`):**

- **Init** uses `/\ cluster \in <enumerable TLC domain>` (e.g. `BoundedSeq(Int, MaxListLen)` for `List<Node>`) instead of `= default`. TypeOK still uses `Seq(...)` so membership stays pointwise.
- Constructors never prime the variable (`UNCHANGED` includes it).
- A write whose RHS is an action-arg symbol is elided: that arg is dropped from the action/`\E`, and remaining references use the TLA state name (unique `cluster` stays `cluster`). So `startRaftCore(n, me, cluster)` becomes `startRaftCore(n, me)` and `me \in Range(cluster)` stays `me \in Range(cluster)`. If an action/index parameter would shadow a VARIABLE, the **parameter** is renamed (`cluster` → `cluster_`, then `cluster_2`, …), not the state var.
- Any other write is an unprimed equality check: `/\ cluster = <rhs> \* global const check` (IO havoc: `v \in domain`).

Plain `global extra` (a `var`) is unchanged: Init default, primed writes, scalar (unindexed).

**`init:` on create-index** (same `{ }` block) adds extra Init conjuncts. Use this for facts about frozen global state that never change, so Init is enough (not a checked invariant):

```jul
spec ClusterSpec := RaftProtocol[n : NodeSet] {
    global RaftProtocol.cluster
    init: cluster.length = NodeSet.length
    init: forall i : Int, (i >= 1 & i <= cluster.length) =>
        (exists node : Node, node in cluster & node.id = i)
    init: forall n1 : NodeSet, forall n2 : NodeSet,
        (RaftProtocol[n1].self = RaftProtocol[n2].self) => (n1 = n2)
}
```

The same `{ models; init: …; global … }` items may trail **any** composition (`spec A := B { … }`, `spec A := B || C { … }`), not only create-index. Prefer shared immutable topology as an **unindexed** sibling leaf:

```jul
spec Cluster := (Store || ShardLogic)[n : Node] || ClusterInfo {
    Node := { "n1", "n2" }
    init: ClusterInfo.cluster.length = Node.length
    init: forall i : Int, (i >= 1 & i <= ClusterInfo.cluster.length) =>
        ClusterInfo.cluster[i].id = i
    init: forall n : Node, n.url = n
    init: ClusterInfo.me = ClusterInfo.cluster[1]
}
```

Several `init:` lines, or `init: A & B`, all become Init `/\`. On a **composition** trailing block, unindexed proc `const`s must be written `Leaf.var` (`ClusterInfo.cluster`, `ClusterInfo.me`). Bare names are only for explicitly listed create-index `global Leaf.x` bindings (then `cluster` means that frozen global). An uninterpreted type identifier is a value of that type (TLA `CONSTANT`). Indexed `RaftProtocol[n].cluster` is an error (frozen globals are scalars). Indexed **`const`** state that is not a listed global is allowed (`RaftProtocol[n1].self`), so Init can constrain per-index constants. Mutable `var`s are not. Uninterpreted `.length` / `length(NodeSet)` is spec/TLA-only (`Cardinality` in TLA+). If `|NodeSet| > MaxListLen` (default 3), compile errors — Init would be empty.

For **opaque record index sorts**, projection VARIABLES default to membership in Init (`Node_url \in [Node -> String]`, `Node_id \in [Node -> Int]`). Pin them with `init:` when you want a singleton: `forall n : Node, n.url = n` emits `Node_url = [n ∈ Node |-> n]`; length plus `ClusterInfo.cluster[i].id = i` emits a concrete `cluster` sequence and pins `Node_id`.

### Delayed models

Delayed models assign finite literal sets to **uninterpreted** and **typedef** names for TLC. They are written `Name := { lit, … }` and may appear in a create-index block (alongside `global` / `init:`), a **composition trailing block** (`spec A := B || C { … }`, same items), or a **leaf-spec body** — not as top-level decls.

**Compile-time rules** (see [Types and expressions — delayed models](types-and-expressions.md#delayed-models) for full examples):

| Type kind | Delayed model | `.cfg` when absent | `.cfg` when present |
|-----------|---------------|--------------------|---------------------|
| Uninterpreted | **Required** if used by the compile target | — (error) | `CONSTANT Name = {…}` |
| Typedef | **Optional** | `CONSTANT Name = Carrier` (alias to erasure) | `CONSTANT Name = {…}` (must match carrier) |
| Record | **Allowed when used as a create-index sort** (opaque atoms + field projections) | — (error if used as index without model) | `CONSTANT Name = {…}` |

Record index sorts keep full structs in JAR; TLA+ encodes values as CONSTANT atoms with projection VARIABLES (`Node_url`, `Node_id`). Empty / sentinel literals (e.g. `emptyNode()` with `url := ""`) emit a distinguished atom **outside** the create-index CONSTANT (prefer `""`, else `#` / `__none` / … if that atom is already in the live model). Top-level Node-typed state uses `(Node \cup {sent})` in TypeOK; nested fields, list/map elements, and action `\E` binders stay on live `Node`. Projections pin `Node_url[sent] = sent` and `Node_id[sent] = -1`. Create-index and `\A n \in Node` stay on live atoms only so `peer = emptyNode` never skips a real replica. Unused-fields stripping/warnings do not apply to opaque index sorts.


**Uninterpreted on create-index** (required when the indexed type is used):

```jul
type NodeSet
spec ClusterSpec := RaftProtocol[n : NodeSet] {
    NodeSet := { "n1", "n2", "n3" }
    init: forall n1 : NodeSet, forall n2 : NodeSet,
        (RaftProtocol[n1].self = RaftProtocol[n2].self) => (n1 = n2)
}
compile ClusterSpec
// .cfg: CONSTANT NodeSet = {"n1", "n2", "n3"}
```

**Typedef with optional pin** (Raft client values):

```jul
export type Value := String

spec ClusterSpec := RaftProtocol[n : NodeSet] {
    NodeSet := { "n1", "n2", "n3" }
    Value := { "", "v1", "v2" }    // pin client log alphabet; omit → Value = String in .cfg
}
// .cfg: CONSTANT Value = {"", "v1", "v2"}
```

**Errors:**

```jul
type NodeSet
spec S := P[n : NodeSet] { }       // error: no delayed model for NodeSet

type Pair { x : Int, y : Int }
spec Bad := P[n : Int] {
    Pair := { 1, 2 }               // error: record cannot have a delayed model
}

type Value := String
spec Bad := P[n : Int] {
    Value := { 1, 2 }              // error: literals must match String carrier
}
```

Models are merged from the compile target’s system AST (create-index items and leaf-spec bodies, including aliased specs). Two different sets for the same name in one compile → error.

### Fun overrides

Spec-only overrides rewrite a **nullary** user `fun`’s TLA operator body (and cfg literal collection). JAR codegen keeps the original definition. Syntax is bare `name := expr` in a create-index block, composition trailing block, or leaf-spec body (same places as delayed models).

```jul
export fun maxBatchEntries() : Int = 32   // runtime / JAR

spec ClusterSpec := RaftProtocol[n : NodeSet] {
    NodeSet := { "n1", "n2", "n3" }
    maxBatchEntries := 2                  // TLA: maxBatchEntries == 2; shrinks cfg Int
}
```

**Rules:** name must be a user `fun` in the compilation unit; must be nullary; override expr must match the return type; conflicting overrides for the same name are errors. Executable JARs are unchanged.

**Shorthand** `(A || B)[n : T]` means the same as create-temps + `with` + applies:

```jul
// Shorthand
spec Result := (RaftProtocol || OtherProc)[n : NodeSet]

// Means the same as
spec RaftProtocolTemp := RaftProtocol[x : NodeSet]
spec OtherProcTemp := OtherProc[x : NodeSet]
spec Result := with (n : NodeSet) {
    RaftProtocolTemp[n] || OtherProcTemp[n]
}
```

Temps / binder `x` may be compiler-internal when using the shorthand. Leaves under one `with` share one TLA binder string.

### Indexed procs

Multiple instances of a class in a spec use **create-index**:

```jul
spec HandlerSpec := incHandler[t : Int]
spec IncSpec := <true> Counter || HandlerSpec <AllInvs>
```

Finite domains are declared with **uninterpreted** `type` and a **delayed model** on the create-index (or leaf-spec body), then used as the index type (and in quantifiers):

```jul
type Node
spec S := Counter[n : Node] {
    Node := { "n1", "n2", "n3" }
}
```

**`ProcfunInst`** is a reserved create-index model (not a domain type). When present, api `calls:` procfuns under that index become dual-indexed `[n][i]` with `i ∈ ProcfunInst` — a **reusable in-flight concurrency bound** (not a lifetime spawn cap). Runtime still spawns per call; see [Procfuns — TLA+](procfun.md#tla--verification).

```jul
spec NodeIndexed := (Store || Shard)[n : NodeSet] {
    NodeSet := { "n1", "n2", "n3" }
    ProcfunInst := { "i1", "i2" }
}
```

Elements must be homogeneous String, non-negative Int, or Boolean literals. Uninterpreted types are illegal as ordinary **proc** state / action args. They **are** allowed on record fields and in **leaf-spec state** (and leaf-spec parameters as domain binders). A JAR `compile` target that reaches an uninterpreted type (including via nested records) is refused — see [Types and expressions](types-and-expressions.md#typedefs-and-uninterpreted-types). Fixtures: [`regression/input/spec/obj-sort-field.jul`](../../regression/input/spec/obj-sort-field.jul). A type may be `export`ed and `import`ed from another module (same rules as other decls); see [Modules](modules.md).

See [`input/inc_server/main.jul`](../../input/inc_server/main.jul) and [`regression/input/spec/`](../../regression/input/spec/).

## TLA+ translation notes

TLA+ emit may rewrite the composed spec (JAR codegen is unchanged). Named ids, all default-on; see [Compiler optimizations](compiler-optimizations.md#tla-emission-optimizations):

- `unused-fields` — project unread record fields out of TLA records and TLC domains
- `unused-vars` — omit state vars/consts the TLA-relevant fragment never reads
- `determined-args` — substitute args fixed by `arg = expr` / `<=>` with `LET` instead of `\E`. Also binds an arg determined on every arm of a mutually exclusive `|` tree (`LET` of `IF`/`CASE`)
- `from-collection` — quantify remaining args from a state set/list (or a struct literal `in` a set), or from `S.filter(x -> x.f = a).length > 0` (`\E a \in { x.f : x \in S }`)
- `literal-domains` — per-site finite `{…}` for String/Int that only use a closed literal set (including TypeOK ranges and const-global Init `\in` domains)
- `unwrap-singletons` — emit a one-field record as that field’s type
- `unused-lets` — drop expression and transit `LET`s that the `IN` body never reads (recursively)

Disable with `--disable-tla-opt` / `--disable-tla-opt=ID,...`.

Open `Int` / `String` sites that still need a TLC universe (leaf index, remaining `\E`, havoc) get a `.cfg` assignment from literals in the emitted spec — not a fixed `{0..3}` or a 9-string set. Cfg `Int` is the contiguous range `{0, …, max(highest non-negative literal, MaxListLen)}` (default `MaxListLen` is `3`). If nothing enumerates `Int`/`String`, that CONSTANT is omitted. Always-on; not a named opt.

TLC also infers `CONSTRAINT StateConstraint` (not `INVARIANT`): each `Int` state variable inhabits cfg `Int` (union negative literals already in the model, so `votedFor = -1` stays allowed), and each list state variable has `Cardinality(DOMAIN x) <= MaxListLen`. Parameterized leaves quantify `\A n \in NodeSet : currentTerm[n] \in Int`. Successors outside those bounds are discarded. That keeps `commitIndex` inside cfg `Int`, so `\A i \in Int` is complete for `StateMachineSafety` in this model. Always-on; not a `--disable-tla-opt` id.

When `init:` uniquely determines a const-global list (length plus identity `xs[i] = i`) or set (length plus covering membership `1..n` in the set), Init emits `xs = <<1, 2, …>>` or `xs = {1, 2, …}` instead of `\in BoundedSeq` / `SUBSET` plus those filters. Opaque index-sort url identity (`forall n : Node, n.url = n`) pins `Node_url = [n ∈ Node |-> n]` instead of `\in [Node -> String]`. TypeOK may add `Len(x[n]) = Len(cluster)` for lists assigned `cluster.map(...)`, `DOMAIN x[n] = cluster` for maps assigned `cluster.associateWith(...)`, `x[n] \subseteq Range(cluster)` for id sets drawn from a sequence, and `x[n] \subseteq cluster` when `cluster` is already a set. The `.cfg` lists each user invariant once: a named operator that is only `&` of other invariants in the closure is still emitted in the `.tla` but omitted from `INVARIANT` lines. `IF P THEN FALSE ELSE TRUE` emits `~P`; `IF P THEN TRUE ELSE FALSE` emits `P`. Always-on; not named opt ids.

### Lists and sequences

- Julay lists are **1-based**, matching TLA `Sequences`. Reads and `EXCEPT` updates on list-typed state use the Julay index as-is.
- List **action-argument / havoc domains** use a finite `BoundedSeq(S, MaxListLen)` (not bare `Seq(S)`, which TLC cannot enumerate). `MaxListLen` is a module `CONSTANT` (default `3` in the `.cfg`); raise it for longer lists. Lists are modeled as `Int ->` functions (Apalache-friendly) rather than `Seq`.
- Emitted helpers sit under `\* TLA+ helpers`: `BoundedSeq(S, N) == UNION { [1..k -> S] : k \in 0..N }` when list domains appear, `Range(f) == { f[__i] : __i \in DOMAIN f }` when list `in` / `~in`, `.toSet()`, or `allDistinct` is used, and `SetToSeq` when `.toList()` is used.

### Collections emitted today

| Julay | TLA+ |
|-------|------|
| `e in xs` / `e ~in xs` on lists | `e \in Range(xs)` / `e \notin Range(xs)` (`Range` under `\* TLA+ helpers`) |
| `k in mp` / `k ~in mp` on maps | `k \in DOMAIN mp` / `k \notin DOMAIN mp` |
| `mp[k] = v` (and other comparisons) in invariants | Desugars to `k in mp & mp[k] = v` for **unguarded** reads so TLA+ only applies `mp` when `k \in DOMAIN mp`. Under `if (k in mp) { … }` / inverted `if` / `when` membership arms, membership stays paired with the read — no extra outer enabling conjunct. Bare determined `v = mp[k]` still gets an action-level `k \in DOMAIN mp`. |
| `e in s` / `e ~in s` on sets | `e \in s` / `e \notin s` |
| `listOf()` / list literals | `<<>>` / `<<…>>` |
| `setOf()` / set literals | `{}` / `{…}` |
| `mapOf()` / map literals | `[x \in {} \|-> 0]` / `[k \|-> v, …]` |
| `splice(xs, a, b)` | `splice` helper above `Init` (1-based inclusive). Default TLC: `Len`/`SubSeq`. With `--opt-tla=apalache`: finite `CASE` on slice length (no symbolic ranges). |
| `length(xs)` / `xs.length` on lists | `Len(xs)` |
| `length` / `.length` on sets | `Cardinality(…)` |
| list/set `.map` (lambda) | function/set comprehension |
| set `.associateWith` (lambda) | `[__k \in S \|-> e]` |
| map `.remove(k)` | `[__k \in DOMAIN mp \\ {k} \|-> mp[__k]]` |
| list `.filter` | `SelectSeq` |
| set `.filter` | set comprehension |
| `xs.toSet()` | `Range(xs)` |
| `s.toList()` | `SetToSeq(s)` (order unspecified). Default TLC: recursive `CHOOSE`+`\o`. With `--opt-tla=apalache`: finite `CASE` on `Cardinality`. |
| `allDistinct(xs)` | `allDistinct(xs)` helper: `Len(xs) = Cardinality(Range(xs))` |
| `startsWith(s, p)` | `startsWith` helper above `Init` (`SubSeq` prefix check) |
| `when` | TLA `CASE` … `[]` … `OTHER` |

### TLA+ translation limits

Unsupported (or only partial) constructs may still degrade to `TRUE` or unusable TLC domains — avoid them in specs you intend to model-check, or expect incomplete operators:

| Feature | Status |
|---------|--------|
| `.fold` | Not emitted |
| Map `.filter` / `.map` / `.fold` / HOF pipelines on `.keys` | Not fully emitted |
| Freestanding HOFs beyond `length` / `map` | Unsupported unless listed above |
| Bare infinite `Seq(S)` as `\E` bound | Replaced by `BoundedSeq`; do not reintroduce |
| `SUBSET Int` / other infinite set domains for `\E` | Still problematic for TLC as action-arg domains |
| Complex list updates beyond index `EXCEPT` / slices | Slices use `splice`; other bulk updates may degrade |
| Named Julay `fun`s used in Init/action bodies **or invariants** | Emitted as TLA+ operators above `Init` (under `\* Julay lib funs` / `\* user defined funs`); call sites keep the fun name |
| `split` / `parseInt` / `trim` / `portFromUrl` | Unsupported in TLA+ (compile warning; degrade to `TRUE`) |
| IO / effectful funlib in guards | Existing havoc rules |
| Procfuns not listed in an api `calls:` | Existing havoc warnings |

See also [Collections](collections.md).

### Action layout

- Multi-leaf (and solo) actions group each participant under `\* <Proc> <transition type> logic` (e.g. `transition`, `constructor`, `timer transition`), with that leaf’s enablement gates, guards, and transits together — similar to Init’s `\* State variables for <Proc>` sections.
- When an action has `error:` arms, TLA+ emits `\* <Proc> <transition type> assumption` **before** the logic section. Each arm’s condition is negated and becomes a `/\\` conjunct (a guard): top-level `~in` / `~=` / `~` flip to `\in` / `=` / the inner formula; otherwise a `~` wraps the condition. List `in` / `~in` emit as `\in Range(xs)` / `\notin Range(xs)` (sequences are functions; `Range` is a generated helper). Error messages are runtime-only (JAR still throws `JulayException`).
- Top-level `&` guard conjuncts become separate `/\\` lines (matching multi-line Julay guards). Nested multi-line `&` / `|` are formatted recursively as `/\\` and `\\/` branches (single-line boolean ops stay compact).
- Multi-line Julay `Rec { ... }` literals become TLA records with one field per line; field lines are indented 2 spaces past the first non-`/\`/`\/` symbol on the opening line (single-line record inits stay compact).
- Multi-line Julay `if` / expression `let` / `when` / list / set literals use that same open-column indent for bodies, `ELSE`/`IN`/`[]`/`OTHER`, and closing brackets (not the full hanging left-hand text such as `EXCEPT` or `\cup`).
- Transit-level `let` bindings become TLA `LET` around later assign conjuncts (not AST-inlined); consecutive lets share one `LET`. Discard `let _ := …` is omitted. Chained expression `let`s are also one `LET`.
- Julay `when` becomes TLA `CASE` with `[]` arms and `OTHER` for the trailing else.
- Invariants preserve multi-line structure (boolean trees). Consecutive `\A` / `\E` binders over the same domain are condensed like Next (`\A n1, n2 \in NodeSet`). Parentheses written in the `.jul` source are kept; other parentheses are omitted when operator precedence makes them unnecessary. A blank line separates consecutive user-specified invariant operators. List indices are 1-based: `i <= xs.length => xs[i]` is true for `i = 0` on an empty list (`0 <= 0`), and TLC then evaluates `<<>>[0]`. Guard with `i >= 1` as well.
- A leaf’s **only** constructor, when it has no sync partner (not a client/service pair, not hybrid ctor+default, not a procfun `*_call` / havoc site), is folded into `Init`: constructor transits become Init assignments (RHS that depend on TLA-unsupported builtins / IO / action args become `\in` domains), `error:` arms that constrain process state become Init constraints under `\* <Proc> constructor assumption` immediately after that leaf’s state variables, and the constructor is omitted from `Next`. Assumptions that only mention unbound constructor args (e.g. `\E args \in … : Len(args) = 2`) are dropped — they do not constrain state after folding. `initially` constructors that are **not** folded (`initially` / `*_initially`) are emitted first after `Init`, both as operator definitions and as the leading disjuncts of `Next`. Consecutive `\E` binders in `Next` over the same domain are written `\E n, m \in NodeSet`. After each leaf’s `\* State variables for …` Init header, if `unused-vars` dropped any of that leaf’s state, a `\* unused-vars: omitted …` comment lists them in declaration order.
- Two blank lines separate funs/helpers from `\* system definition`, then a blank line before `Init`. After `Spec == Init /\ [][Next]_vars`, two blank lines precede `\* Invariants`. A blank line precedes the closing `====`. Automatically generated operators (`TypeOKInt`, `TypeOK`, `StateConstraint` when Int/list state vars exist, and `SessionIntegrity` when present) sit under `\* automatically generated invariants`; named and inline guarantees sit under `\* user-specified invariants`. `TypeOK` is listed first in the `.cfg`, then `CONSTRAINT StateConstraint` when present. `TypeOKInt == Int \cup Nat \cup {lo..hi}` sits immediately above `TypeOK`. It unions cfg `Int` (a finite non-negative `\E` bound that cannot include negatives), `Nat` (so `x := x + 1` counters stay in-type when `Nat` is not overridden), and every integer from the lowest negative literal through `max(highest literal, MaxListLen+1)`. List vars use `BoundedSeq(S, MaxListLen)` (`Int ->` functions) rather than bare `Seq(S)`, so Apalache typechecking stays finite. `StateConstraint` is the finite-model bound: Int vars inhabit cfg `Int` (plus those negatives), list length is `Cardinality(DOMAIN x) <= MaxListLen`.
- `BoundedSeq`, `Range`, and `SetToSeq` are emitted under `\* TLA+ helpers` (above `\* Julay lib funs`) when used. Julay `fun`s referenced from Init/action guards, transit RHS, **or invariants** (and their transitive callees) are emitted as TLA+ operators immediately above `Init`, grouped under `\* Julay lib funs` (stdlib funlib) or `\* user defined funs`. Operator parameters that collide with `VARIABLES` / `CONSTANT`s / other module operators are renamed (`p_…`).
- `splice(xs, a, b)` becomes a call to a module-level `splice` operator (defined above `Init` under `\* Julay lib funs` when any call is used); splice params/binders are clash-renamed like fun params.
- `startsWith` and `allDistinct` become module-level helpers (same Julay-lib section) when used.

## Compiling specs

```jul
compile IncServer, HandlerSpec, IncSpec, Env
```

- `IncServer` (a proc) → JAR
- `HandlerSpec` / `IncSpec` / `Env` (composition or leaf specs) → `.tla` / `.cfg`

Primary how-to: [Getting started](../getting-started.md). CLI flag `--allow-unindexed-spec` softens indexing errors—see [Tooling](../tooling.md).

## Running TLC

Generated `.tla` / `.cfg` files are checked with TLC (the test suite downloads a pinned `tla2tools` jar). From the directory where files were emitted:

```bash
java -cp /path/to/tla2tools.jar tlc2.TLC SpecName
```

Exact classpath and options depend on your TLC install; project smoke tests live in `src/test/kotlin/julay/spec/`.

## Construction / exit bookkeeping in TLA+

Session affinity encoding (`session_*`, `CanStartSession_*`, `exitSession` / `killSessionPeer`, `SessionIntegrity`) is **removed** with the session language feature. Remaining bookkeeping:

**Per SpecLeaf:**

- `*_constructed` — constructors require `~constructed`, transitions require `constructed`. Omitted when that leaf’s sole unsynced constructor is folded into `Init`.

**Omitted when unused:**

- `*_killed` — for leaves that call `exitProc` somewhere in the module. Those leaves also get `~killed` enablement gates. Specs without `exitProc` omit `*_killed`.
- Actions whose TLA body updates no variables (guard-only / no primed updates) are omitted from action definitions and from `Next`; stuttering remains via `[][Next]_vars`.

| Effect | TLA+ |
|--------|------|
| `exitProc()` | caller `*_killed' = TRUE` (self-exit; compile error inside procfuns) |

**IO in transit (havoc):** assignments whose RHS involves `readln()` or `readFile(...)` do not emit a concrete next-state expression. The target is **havoc’d**: `stateVar' \in String` (or an `\E` form for indexed leaves). See [Before/after and IO](effects.md#tla-translation-io-havoc).

Example: [`regression/input/spec/exit-proc.jul`](../../regression/input/spec/exit-proc.jul) (`exitProc` / `*_killed`).

## What else to expect

- Specs model the transition system and synchronization structure relevant to the written invariants.
- `before:` / `after:` side effects such as real-time `delaySeconds` are not a faithful continuous-time model in TLC; treat timing as approximate or structure specs around discrete control state.

## See also

- [Inc server example](../examples/inc-server.md) — native binary + specs in one file
- [Bugs found with Julay](../examples/bugs-found-with-julay.md) — historical Timer / session case study
- [`regression/input/spec/safe-inc.jul`](../../regression/input/spec/safe-inc.jul) — small AG example
- [`regression/input/spec/circular-ag.jul`](../../regression/input/spec/circular-ag.jul) — circular comma AG + CircSide
