# Types and expressions

## Base types

`Boolean`, `Int`, `Real`, `String`, with literals `true` / `false`, integers, reals, and `"strings"`.

## Collections

`List<T>`, `Map<K, V>`, and `Set<T>` — literals, operators, indexing, updates, methods, and lambdas: [Collections](collections.md).

`length` from `julay.funlib` works on lists, sets, and maps (see [Standard library](standard-library.md)).

## Objects (`obj`)

```jul
obj ReqInfo {
    req : String
    resp : String
}

info := ReqInfo {
    req := req.body,
    resp := req.body + " is a good point!"
}
```

Field access uses `.` (e.g. `info.req`). Polymorphic objects and functions are supported (`obj Box<T>`, `fun f<T>(...)`).

Fields may use `sort` types (and nest objs/collections that mention sorts). Those objs are fine in **specs / TLA+**, including leaf-spec state. They must **not** appear in any **JAR** compile target: `compile` of a proc that reaches a sort-bearing type is an error (no `.jar`).

## Finite sorts (`sort`)

```jul
sort Node := {"n1", "n2", "n3"}
```

Declares a finite homogeneous domain for **spec index, quantifier, and leaf-spec parameter binders**, and for **leaf-spec state** (and `obj` fields used from specs). Ordinary **proc** state and action args still cannot be sorts directly. Allowed element types: `String`, non-negative `Int`, and `Boolean`. Compiling a spec that uses the sort emits `CONSTANT Node` in TLA+ and assigns the exact set in the `.cfg`. Mark with `export` and import by name like other decls (`import path.Node`).

**JAR refusal:** if a JAR compile target’s leaf procs mention a sort-bearing type (a sort, or an `obj`/collection that nests one), compile and `julayc check` report an error. Specs are unaffected.
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

Collection methods (`.filter`, `.map`, `.fold`), properties (`.keys`, `.length`), and inline lambdas: [Collections](collections.md).

## See also

- [Reference](reference.md)
- [Collections](collections.md)
- [Standard library](standard-library.md) — funlib catalog including `map`
- Regression coverage under [`regression/input/list/`](../../regression/input/list/), [`map/`](../../regression/input/map/), [`set/`](../../regression/input/set/), [`obj` fixtures](../../regression/input/oclass/), [`expr/`](../../regression/input/expr/)
