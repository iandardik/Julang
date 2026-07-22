# List server

**Sources:** [`input/list_server/`](../../input/list_server/)

| File | Role |
|------|------|
| [`main.jul`](../../input/list_server/main.jul) | `RunningList`, composition, `compile` |
| [`server.jul`](../../input/list_server/server.jul) | HTTP starter + per-request handler |
| [`printer.jul`](../../input/list_server/printer.jul) | Periodic `getList` + print |

## Intent

Same architectural pattern as the [inc server](inc-server.md), but the resource is a **`List<String>`**. Clients append via HTTP; the list lives only inside `RunningList`, exposed through service actions.

## The list as an interface

```jul
proc RunningList {
    var list : List<String>

    constructor initially(args : List<String>) {
        transit: list := []
    }

    service transition getList(lst : List<String>) {
        guard: lst = list
    }

    service transition getAndAppend(e : String, lst : List<String>) {
        guard: lst = list
        transit: list := list + [e]
    }
}
```

- `getList` publishes the current list by synchronizing on an equal copy.
- `getAndAppend` takes the element to append and the pre-image list (must match), then updates to `list + [e]`.

Peers never share a reference to `list`; they agree on values at sync time ([philosophy](../language/README.md)).

## Composition

```jul
proc ListServer := RunningList || Printer || ServerLogic
```

### HTTP handler (`server.jul`)

`IncReqHandler` (name reused from the inc demo) on `receiveRequest`:

1. Stores the request body
2. Syncs `getAndAppend(e, lst)` with `e = reqBody`
3. Responds with the updated list stringified in the HTTP body

### Printer (`printer.jul`)

```jul
proc Printer := PeriodicPrint || Println
```

Periodically `getList`s, prints the list via `println`, waits 5 seconds (`delaySeconds`).

## How to run

```bash
./gradlew shadowJar
java -jar build/libs/julayc.jar input/list_server/main.jul
```

Run the `ListServer` JAR (port **8000**). POST bodies are appended to the running list.

## Language features showcased

- `List<String>` state and list concatenation in `transit`
- Service interface for a collection resource
- Same HTTP session + modular layout as echo/inc
- Printer as a separate consumer of `getList`

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
