# Language reference

Compact cheat sheet. For explanations, use the other chapters.

## Keywords

`proc` `obj` `fun` `import` `compile` `spec` `invariant`  
`var` `const` `constructor` `transition`  
`internal` `service` `session`  
`guard` `transit` `error` `effect`  
`all` `exists` `if` `else` `let` `when` `in`  
`true` `false`

## Operators

| Class | Operators |
|-------|-----------|
| Logic | `&` `\|` `~` `=>` `<=>` |
| Compare | `=` `~=` `<` `<=` `>` `>=` |
| Arithmetic | `+` `-` `*` `/` `%` |
| Assign | `:=` |
| Parallel | `\|\|` |
| Other | `.` `->` (map entries), list/map/set literals |

## Top-level declarations

```text
import Module.Path
proc Name { ... }
proc Name := Expr
obj Name { ... }
fun name(...) : Type { ... }
invariant Name := Expr
spec Name := <Assume> System <Guarantee>
spec Name := System
compile Name1, Name2, ...
```

`compile` targets: **proc** → JAR; **spec** → `.tla` / `.cfg`.

## Effect builtins

`println` · `readln` · `exitProcess` · `delaySeconds` · `exitSession` · `killSessionPeer`  
(`exitSession` / `killSessionPeer`: transitions only)

## Funlib builtins

`length` · `parseInt` · `startsWith` · `readFile` · `split` · `trim` · `portFromUrl`  
(Last four: not for guards)

## Common mistakes

Browse negative cases under:

- [`regression/input/syntax-errors/`](../../regression/input/syntax-errors/)
- [`regression/input/type-errors/`](../../regression/input/type-errors/)
- [`regression/input/imports/`](../../regression/input/imports/)

## Grammar

Authoritative syntax: [`JulayLexer.g4`](../../src/main/java/julay/parser/JulayLexer.g4), [`JulayParser.g4`](../../src/main/java/julay/parser/JulayParser.g4).
