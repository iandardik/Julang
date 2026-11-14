# Inc server

**Sources:** [`input/inc_server/`](../../input/inc_server/)

| File | Role |
|------|------|
| [`main.jul`](../../input/inc_server/main.jul) | `Counter`, composition, invariants, specs, `compile` |
| [`server.jul`](../../input/inc_server/server.jul) | `serveHttp` + `incHandler` procfun |
| [`printer.jul`](../../input/inc_server/printer.jul) | Periodic read of the counter |

## Intent

A counter exposed as a **`service` hub**: HTTP handlers and a printer never hold shared mutable state with `Counter`. They synchronize as **`client`s** on `increment` / `getCounter`, and values move **by copy**. The same file also declares **specs** so you can emit a native binary and TLA+ together—Julay’s verification-aware story in one demo.

## The counter as an interface

```jul
static proc Counter {
    var counter : Int

    constructor initially(args : List<String>) {
        transit: counter := 0
    }

    service transition increment() {
        transit: counter := counter + 1
    }

    service transition getCounter(counterVal : Int) {
        guard: counterVal = counter
    }
}
```

- `increment` mutates only Counter’s local state.
- `getCounter(counterVal)` syncs when the peer’s `counterVal` equals the true counter—the peer learns the value by agreement on the argument (copy), not by reading Counter’s memory.
- Handlers / printer use `client transition` so they sync only with this service (the hub stays in the external alphabet).

This matches the [language philosophy](../language/README.md): no shared resources; the proc **is** the interface. See [Composition — service/client](../language/composition-and-actions.md#action-modifiers).

## Composition

```jul
proc IncServer := Counter || Printer || ServerLogic
```

### HTTP path (`server.jul`)

```jul
static proc ServerStarter {
    constructor initially(args : List<String>) {
        after:
            serveHttp(8000, incHandler)
    }
}

export proc ServerLogic := ServerStarter
```

`incHandler` is registered via `serveHttp`. Each request runs the procfun, which then:

1. Syncs on `increment` (with Counter)
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

- **`IncServer`** → native binary  
- **`HandlerSpec` / `IncSpec`** → `.tla` / `.cfg` for TLC  

Indexed `incHandler[t : Int]` models many handler instances. See [Specifications](../language/specifications.md).

## How to run

```bash
./gradlew shadowJar
java -jar build/libs/julayc.jar input/inc_server/main.jul
```

This writes `IncServer` (and `IncSpec` TLA files) in the current directory. Start the server on port **8000**, then POST with curl:

```bash
./IncServer           # terminal 1

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

- `static proc` + `service` hub as a resource API; callers tagged `client`
- Procfun HTTP handler syncing with Counter
- Modules (`printer`, `server`)
- Assume-guarantee `spec`, indexed procfuns, `compile` mixing binary and TLA targets

## See also

- [Specifications](../language/specifications.md)
- [Effects](../language/effects.md) — `serveHttp`, `delaySeconds`
- Previous: [Echo server](echo-server.md) · Next: [List server](list-server.md) · [RPC server](rpc-server.md)
