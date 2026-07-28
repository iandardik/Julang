# Higher-order functions

Collection methods and inline lambdas. Julay does **not** have first-class function values: lambdas exist only as arguments to higher-order calls, and bodies are inlined at compile time.

## Lambdas

Unary (for `map` / `filter`):

```jul
i -> i >= 0
```

Binary (for `fold`):

```jul
(acc, x) -> acc + x
```

Lambdas may close over outer variables (state, locals, action args in transit). They are not valid as standalone expressions.

## Properties

| Property | Types | Result |
|----------|-------|--------|
| `.length` | `List`, `Set`, `Map` | `Int` (same as funlib `length`) |
| `.keys` | `Map<K,V>` | `Set<K>` |

```jul
xs.length
m.keys
m.keys.length
```

These are **intrinsic** (no `import`). Freestanding `import julay.funlib.length` still works.

## Methods

| Method | Receiver | Arguments | Result |
|--------|----------|-----------|--------|
| `.filter(p)` | `List` / `Set` | unary pred → `Boolean` | same collection kind |
| `.map(f)` | `List` / `Set` | unary → `U` | `List<U>` / `Set<U>` |
| `.fold(init, f)` | `List` / `Set` | init + `(Acc, Elem) -> Acc` | type of `init` |

```jul
ys := xs.filter(i -> i >= 3)
ts := s.map(x -> x + 1)
n := xs.fold(0, (acc, x) -> acc + x)
agree := m.keys.filter(k -> m[k] >= commitIndex)
```

The function argument may also be a **named** unary `fun` (same as freestanding `map`):

```jul
fun double(n : Int) : Int = n + n
ys := xs.map(double)
```

Freestanding `map(xs, f)` accepts a named fun or a lambda.

**Set `.fold`:** iteration order is unspecified (same as Kotlin). Prefer `List` when order matters.

## Guards

HOFs that only depend on concrete process state are encoded by evaluating the Kotlin form and embedding the result in Z3 (so patterns like `m.keys.filter(...).length` work in guards). HOFs that depend on **symbolic action arguments** are rejected in guards.

## See also

- [Types and expressions](types-and-expressions.md)
- [Standard library](standard-library.md) — funlib `length` / `map`
- [Reference](reference.md)
