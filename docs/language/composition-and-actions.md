# Composition and actions

## Parallel composition

Compose procs with `||`:

```jul
proc IncServer := Counter || Printer || ServerLogic
```

Each component keeps its own state. They interact only by **synchronizing on shared actions**.

`||` is **occurrence-based** and **left-associative**: each mention of a proc class is a separate occurrence (`A || A` is two occurrences of `A`, not one). Do not confuse this with exchanging action arguments **by copy** (no shared references).

When both sides of a binary `||` offer the same **ordinary** (untagged) or `session` action with a matching signature, they sync and that action becomes **internal to the composition** — it is not part of the outer alphabet. Unilateral actions and unmatched `client` actions remain visible on the assembly. A `provider` stays external after meeting clients (further clients can still sync with it); those clients leave the external alphabet. Source-tagged `internal` actions never leave their declaring proc and may reuse names freely.

```jul
proc X := A || B    // A,B sync on y → y internal to X (private channel)
proc Z := C || D    // C,D sync on y → y internal to Z (different private channel)
proc W := X || Z
```

In `W`, **A does not sync with C or D on `y`**, and **B does not sync with C or D on `y`**. Same surface name, distinct hidden events.

The same scoping applies to duplicated classes under different partners:

```jul
proc P := (A || X) || (B || X)   // or: Left := A||X; Right := B||X; P := Left || Right
```

Here the two occurrences of `X` sync with `A` and `B` respectively on a shared action `w`; those events stay private to each pair. Tooling: `julayc analyze --tree` lists each occurrence; `--actions-detail --include-internal` shows composition-hidden offers with distinct `scope=…` channel keys (not collapsed by class name).

### Provider / client (local compose rules)

| Offers on `w` | Behavior |
|---------------|----------|
| ordinary / ordinary | sync → composition-hide |
| `client` / `client` | do **not** hide; both stay external |
| `client` / `provider` | provider stays external; clients leave the external alphabet |
| ordinary / `provider` or ordinary / `client` | **compile error** |
| `provider` / `provider` | **compile error** (at most one provider) |

`client` is an explicit opt-in: two clients never pairwise-hide with each other, so they can wait for a `provider` that appears later in the composition tree. Ordinary peers that already hid `w` inside a library stay sealed — `(A || B) || P || C` still compiles when `A`/`B` are ordinary and `P`/`C` are provider/client.

Same-class ordinary offers never sync: when two occurrences of class `X` both expose untagged `w` at a compose step (e.g. `(A || X) || (B || X)` with no peer syncing on `w`), that is a **compose-time** error. A later `provider` cannot redeem those offers (ordinary + provider is also illegal).

### Alphabet integrity (JAR and TLA+)

**Complete sync** of `client`, ordinary (default), and `session` actions is required only on the **top-level `compile` / TLA+ target** — not on intermediate apis or procs that a parent will compose later.

On that top-level target:

- External `client` of `w` with no `provider` `w` → error, **except** when every external client offer for `w` comes from a **procfun** (call-folded or listed in an api's `calls:`). Those surfaces stay visible on the parent until a provider peer is composed.
- At most one `provider` per action name (always checked, including intermediate analyze).
- Unsynced ordinary or session actions in the external alphabet → **warn** (JAR still succeeds). Tag them `internal` if a solo step is intentional. Specs may still use unilateral assume/system actions.

`analyze`, IDE alphabet panels, and `check` without a `compile` directive do not require incomplete client / ordinary / session wiring to be resolved yet.

### Procfuns and alphabets

Procfuns are **not** allowed in `||` (compile error). They are not SyncChannel-started peers. For **analyze / IDE alphabets**, any procfun **called** by a host under an assembly contributes its non-synthetic actions to that assembly. To **couple** a procfun in TLA+, list it in an [api](#apis)'s `calls:` (see below).

### TLA+ occurrence names

When emitting TLA+, a unique leaf class keeps its name. If class `X` appears more than once, every occurrence is renamed using the introducing assembly: `proc P := A || X` and `proc Q := B || X` composed together become `X_P` and `X_Q`. Same-parent ties (`proc P := X || X`) use `X_P_1`, `X_P_2`, …. Renamed state variables get `(* ... *)` comments. Julay invariants still write `X.n`; the compiler expands them per occurrence in TLA+.

## APIs

An **api** packages a resident parallel composition (`proc:`) with optional procfun entry points (`calls:`). Conceptually it is one composition unit:

```jul
api RpcOut := <proc-field> || <call₁> || <call₂> || …
```

Operationally, only `proc:` leaves are SyncChannel-started; `calls` participate in alphabet, channel keys, and TLA coupling, and are still spawn-awaited when invoked.

### Syntax

```jul
export api RpcOut {
    proc: RpcOutClient || HttpClient
    calls: rpcOutClientCaller, outRequestVoteRPC, outAppendEntriesRPC
}
```

- `proc:` — required; parallel composition of **procs and/or apis** only (procfun names are illegal here, as in any `||`).
- `calls:` — optional; comma-separated procfun names owned by this api.

### Author mental model

As the api author, treat everything in the api as one parallel composition: the `proc:` peers plus each `calls` procfun. Matching actions sync and may become internal to the api. Incomplete wiring is allowed at declaration time:

```jul
export api RpcOut {
    proc: HttpClient
    calls: rpcOutClientCaller
}
```

This is legal. `rpcOutClientCaller`'s `sendRpcOut` / `responseRpcOut` do not meet a peer inside the api, so they remain **external actions of `RpcOut`** (visible in `analyze -s RpcOut` and the VS Code alphabet panel). A parent may still compose a partner that syncs those actions. `compile RpcOut` yields the usual unsynced ordinary/session **warning** (same as proc assemblies), not a hard error.

### Consumer mental model

Consumers treat an api as:

1. A process-like unit to compose: `… || RpcOut || …`
2. Qualified call entry points: `RpcOut.outRequestVoteRPC(...)`

```jul
import node.rpc_out.RpcOut

// compose
proc System := Client || RpcOut

// call — no separate import of outRequestVoteRPC required
transit:
    let ok : Boolean := RpcOut.outRequestVoteRPC(peer, payload)
```

- `import path.Api` is enough to compose that api and to call every procfun listed in its `calls:` as `Api.fn(...)`. Those procfuns need not be `export`ed (and usually should not be — the api is the public surface).
- Outside the api, listed procfuns **must** be called as `ApiName.fn(...)` (not bare `fn(...)`).
- Inside other procfuns of the same module, bare calls to siblings remain allowed (e.g. `outRequestVoteRPC` calling `rpcOutClientCaller`).
- `ApiName.fn(...)` requires a **unique** occurrence of that api in the enclosing composition (error if missing or duplicated).

### Nesting

Apis may appear in other apis' `proc:` fields and in ordinary `||`:

```jul
export api RaftNode {
    proc: RaftNodeMain || RaftCore || RpcOut || RpcIn
        || ElectionTimeout || Timer || VoteRequester || LeaderHeartbeat
}
```

Nested api `calls` stay in the nested scope; a parent does not automatically re-export child calls.

### Specs and TLA+

Apis are valid systems wherever procs are:

```jul
api RpcOut1 {
    proc: RpcOutClient
    calls: rpcOutClientCaller
}
spec RpcOutSpec1 := RpcOut1
// TLA models RpcOutClient || rpcOutClientCaller (caller coupled)

api RpcOut2 {
    proc: RpcOutClient
}
spec RpcOutSpec2 := RpcOut2
// TLA models RpcOutClient only

spec WithInv := RpcOut1 |= SomeInv
spec AG := <Env> RpcOut1 <Guarantees>
```

| Situation | TLA |
|-----------|-----|
| Procfun listed in `calls` of an api in the composition | **Coupled** |
| Procfun called but not listed in any composed api's `calls` | **Havoc** + warning |
| Procfun name written in `\|\|` | **Compile error** |

### Parallel composition may only use procs and apis

```jul
// ERROR
proc Bad := Main || countUp
```

Use an api instead:

```jul
api CountUpApi {
    proc: Main
    calls: countUp
}
spec CountUpSpec := CountUpApi
```

## Synchronization (language level)

When two (or more, for some roles) procs offer the **same action** with compatible arguments and guards, they take a **synchronized step** together.

```mermaid
flowchart LR
  subgraph peers [Parallel composition]
    A[Proc A]
    B[Proc B]
  end
  A -->|"offer action foo"| Foo[action foo]
  B -->|"offer action foo"| Foo
  Foo -->|"synchronized step"| A
  Foo -->|"synchronized step"| B
```

Important consequences of Julay’s philosophy:

- Processes **do not share resources**; a proc may **interface** a resource for others.
- Action arguments are exchanged **by copy**, never by reference—no shared mutable objects across procs.

### Same class never syncs

**Two different occurrences of the same proc class never synchronize with each other** on ordinary (default) or `internal` actions. Only **distinct proc classes** can pair on a shared action.

`provider` / `client` actions pair one hub with many clients—not two equal peers of the same class collaborating as duplicates of each other.

## Action modifiers

| Modifier | Role |
|----------|------|
| *(none)* | Ordinary rendezvous between complementary peers |
| `internal` | Local / solo-style step (no cross-class pairing of the ordinary kind) |
| `provider` | One hub for an action name; syncs with `client`s |
| `client` | Only syncs with a `provider` of the same name (never with other clients) |
| `session` | Sticky pairwise protocol—see [Sessions](sessions.md) |

`session` is incompatible with `provider`, `client`, and `internal`. If two classes share any session action, they must not also share an ordinary (untagged) action; ordinary sync or `provider`/`client` with a *different* peer class is fine (examples in [Sessions](sessions.md)).

Examples:

```jul
provider transition increment() { ... }   // API on Counter
client transition increment() { ... }     // handler uses Counter
internal transition println(msg : String) { ... }
session transition createHttpServer(port : Int) { ... }
```

## Offering the same action

For ordinary sync, different classes declare the same action name. For a shared API, the hub uses `provider` and callers use `client`. For example, `IncReqHandler` offers `client transition increment()` while `Counter` offers `provider transition increment()` ([inc server](../examples/inc-server.md)).

## See also

- [Processes](processes.md)
- [Procfuns](procfun.md) — call-site alphabet folding; packaging via [apis](#apis)
- [Sessions](sessions.md)
- [Creating libraries](creating-libraries.md)
