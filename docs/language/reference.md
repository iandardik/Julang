# Language reference

Compact cheat sheet. For explanations, use the other chapters.

## Keywords

`proc` `procfun` `type` `fun` `import` `export` `compile` `spec` `invariant`  
`var` `const` `constructor` `transition`  
`internal` `provider` `client` `session`  
`guard` `before` `transit` `error` `after` `return`  
`also` `with` `this` `global` `init`  
`forall` `exists` `if` `else` `let` `when` `in` `to`  
`true` `false` `listOf` `setOf` `mapOf`

`let` has two forms: expression `let (name : Type := init) body` (braces optional; [Types and expressions](types-and-expressions.md#expression-let)) and transit statement `let name : Type := expr` ([Side effects](effects.md#transit-statement-let)).

`this.x` in a proc/procfun/leaf-spec action body always means state variable `x` (even when an action arg is also named `x`); bare names still prefer args. See [Processes](processes.md#this-vs-action-args).

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
| Other | `.` `->` (lambdas / `when` / `error` arms), `to` (only inside `mapOf`); funlib `listOf` / `setOf` / `mapOf` / `splice` / `allDistinct`; collection `.keys` / `.length` / `.filter` / `.map` / `.associateWith` / `.fold` / `.toSet` / `.toList` — [Collections](collections.md); TLA limits — [Specifications](specifications.md#tla-translation-limits) |

## Top-level declarations

```text
import Module.Path
proc Name { ... }
proc Name := Expr
procfun name(...) : Type { ... }
type Name { ... }                           // record
type Name := TypeExpr                       // typedef
type Name                                   // uninterpreted
// delayed model Name := { lit, ... } only inside create-index / leaf-spec body
fun name(...) : Type = expr
invariant Name := Expr
spec Name { ... }                          // leaf spec (optional [p : T] body binder; no state lift)
spec Name[p : T] { ... }                   // leaf spec with decl param
spec Name := <Assume> System <Guarantee>
spec Name := System |= Guarantee
spec Name := System
spec Name := with (v : T) { System }       // shared apply-binder scope
spec Name := System[v : T] { const global x } // create-index; x unindexed & immutable in TLA+
spec Name := System[v : T] { const global x; init: expr } // extra Init conjuncts (const-global)
spec Name := System[v : T] { global y }       // create-index; y unindexed (mutable) in TLA+
compile Name1, Name2, ...
```

System atoms: `Name[v : T]` **creates** an index (lifts state); `{ const global x }` / `{ global y }` on create-index leave those vars scalar in TLA+ (`const global` also freezes them; does not affect JAR). Inside `with`, only `Name[v]` **applies** a shared binder. Shorthand `(A || B)[n : T]` desugars to create-temps + `with` + applies — see [Specifications](specifications.md#indexes-create-with-and-apply).

Leaf-spec actions: `transition name(args) also (aux : T) { … }` (leaf specs only). Bodies may read peer state `P.var` / `P[idx].var` (compile checks composition + indexing).

**Types:** record `type Name { fields }`; typedef `type Name := Carrier`; uninterpreted `type Name`. Delayed models `Name := { lits }` appear only in create-index or leaf-spec bodies — required for uninterpreted when used, optional for typedef (cfg aliases carrier if absent), forbidden for records. Uninterpreted types are spec-only; JAR `compile` errors if a proc reaches one. Typedefs erase to the carrier in procs. Details: [Types and expressions](types-and-expressions.md#delayed-models).

`Guarantee` may be a named invariant, an inline Boolean formula (`true` / `false` included), or `true` meaning no guarantee. Plain `spec := System` equals `<true> System <true>` / `--compile-tla`.

`compile` targets: **proc** → JAR (fails if the assembly reaches an uninterpreted type); **spec** (composition or leaf) → `.tla` / `.cfg`; **procfun** → standalone TLA/analyze (not a JAR root). Leaf specs must not appear in `proc Name := …` assemblies.

Procfuns cannot appear in `||`. List them in an [api](composition-and-actions.md#apis)'s `calls:` for TLA coupling. Parent **alphabets** always include called procfuns' non-synthetic actions. See [Procfuns](procfun.md).

## Funlib (`import julay.funlib.<name>`)

Pure / expression helpers: `length` · `max` · `min` · `parseInt` · `startsWith` · `split` · `trim` · `portFromUrl`  
(`split` / `trim` / `portFromUrl`: not for guards)

Effectful: `println` · `readln` · `readFile` · `exitProgram(code)` · `exitProc` · `delaySeconds` · `delayMillis` · `exitSession(Peer)` · `killSessionPeer(Peer)`  
(`exitSession` / `killSessionPeer`: transitions only; Peer is a leaf proc-class name; no-op if affinity absent)  
(`exitProc`: ordinary procs / leaf-spec actions only — **compile error inside procfuns**)

Full tables: [Standard library](standard-library.md). Calling conventions: [Side effects](effects.md).

## Common mistakes

- Constructors cannot have `guard:` (only transitions can)—see [`constructor-has-guard.jul`](../../regression/input/syntax-errors/constructor-has-guard.jul).

Browse negative cases under:

- [`regression/input/syntax-errors/`](../../regression/input/syntax-errors/)
- [`regression/input/type-errors/`](../../regression/input/type-errors/)
- [`regression/input/imports/`](../../regression/input/imports/)

## Grammar

Authoritative syntax: [`JulayLexer.g4`](../../src/main/java/julay/parser/JulayLexer.g4), [`JulayParser.g4`](../../src/main/java/julay/parser/JulayParser.g4).
