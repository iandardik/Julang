# Language reference

Compact cheat sheet. For explanations, use the other chapters.

## Keywords

`proc` `obj` `sort` `fun` `import` `export` `compile` `spec` `invariant`  
`var` `const` `constructor` `transition`  
`internal` `service` `session`  
`guard` `before` `transit` `error` `after`  
`all` `exists` `if` `else` `let` `when` `in`  
`true` `false`

## Operators

| Class | Operators |
|-------|-----------|
| Logic | `&` `\|` `~` `=>` `<=>` |
| Compare | `=` `~=` `<` `<=` `>` `>=` |
| Arithmetic | `+` `-` `*` `/` `%` |
| Assign | `:=` |
| Models | `\|=` (system with guarantee) |
| Parallel | `\|\|` |
| Other | `.` `->` (map entries), list/map/set literals |

## Top-level declarations

```text
import Module.Path
proc Name { ... }
proc Name := Expr
obj Name { ... }
sort Name := { lit, ... }
fun name(...) : Type { ... }
invariant Name := Expr
spec Name := <Assume> System <Guarantee>
spec Name := System |= Guarantee
spec Name := System
compile Name1, Name2, ...
```

`sort` declares a finite homogeneous domain (String, non-negative Int, or Boolean literals) for **spec index / quantifier domains only**. It becomes a TLA+ `CONSTANT` with the exact set in the `.cfg`. Do not use sorts as proc state, action args, or `obj` fields.

`Guarantee` may be a named invariant, an inline Boolean formula (`true` / `false` included), or `true` meaning no guarantee. Plain `spec := System` equals `<true> System <true>` / `--compile-tla`.

`compile` targets: **proc** → JAR; **spec** → `.tla` / `.cfg`.

## Funlib (`import julay.funlib.<name>`)

Pure / expression helpers: `length` · `parseInt` · `startsWith` · `split` · `trim` · `portFromUrl`  
(`split` / `trim` / `portFromUrl`: not for guards)

Effectful: `println` · `readln` · `readFile` · `exitProcess` · `delaySeconds` · `exitSession(Peer)` · `killSessionPeer(Peer)`  
(`exitSession` / `killSessionPeer`: transitions only; Peer is a leaf proc-class name; no-op if affinity absent)

Full tables: [Standard library](standard-library.md). Calling conventions: [Side effects](effects.md).

## Common mistakes

Browse negative cases under:

- [`regression/input/syntax-errors/`](../../regression/input/syntax-errors/)
- [`regression/input/type-errors/`](../../regression/input/type-errors/)
- [`regression/input/imports/`](../../regression/input/imports/)

## Grammar

Authoritative syntax: [`JulayLexer.g4`](../../src/main/java/julay/parser/JulayLexer.g4), [`JulayParser.g4`](../../src/main/java/julay/parser/JulayParser.g4).
