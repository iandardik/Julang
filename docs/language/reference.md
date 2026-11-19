# Language reference

Compact cheat sheet. For explanations, use the other chapters.

## Keywords

`proc` `static` `type` `fun` `import` `export` `compile` `spec` `invariant` `fluent`  
`on` `initially`  
`var` `const` `constructor` `transition` `return`  
`service` `client` `internal` `timer`  
`guard` `before` `transit` `error` `after`  
`also` `with` `this` `global` `init`  
`forall` `exists` `if` `else` `let` `when` `in` `to`  
`true` `false` `listOf` `setOf` `mapOf`

`let` has two forms: expression `let (name : Type := init) body` (braces optional; [Types and expressions](types-and-expressions.md#expression-let)) and transit statement `let name : Type := expr` ([Side effects](effects.md#transit-statement-let)).

`this.x` in a proc / leaf-spec action body always means state variable `x` (even when an action arg is also named `x`); bare names still prefer args. See [Processes](processes.md#this-vs-action-args).

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
static proc Name { ... }
proc Name := Expr                          // nest children with || (parent inferred)
type Name { ... }                           // record
type Name := TypeExpr                       // typedef
type Name                                   // uninterpreted
// delayed model Name := { lit, ... } only inside create-index / leaf-spec body
fun name(...) : Type = expr
invariant Name := Expr
fluent Name(p : T, …) { initially Expr  on Act[n](a, _) := true | ~Other(a) … }
static spec Name { ... }                   // static leaf (initially only; optional [p : T] binder)
static spec Name[p : T] { ... }            // static leaf with decl param
spec Name { ... }                          // dynamic leaf (spawn ctor + return; same as dynamic proc)
spec Name := <Assume> System <Guarantee>
spec Name := System |= Guarantee
spec Name := System
spec Name := with (v : T) { System }       // shared apply-binder scope
spec Name := System[v : T] { const global x } // create-index; x unindexed & immutable in TLA+
spec Name := System[v : T] { const global x; init: expr } // extra Init conjuncts (const-global)
spec Name := System[v : T] { global y }       // create-index; y unindexed (mutable) in TLA+
spec Name := System[v : T] { DynProc := { … } } // optional; default {"dp1","dp2"}
compile Name1, Name2, ...
```

System atoms: `Name[v : T]` **creates** an index (lifts state); `{ const global x }` / `{ global y }` on create-index leave those vars scalar in TLA+ (`const global` also freezes them; does not affect the native binary). Inside `with`, only `Name[v]` **applies** a shared binder. Shorthand `(A || B)[n : T]` desugars to create-temps + `with` + applies — see [Specifications](specifications.md#indexes-create-with-and-apply). Parent/child edges are inferred from nested `||` — see [Parent/child inference](children.md).

Leaf-spec actions: `transition name(args) also (aux : T) { … }` (leaf specs only). Bodies may read peer state `P.var` / `P[idx].var` (compile checks composition + indexing).

**Types:** record `type Name { fields }`; typedef `type Name := Carrier`; uninterpreted `type Name`. Delayed models `Name := { lits }` appear only in create-index or leaf-spec bodies — required for uninterpreted when used, optional for typedef (cfg aliases carrier if absent), forbidden for records. Nullary fun overrides `name := expr` in those same places rewrite the TLA operator only (binary unchanged). Uninterpreted types are spec-only; proc `compile` errors if a proc reaches one. Typedefs erase to the carrier in procs. Details: [Types and expressions](types-and-expressions.md#delayed-models), [Specifications — Fun overrides](specifications.md#fun-overrides).

`Guarantee` may be a named invariant, an inline Boolean formula (`true` / `false` included), or `true` meaning no guarantee. Plain `spec := System` equals `<true> System <true>` / `--compile-tla`.

`Assume` may be `true`, a system expression, or a **named invariant** (formula assume, often over fluents). A bare name that is both an invariant and a declared system is an error. Formula assumes are not composed as env leaves; TLA conjoins a post-state `AssumeOK` (primed fluents) on actions. See [Specifications — Fluents](specifications.md#fluents).

`LocalInv |= <A> C <G>` attaches a local inductive invariant: the Obl checks `LocalInv` while A/G remain the contract. Same prefix works on circular comma forms. See [Local inductive invariant](specifications.md#local-inductive-invariant-localinv--a-c-g).

Circular AG uses comma slots: `<P, A> C1 <G>` / `<G> C2 <A, Safe>` (A/G fluent). Compiling a matched pair also emits `CircSide` TLC side conditions. See [Specifications — Circular AG](specifications.md#circular-ag-comma-slots). AG Obl `.cfg`s default to safety-only (`SpecSafety`, no `PROPERTY *Terminates`); see [AG Obl `.cfg`](specifications.md#ag-obl-cfg-safety-by-default-no-terminates).

`compile` targets: **proc** → native binary (fails if the assembly reaches an uninterpreted type); **spec** (composition or leaf) → `.tla` / `.cfg`. Leaf specs must not appear in `proc Name := …` assemblies. Only `static proc` / `static spec` may declare `constructor initially`. Dynamic procs and dynamic leaf specs must have at least one named `return` action.

## Funlib (`import julay.funlib.<name>`)

Pure / expression helpers: `length` · `max` · `min` · `parseInt` · `startsWith` · `split` · `trim` · `portFromUrl`  
(`split` / `trim` / `portFromUrl`: not for guards)

Effectful: `serveHttp` · `doHttpRequest` · `setTimer` · `cancelTimer` · `println` · `readln` · `readFile` · `exitProgram(code)` · `delaySeconds` · `delayMillis`  
(`cancelTimer`: transitions only; `setTimer` / `serveHttp` also OK in constructor `after:`)  
(`serveHttp(port, Class.ctor ~> Class.action)` — see [Parent/child inference](children.md#http-handlers))

Full tables: [Standard library](standard-library.md). Calling conventions: [Side effects](effects.md).

## Common mistakes

- Constructors cannot have `guard:` (only transitions can)—see [`constructor-has-guard.jul`](../../regression/input/syntax-errors/constructor-has-guard.jul).

Browse negative cases under:

- [`regression/input/syntax-errors/`](../../regression/input/syntax-errors/)
- [`regression/input/type-errors/`](../../regression/input/type-errors/)
- [`regression/input/imports/`](../../regression/input/imports/)

## Grammar

Authoritative syntax: [`JulayLexer.g4`](../../src/main/java/julay/parser/JulayLexer.g4), [`JulayParser.g4`](../../src/main/java/julay/parser/JulayParser.g4).
