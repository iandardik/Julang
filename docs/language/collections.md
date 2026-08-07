# Collections

Built-in `List`, `Map`, and `Set` values. Collection values are updated by reassignment (and, for lists and maps, by transit index updates). On action sync, arguments are passed **by copy**, never by reference.

Julay does **not** have first-class function values: lambdas exist only as arguments to higher-order collection calls, and bodies are inlined at compile time.

## Quick comparison

| | `List<T>` | `Map<K, V>` | `Set<T>` |
|--|-----------|-------------|----------|
| Literal | `[a, b]`, typed `[]` | `[k -> v, …]`, typed `[]` | `{a, b}`, typed `{}` |
| `+` | concat | — | union |
| `-` | — | — | difference |
| `in` | element | **key** | element |
| Index read | `xs[i]` (`Int`) | `m[k]` | — |
| Slice | `xs[a:b]` | — | — |
| Index assign (transit) | `xs[i] := v` | `m[k] := v` | — |
| `.length` | yes | yes | yes |
| `.keys` | — | → `Set<K>` | — |
| `.filter` / `.map` / `.fold` | yes | — (use `.keys`) | yes |

## `List<T>`

### Literals

```jul
xs := [1, 2, 3]
ys : List<Int> := []   // empty needs a known List target type
```

Elements must share one type. Untyped empty `[]` is a compile error.

### Operators and membership

- `xs + ys` — concatenation (same `List<T>`)
- `=` / `~=` — structural equality
- `e in xs` — element membership

```jul
xs := [1, 2]
xs := xs + [3]   // [1, 2, 3]
ok := 2 in xs
```

### Indexing

`xs[i]` requires an `Int` index and yields the element type. Out-of-bounds reads throw at runtime (`IndexOutOfBoundsException`). There is no compile-time bounds check.

```jul
n := xs[0]
```

### Slicing

`xs[start:end]` yields a `List<T>`. Both bounds must be `Int`.

At runtime:

- bounds must be non-negative
- `end` is clamped to `xs.length`
- if `start >= end`, the result is empty (including reversed bounds such as `xs[3:1]`)

```jul
xs := [10, 20, 30, 40]
mid := xs[1:3]       // [20, 30]
empty := xs[2:2]     // []
clamped := xs[2:99]  // [30, 40]
nested := xs[1:3][0] // 20
```

### Updates

Whole-list reassignment:

```jul
xs := [1, 2]
xs := xs + [3]
```

Index assignment in `transit` only:

```jul
xs[1] := 99
```

- Index must be `Int`; value must match `T`.
- Updates an existing slot; does **not** grow the list.
- Out-of-bounds throws at runtime (same as reads).
- Multiple `xs[i] := …` in one action apply in order.
- You cannot mix whole-list `xs := …` and `xs[i] := …` in the same action.

### Size

`xs.length` (intrinsic) or `length(xs)` from `julay.funlib.length`.

## `Map<K, V>`

### Literals

```jul
mp := ["a" -> 1, "b" -> 2]
empty : Map<String, Int> := []   // same [] token as empty list; target type decides
```

### Operators and membership

- `=` / `~=` only (no `+` / `-` on maps)
- `k in mp` tests **key** membership, not value membership

```jul
hasA := "a" in mp
missing := ~("z" in mp)
```

### Indexing

`mp[k]` requires a key of type `K` and yields `V`. A missing key throws at runtime (`Key … is missing in the map`). Prefer guarding with `k in mp` when the key may be absent.

### Updates

Whole-map reassignment:

```jul
mp := ["a" -> 1]
```

Put in `transit` (insert or overwrite):

```jul
mp["c"] := 3
```

- Multiple puts to the same map in one action compose in order.
- You cannot mix whole-map `mp := …` and `mp[k] := …` in the same action.

### Keys and size

- `mp.keys` → `Set<K>` (intrinsic)
- `mp.length` / `length(mp)` — entry count

Maps are **not** receivers for `.filter` / `.map` / `.fold`. Iterate keys instead:

```jul
agree := mp.keys.filter(k -> mp[k] >= commitIndex)
```

## `Set<T>`

### Literals

```jul
s := {1, 2}
empty : Set<Int> := {}
```

### Operators and membership

- `s + t` — union
- `s - t` — difference
- `=` / `~=`
- `e in s` — element membership

```jul
s := {1, 2}
s := s + {3}   // {1, 2, 3}
s := s - {1}   // {2, 3}
```

No indexing, slicing, or index assignment. Update only by whole reassignment.

### Size and methods

`s.length` / `length(s)`. Sets support `.filter` / `.map` / `.fold` (see below). **Set `.fold` iteration order is unspecified** — prefer `List` when order matters.

## Empty literals and typing

| Literal | Needs |
|---------|--------|
| `[]` | known `List<…>` or `Map<…>` target |
| `{}` | known `Set<…>` target |

```jul
var xs : List<Int>
xs := []                    // OK
var mp : Map<String, Int>
mp := []                    // OK — Map, not List
```

Assigning a map literal to a list variable (or the reverse) is a type error.

## Properties

| Property | Types | Result |
|----------|-------|--------|
| `.length` | `List`, `Set`, `Map` | `Int` (same as funlib `length`) |
| `.keys` | `Map<K,V>` | `Set<K>` |

```jul
xs.length
mp.keys
mp.keys.length
```

These are **intrinsic** (no `import`). Freestanding `import julay.funlib.length` still works.

## Methods and lambdas

### Lambdas

Unary (for `map` / `filter`):

```jul
i -> i >= 0
```

Binary (for `fold`):

```jul
(acc, x) -> acc + x
```

Lambdas may close over outer variables (state, locals, action args in transit). They are not valid as standalone expressions.

### Methods

| Method | Receiver | Arguments | Result |
|--------|----------|-----------|--------|
| `.filter(p)` | `List` / `Set` | unary pred → `Boolean` | same collection kind |
| `.map(f)` | `List` / `Set` | unary → `U` | `List<U>` / `Set<U>` |
| `.fold(init, f)` | `List` / `Set` | init + `(Acc, Elem) -> Acc` | type of `init` |

```jul
ys := xs.filter(i -> i >= 3)
ts := s.map(x -> x + 1)
n := xs.fold(0, (acc, x) -> acc + x)
agree := mp.keys.filter(k -> mp[k] >= commitIndex)
```

The function argument may also be a **named** unary `fun`:

```jul
fun double(n : Int) : Int = n + n
ys := xs.map(double)
```

Freestanding `map(xs, f)` from `julay.funlib.map` accepts a named fun or a lambda (lists and sets only). Prefer the method form `.map`.

## Guards

Higher-order calls that only depend on concrete process state are encoded by evaluating the Kotlin form and embedding the result in Z3 (so patterns like `mp.keys.filter(...).length` work in guards). Calls that depend on **symbolic action arguments** are rejected in guards.

For **TLA+ / TLC**, list and set `.map` / `.filter` / `.length` are emitted (with list indexes shifted `+ 1`); `.fold` and map HOFs are not — see [Specifications — TLA+ translation limits](specifications.md#tla-translation-limits).

Runtime list/map indexing throws on out-of-bounds or missing keys. Symbolic map reads in guards may soft-default missing keys — do not rely on that for executable behavior; check `k in mp` first.

## See also

- [Types and expressions](types-and-expressions.md)
- [Standard library](standard-library.md) — funlib `length` / `map`
- [Reference](reference.md)
- [List server example](../examples/list-server.md)
- Regression coverage: [`regression/input/list/`](../../regression/input/list/), [`map/`](../../regression/input/map/), [`set/`](../../regression/input/set/), [`expr/`](../../regression/input/expr/)
