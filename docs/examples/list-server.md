# List server

**Sources:** [`input/list_server/`](../../input/list_server/)

| File | Role |
|------|------|
| [`main.jul`](../../input/list_server/main.jul) | `RunningList`, composition, `compile` |
| [`server.jul`](../../input/list_server/server.jul) | `serveHttp` + `listHandler` procfun |
| [`printer.jul`](../../input/list_server/printer.jul) | Periodic `getList` + print |

## Intent

Same architectural pattern as the [inc server](inc-server.md), but the resource is a **`List<String>`**. HTTP handlers append; the list lives only inside `RunningList`, exposed as a **`service` hub** with **`client`** callers.

## The list as an interface

```jul
import julay.funlib.listOf

static proc RunningList {
    var list : List<String>

    constructor initially(args : List<String>) {
        transit: list := listOf()
    }

    service transition getList(lst : List<String>) {
        guard: lst = list
    }

    service transition getAndAppend(e : String, lst : List<String>) {
        guard: lst = list
        transit: list := list + listOf(e)
    }
}
```

- `getList` publishes the current list by synchronizing on an equal copy.
- `getAndAppend` takes the element to append and the pre-image list (must match), then updates to `list + listOf(e)`.

Peers never share a reference to `list`; they agree on values at sync time ([philosophy](../language/README.md)).

## Composition

```jul
proc ListServer := RunningList || Printer || ServerLogic
```

### HTTP handler (`server.jul`)

`listHandler` procfun (registered via `serveHttp(8000, listHandler)`):

1. Stores the request body
2. Syncs `getAndAppend(e, lst)` with `e = reqBody`
3. Returns an `HttpServerResponse` with the updated list stringified in the body

### Printer (`printer.jul`)

```jul
proc Printer := PeriodicPrint
```

Periodically `getList`s, prints the list via funlib `println` in an `after:` block, waits 5 seconds (`delaySeconds`).

## How to run

```bash
./gradlew shadowJar
java -jar build/libs/julayc.jar input/list_server/main.jul
```

This writes `ListServer` in the current directory. Start the server on port **8000**, then POST bodies to append:

```bash
./ListServer          # terminal 1

curl -s -X POST -d 'hello' http://localhost:8000/
curl -s -X POST -d 'world' http://localhost:8000/
```

**Client** (curl) sample — each response body is the updated list:

```text
[hello]
[hello, world]
```

**Server** sample — the printer reads the list every 5 seconds (so quick POSTs may skip intermediate values):

```text
[]
[]
[hello, world]
[hello, world]
```

## Language features showcased

- `List<String>` state and list concatenation in `transit`
- Static `service` hub for a collection resource; handlers/printer as `client`
- Procfun HTTP handler + modular layout (same pattern as echo/inc)
- Printer as a separate peer on `getList`

## Compare to inc server

| | Inc | List |
|--|-----|------|
| Resource proc | `Counter` | `RunningList` |
| Mutating API | `increment` | `getAndAppend` |
| Read API | `getCounter` | `getList` |
| Specs in `main.jul` | Yes (`IncSpec`, …) | No (execution-focused) |

## See also

- [Types and expressions](../language/types-and-expressions.md) — lists
- [Inc server](inc-server.md) — verification-aware sibling
- [Echo server](echo-server.md) — client/server split
- [RPC server](rpc-server.md) — parse/route churn on a counter core
