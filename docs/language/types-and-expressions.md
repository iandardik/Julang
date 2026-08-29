# Types and expressions

## Base types

`Boolean`, `Int`, `Real`, `String`, with literals `true` / `false`, integers, reals, and `"strings"`.

## Collections

`List<T>`, `Map<K, V>`, and `Set<T>` — literals, operators, indexing, updates, methods, and lambdas: [Collections](collections.md).

`length` from `julay.funlib` works on lists, sets, and maps (see [Standard library](standard-library.md)).

## User-defined types (`type`)

Julay has one keyword, `type`, with three forms:

| Form | Syntax | Meaning |
|------|--------|---------|
| **Record** | `type Pair { x : Int, y : Int }` | Structural product type (old `obj`). TLA+ record; not a `CONSTANT`. |
| **Typedef** | `type Value := String` | Named domain with a known carrier. **JAR-legal** — erases to `String` / `Int` / `Boolean` in procs. Spec TLA+ uses a separate `CONSTANT Value`. |
| **Uninterpreted** | `type NodeSet` | Opaque named domain (old `sort`). **Spec-only** — a JAR compile that reaches one is an error. Spec TLA+ uses `CONSTANT NodeSet`. |

There is **no enum form**. `type Name := { "n1", "n2" }` is a parse error; `:=` after `type` is only for typedef (`type Name := typeExpr`). Finite sets for uninterpreted and typedef names come from a **delayed model** (below).

### Records

```jul
type ReqInfo {
    req : String
    resp : String
}

info := ReqInfo {
    req := req.body,
    resp := req.body + " is a good point!"
}
```

Field access uses `.` (e.g. `info.req`). Polymorphic records and functions are supported (`type Box<T> { ... }`, `fun f<T>(...)`).

Record fields may use uninterpreted types (and nest records/collections that mention them). Those records are fine in **specs / TLA+**, including leaf-spec state. They must **not** appear in any **JAR** compile target that reaches an uninterpreted type: `compile` of such a proc is an error (no `.jar`). Typedef fields are JAR-legal (they erase to the carrier).

### Typedefs and uninterpreted types

```jul
type NodeSet                    // uninterpreted — opaque domain for specs
type Value := String            // typedef — carrier is String; erases in procs
```

**Uninterpreted** types may appear in leaf specs, composition specs, invariants, `init:`, quantifiers, index binders, and record fields used only from specs. They must not appear in ordinary **proc** / **procfun** state, action args, or return types.

**Typedefs** behave like their carrier in proc bodies (`value : Value` is a `String` at runtime) but stay a distinct `CONSTANT` in TLA+ (not interchangeable with `String` in spec typing).

In **spec/TLA expressions** (invariants, `init:`), `NodeSet.length` and `length(NodeSet)` are the domain size (`Cardinality(NodeSet)` in TLA+). They are not legal in ordinary proc bodies.

**JAR refusal:** `compile` of a **proc** (and `julayc check` for those targets) errors if the assembly can reach an **uninterpreted** type, including nested in a record or collection. Spec `compile` targets are unaffected. A delayed model does **not** make an uninterpreted type JAR-legal.

### Delayed models

A **delayed model** assigns a finite literal set to a typedef or uninterpreted name. It may appear **only** inside a **create-index block** or a **leaf-spec body** — never as a top-level declaration (and never `export`ed). Export the type (`export type NodeSet`); pin the model on the spec you compile.

```jul
type NodeSet
spec ClusterSpec := RaftProtocol[n : NodeSet] {
    NodeSet := { "n1", "n2", "n3" }   // create-index
}

spec Net[n : NodeSet] {
    NodeSet := { "n1", "n2", "n3" }   // leaf-spec body
    var lastDest : String := ""
    ...
}
```

Models are collected from the **spec you `compile`** (create-index items and leaf-spec bodies in that system, including aliased specs). Two disagreeing models for one name → error.

The same create-index / leaf-spec blocks may also contain **fun overrides** `name := expr` for nullary user funs (TLA body only; see [Specifications — Fun overrides](specifications.md#fun-overrides)).

**Rules (any violation is a compile error):**

#### 1. Uninterpreted — delayed model **required**

When an uninterpreted type is used by the spec compile target, it **must** have a delayed model. Missing, duplicate, or disagreeing models are errors.

```jul
type NodeSet
spec ClusterSpec := RaftProtocol[n : NodeSet] {
    NodeSet := { "n1", "n2", "n3" }   // required
}
compile ClusterSpec
// .tla: CONSTANT NodeSet, …
// .cfg: CONSTANT NodeSet = {"n1", "n2", "n3"}
```

```jul
type NodeSet
spec S := P[n : NodeSet] { }          // error: NodeSet has no delayed model
compile S
```

#### 2. Record — delayed model **forbidden**

Record types are not `CONSTANT`s. Assigning a model to a record name is always an error.

```jul
type Pair { x : Int, y : Int }
spec S := P[n : Int] {
    Pair := { 1, 2 }                  // error: cannot assign a model to a record type
}
```

#### 3. Typedef — delayed model **optional**

If present, literals must match the carrier (`String`, non-negative `Int`, or `Boolean`). If absent, the `.cfg` binds the `CONSTANT` to the **erasure type** (`String`, `Int`, or `BOOLEAN`), so TLC uses the same universe as that builtin.

**No delayed model** — cfg aliases the carrier:

```jul
type Value := String
spec S := P { var x : Value }
compile S
// .tla: CONSTANT Value, String, …
// .cfg: CONSTANT Value = String
//       CONSTANT String = { … inferred string model … }
```

**With delayed model** — cfg pins an exact subset (must match carrier):

```jul
type Value := String
spec S := P[n : NodeSet] {
    NodeSet := { "n1", "n2" }
    Value := { "", "v1", "v2" }       // optional pin; literals must be Strings
}
// .cfg: CONSTANT Value = {"", "v1", "v2"}
```

**Carrier mismatch** — error:

```jul
type Value := String
spec S := P[n : Int] {
    Value := { 1, 2 }                 // error: carrier is String, not Int
}
```

Without a pin, `Value` and `String` share TLC’s string universe (including any role strings in the inferred `String` model). Specs that need a dedicated client-value alphabet should pin `Value := { "", "v1", "v2" }` (see Raft in [Specifications](specifications.md)).

Allowed model elements: homogeneous `String`, non-negative `Int`, or `Boolean` literals. Export the type with `export type …` and import by name like other decls (`import path.NodeSet`).

See [Specifications — delayed models](specifications.md#delayed-models) for create-index placement and TLA+ details.

## Expressions

- Arithmetic and comparisons: `+`, `-`, `*`, `/`, `%`, `<`, `<=`, `>`, `>=`, `=`, `~=`
- Logic: `&`, `|`, `~`, `=>`, `<=>`
- `if` / `else`, `let`, `when` (subject and guard forms)
- Membership: `in`, `~in` (not in)
- Quantifiers (especially in invariants): `forall`, `exists`
- In proc action bodies: `this.x` for state when an arg shadows the same name ([Processes](processes.md#this-vs-action-args))

Example (braces optional; useful for multi-line arms):

```jul
targetNumReqs := if (length(args) > 0) {
    parseInt(args[1])
} else {
    25
}
```

One-line form:

```jul
n := if (ready) 1 else 0
```

Brace-free `if`/`let` bind looser than `+` and most other operators, so parenthesize (or use braces) when an `if` sits inside a larger expression:

```jul
// wrong: else absorbs the trailing + …
s := "a=" + if (ok) "t" else "f" + "|b=" + x
// right:
s := "a=" + (if (ok) "t" else "f") + "|b=" + x
```

### Expression `let`

Syntax: `let (name : Type := init) body`, or `let (name : Type := init) { body }` when you want braces (e.g. multi-line body).

- `init` is typed in the outer environment (the bound name is not in scope there, unless it shadows an outer symbol of the same name).
- `body` is typed with `name` in scope; the whole `let` has the type of `body`.
- Allowed anywhere an expression is allowed: guards, transit RHS, returns, nested expressions.

```jul
x := let (inc : Int := 2) inc + 3
```

To share one binding across **multiple** transit assignments, use a [transit statement `let`](effects.md#transit-statement-let) instead.

## User functions

```jul
fun add(x : Int, y : Int) : Int {
    x + y
}
```

Recursive user functions are rejected. Prefer `julay.funlib.*` for common helpers.

Collection methods (`.filter`, `.map`, `.fold`, `.toSet`, `.toList`), properties (`.keys`, `.length`), and inline lambdas: [Collections](collections.md).

## See also

- [Reference](reference.md)
- [Collections](collections.md)
- [Standard library](standard-library.md) — funlib catalog including `map`
- Regression coverage under [`regression/input/list/`](../../regression/input/list/), [`map/`](../../regression/input/map/), [`set/`](../../regression/input/set/), [`record` fixtures](../../regression/input/oclass/), [`expr/`](../../regression/input/expr/)
