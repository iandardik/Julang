# Inc server

**Sources:** [`input/inc_server/`](../../input/inc_server/)

| File | Role |
|------|------|
| [`main.jul`](../../input/inc_server/main.jul) | `Counter`, composition, invariants, specs, `compile` |
| [`server.jul`](../../input/inc_server/server.jul) | HTTP starter + `incHandler` procfun |
| [`printer.jul`](../../input/inc_server/printer.jul) | Periodic read of the counter |

## Intent

A counter exposed as a **provider interface**: HTTP handlers and a printer never hold shared mutable state with `Counter`. They synchronize on `increment` / `getCounter` (`provider` on Counter, `client` on handlers), and values move **by copy**. The same file also declares **specs** so you can emit JARs and TLA+ together—Julay’s verification-aware story in one demo.

## The counter as an interface

```jul
proc Counter {
    var counter : Int

    constructor initially(args : List<String>) {
        transit: counter := 0
    }

    provider transition increment() {
        transit: counter := counter + 1
    }

    provider transition getCounter(counterVal : Int) {
        guard: counterVal = counter
    }
}
```

- `increment` mutates only Counter’s local state.
- `getCounter(counterVal)` syncs when the peer’s `counterVal` equals the true counter—the peer learns the value by agreement on the argument (copy), not by reading Counter’s memory.

This matches the [language philosophy](../language/README.md): no shared resources; the proc **is** the interface.

## Composition

```jul
proc IncServer := Counter || Printer || ServerLogic
```

### HTTP path (`server.jul`)

```jul
procfun incHandler(req : HttpServerRequest) : HttpServerResponse { ... }

proc ServerLogic := ServerStarter || HttpServer
```

`incHandler` is registered at startup (`handler = incHandler` on `listen`). Each request spawns a procfun instance via `Program.invokeProcFun`; the handler then:

1. Syncs on `increment` (with Counter’s provider)
2. Syncs on `getCounter` to learn `localCounter`
3. Returns an `HttpServerResponse` with that value as the body

Handler and Counter are **different classes**, so they may sync ([Composition](../language/composition-and-actions.md)).

### Printer (`printer.jul`)

`PeriodicPrint` repeatedly `getCounter`s, prints, and `delaySeconds(5)`—again using Counter only through actions.

## Specs and invariants

From [`main.jul`](../../input/inc_server/main.jul):

```jul
invariant NonNegative := Counter.counter >= 0
invariant CorrectCounter := forall t1 : Int,
    (incHandler[t1].step = "C" & forall t2 : Int, incHandler[t2].step ~= "B")
        => (incHandler[t1].localCounter = Counter.counter)
invariant AllInvs := NonNegative & CorrectCounter

spec HandlerSpec := incHandler[t : Int]
spec IncSpec := <true> Counter || HandlerSpec <AllInvs>

compile IncServer, IncSpec
```

- **`IncServer`** → runnable JAR  
- **`HandlerSpec` / `IncSpec`** → `.tla` / `.cfg` for TLC  

Indexed `incHandler[t : Int]` models many handler instances. See [Specifications](../language/specifications.md).

## How to run

```bash
./gradlew shadowJar
java -jar build/libs/julayc.jar input/inc_server/main.jul
```

This writes `IncServer.jar` (and `IncSpec` TLA files) in the current directory. Start the server on port **8000**, then POST with curl:

```bash
java -jar IncServer.jar           # terminal 1

curl -s -X POST -d 'hello' http://localhost:8000/
curl -s -X POST -d 'hello' http://localhost:8000/
curl -s -X POST -d 'hi' http://localhost:8000/
```

**Client** (curl) sample — each response body is the new counter value (request body is ignored):

```text
1
2
3
```

**Server** sample — the printer reads the counter every 5 seconds (so quick POSTs may skip intermediate values):

```text
0
0
3
3
```

Spec artifacts appear as `HandlerSpec` / `IncSpec` TLA files in the working directory for TLC.

## Language features showcased

- `provider` actions as a resource API
- Procfun HTTP handler with `client` steps to Counter
- Modules (`printer`, `server`)
- Assume-guarantee `spec`, indexed procfuns, `compile` mixing JAR and TLA targets

## See also

- [Specifications](../language/specifications.md)
- [Effects](../language/effects.md) — `delaySeconds` in the printer
- Previous: [Echo server](echo-server.md) · Next: [List server](list-server.md) · [RPC server](rpc-server.md)
