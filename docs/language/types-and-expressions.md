# Types and expressions

## Base types

`Boolean`, `Int`, `Real`, `String`, with literals `true` / `false`, integers, reals, and `"strings"`.

## Collections

| Type | Literals / notes |
|------|------------------|
| `List<T>` | `[]`, `[a, b]`, concatenation with `+`, indexing |
| `Map<K, V>` | `[k -> v, ...]`; update with `m[k] := v` in `transit` |
| `Set<T>` | `{...}`; set operations as supported by the language |

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

## Finite sorts (`sort`)

```jul
sort Node := {"n1", "n2", "n3"}
```

Declares a finite homogeneous domain for **spec index and quantifier binders only** (not proc state, action args, or `obj` fields). Allowed element types: `String`, non-negative `Int`, and `Boolean`. Compiling a spec that uses the sort emits `CONSTANT Node` in TLA+ and assigns the exact set in the `.cfg`.

## Expressions

- Arithmetic and comparisons: `+`, `-`, `*`, `/`, `%`, `<`, `<=`, `>`, `>=`, `=`, `~=`
- Logic: `&`, `|`, `~`, `=>`, `<=>`
- `if` / `else`, `let`, `when` (subject and guard forms)
- Membership: `in`
- Quantifiers (especially in invariants): `all`, `exists`

Example:

```jul
targetNumReqs := if (length(args) > 0) {
    parseInt(args[0])
} else {
    25
}
```

### Expression `let`

Syntax: `let (name : Type := init) { body }`.

- `init` is typed in the outer environment (the bound name is not in scope there, unless it shadows an outer symbol of the same name).
- `body` is typed with `name` in scope; the whole `let` has the type of `body`.
- Allowed anywhere an expression is allowed: guards, transit RHS, returns, nested expressions.

```jul
x := let (inc : Int := 2) { inc + 3 }
```

To share one binding across **multiple** transit assignments, use a [transit statement `let`](effects.md#transit-statement-let) instead.

## User functions

```jul
fun add(x : Int, y : Int) : Int {
    x + y
}
```

Recursive user functions are rejected. Prefer `julay.funlib.*` for common helpers.

Higher-order `map` (from `julay.funlib.map`) takes a collection and a **named** unary `fun` (not a lambda):

```jul
fun double(n : Int) : Int = n + n
ys := map([1, 2, 3], double)   // List<Int>
ts := map({1, 2}, double)      // Set<Int>
```

## See also

- [Reference](reference.md)
- [Standard library](standard-library.md) — funlib catalog including `map`
- Regression coverage under [`regression/input/list/`](../../regression/input/list/), [`map/`](../../regression/input/map/), [`set/`](../../regression/input/set/), [`oclass/`](../../regression/input/oclass/), [`expr/`](../../regression/input/expr/)
