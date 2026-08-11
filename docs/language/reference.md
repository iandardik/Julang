# Language reference

Compact cheat sheet. For explanations, use the other chapters.

## Keywords

`proc` `procfun` `obj` `sort` `fun` `import` `export` `compile` `spec` `invariant`  
`var` `const` `constructor` `transition`  
`internal` `provider` `client` `session`  
`guard` `before` `transit` `error` `after` `return`  
`also` `with`  
`forall` `exists` `if` `else` `let` `when` `in` `to`  
`true` `false` `listOf` `setOf` `mapOf`

`let` has two forms: expression `let (name : Type := init) body` (braces optional; [Types and expressions](types-and-expressions.md#expression-let)) and transit statement `let name : Type := expr` ([Side effects](effects.md#transit-statement-let)).

`to` is only legal inside `mapOf(...)`. `listOf` / `setOf` / `mapOf` require `import julay.funlib.<name>`.

## Operators

| Class | Operators |
|-------|-----------|
| Logic | `&` `\|` `~` `=>` `<=>` |
| Compare | `=` `~=` `<` `<=` `>` `>=` |
| Membership | `in` `~in` |
| Arithmetic | `+` `-` `*` `/` `%` |
| Assign | `:=` |
| Models | `\|=` (system with guarantee) |
| Parallel | `\|\|` |
| Other | `.` `->` (lambdas / `when` / `error` arms), `to` (only inside `mapOf`); funlib `listOf` / `setOf` / `mapOf` / `splice`; collection `.keys` / `.length` / `.filter` / `.map` / `.fold` — [Collections](collections.md); TLA limits — [Specifications](specifications.md#tla-translation-limits) |

## Top-level declarations

```text
import Module.Path
proc Name { ... }
proc Name := Expr
procfun name(...) : Type { ... }
obj Name { ... }
sort Name := { lit, ... }
fun name(...) : Type = expr
invariant Name := Expr
spec Name { ... }                          // leaf spec (optional [p : T] body binder; no state lift)
spec Name[p : T] { ... }                   // leaf spec with decl param
spec Name := <Assume> System <Guarantee>
spec Name := System |= Guarantee
spec Name := System
spec Name := with (v : T) { System }       // shared apply-binder scope
compile Name1, Name2, ...
```

System atoms: `Name[v : T]` **creates** an index (lifts state); inside `with`, only `Name[v]` **applies** a shared binder. Shorthand `(A || B)[n : T]` desugars to create-temps + `with` + applies — see [Specifications](specifications.md#indexes-create-with-and-apply).

Leaf-spec actions: `transition name(args) also (aux : T) { … }` (leaf specs only). Bodies may read peer state `P.var` / `P[idx].var` (compile checks composition + indexing).

`sort` declares a finite homogeneous domain (String, non-negative Int, or Boolean literals) for **spec index / quantifier / leaf-spec parameter domains**, **leaf-spec state**, and **`obj` fields**. It becomes a TLA+ `CONSTANT` with the exact set in the `.cfg`. Do not use sorts as ordinary proc state or action args. Sort-bearing objs in a JAR `compile` target are an error. Sorts may be `export`ed and imported by name.

`Guarantee` may be a named invariant, an inline Boolean formula (`true` / `false` included), or `true` meaning no guarantee. Plain `spec := System` equals `<true> System <true>` / `--compile-tla`.

`compile` targets: **proc** → JAR (fails if the assembly reaches a sort-bearing type); **spec** (composition or leaf) → `.tla` / `.cfg`; **procfun** → standalone TLA/analyze (not a JAR root). Leaf specs must not appear in `proc Name := …` assemblies.

Procfuns cannot appear in `||`. List them in an [api](composition-and-actions.md#apis)'s `calls:` for TLA coupling. Parent **alphabets** always include called procfuns' non-synthetic actions. See [Procfuns](procfun.md).

## Funlib (`import julay.funlib.<name>`)

Pure / expression helpers: `length` · `max` · `min` · `parseInt` · `startsWith` · `split` · `trim` · `portFromUrl`  
(`split` / `trim` / `portFromUrl`: not for guards)

Effectful: `println` · `readln` · `readFile` · `exitProcess` · `delaySeconds` · `exitSession(Peer)` · `killSessionPeer(Peer)`  
(`exitSession` / `killSessionPeer`: transitions only; Peer is a leaf proc-class name; no-op if affinity absent)

Full tables: [Standard library](standard-library.md). Calling conventions: [Side effects](effects.md).

## Common mistakes

- Constructors cannot have `guard:` (only transitions can)—see [`constructor-has-guard.jul`](../../regression/input/syntax-errors/constructor-has-guard.jul).

Browse negative cases under:

- [`regression/input/syntax-errors/`](../../regression/input/syntax-errors/)
- [`regression/input/type-errors/`](../../regression/input/type-errors/)
- [`regression/input/imports/`](../../regression/input/imports/)

## Grammar

Authoritative syntax: [`JulayLexer.g4`](../../src/main/java/julay/parser/JulayLexer.g4), [`JulayParser.g4`](../../src/main/java/julay/parser/JulayParser.g4).
