# Julang regression tests

End-to-end checks for Julang programs. Each case compiles a `.jul` file, runs the generated JAR(s), and compares terminal output and/or HTTP responses to what you declare in YAML.

## Layout

```
regression/
  README.md           # this file
  input/              # .jul sources used by regression cases (mirrors old input/ layout)
  cases/              # YAML test scenarios, mirroring input/ layout (e.g. cases/basic/test1.yaml)
  expected/           # optional golden stdout files (referenced by path)
```

Case files live under **`regression/cases/`**, using the same subdirectory layout as `regression/input/` (e.g. `cases/basic/test1.yaml` pairs with `input/basic/test1.jul`). Paths inside YAML (`source`, `expectStdout`) are relative to the **project root** (where `build.gradle.kts` lives).

## Running tests

From the project root:

```bash
./gradlew test
```

Regression tests are `julay.regression.RegressionTest` and run as part of the normal test task. Gradle builds the compiler (`shadowJar` → `build/libs/julayc.jar`), then each case:

1. Creates an isolated workspace under `build/regression-workspace/`
2. Copies `julayc.jar` into that workspace
3. Compiles the `.jul` file (same as `java -jar julayc.jar <source>`)
4. Runs each listed program and checks expectations

**Note:** Each compile spawns a temporary Gradle build for generated code, so the suite is slow. HTTP cases also run **one at a time** (shared port 8000).

## Case file structure

Every case file must define **`source`**. Positive cases also need **`programs`**; negative compile-failure cases use **`expectCompileFailure`** instead (see [Negative tests](#negative-tests)).

```yaml
source: regression/input/basic/test1.jul   # required: path to .jul file
tags:                           # optional
  - http
programs:                       # required for positive cases
  - name: TermTest1             # must match `program <Name> := ...` in the .jul file
    dependsOn: EchoServer       # optional: start another program’s JAR first
    expectFailure: false        # optional: set true when the program run should fail
    run:                        # required per program entry (unless expectFailure-only)
      # ... run options below
```

A single `.jul` file may define **multiple** `program` declarations (e.g. `EchoServer` and `EchoClient`). List each program you want to exercise under `programs`. The harness compiles the file **once** per case and produces one JAR per program name (e.g. `TermTest1.jar`).

### `tags`

| Tag | Effect |
|-----|--------|
| `http` | Case runs under a global lock so only one HTTP test uses port 8000 at a time. Use for any case that starts `HttpServer` or depends on localhost:8000. |

### `programs[].name`

Must match the program identifier in the source file exactly (case-sensitive). This is also the JAR base name: `TermTest1` → `TermTest1.jar`.

### `programs[].dependsOn`

Optional. Name of another program in the **same** case file whose JAR should be started in the **background** before this program runs. Typical pattern: start `EchoServer`, then run `EchoClient`.

The harness waits until **port 8000** accepts connections before continuing.

## `run` options

All fields under `run` are optional except that each program should have a `run` block with at least one **expectation** (stdout or HTTP).

### Timing

| Field | Default | Description |
|-------|---------|-------------|
| `timeoutMs` | `30000` | Max time to wait for the program to **exit** (foreground runs only). |
| `durationMs` | — | How long to capture stdout from a **background** process (ms). Used with `http` or `background: true`. Falls back to `3000` when `http` is set and `durationMs` is omitted; falls back to `timeoutMs` when only `background: true`. |

### Input

| Field | Default | Description |
|-------|---------|-------------|
| `stdin` | `[]` | Lines fed to the process stdin. A newline is appended to each entry if it does not already end with `\n`. Used with `Readln` and similar. |
| `args` | `[]` | Command-line arguments passed after the JAR path (`java -jar Prog.jar …`). Bound to `initially(args : List String)`. |

Example:

```yaml
stdin:
  - "A"
args:
  - "cli"
```

### Execution mode

The harness picks a mode based on which fields you set:

| Mode | When | Behavior |
|------|------|----------|
| **Foreground** | Default (no `http`, no `background`) | Run JAR until it exits or `timeoutMs` elapses. |
| **HTTP + server** | `http` list is non-empty | Start program in background, wait for port 8000, send each HTTP check, capture stdout for `durationMs` (default 3s), then stop. |
| **Background** | `background: true` | Start JAR in background, capture stdout for `durationMs` or `timeoutMs`, then stop. |

You do not need `background: true` when using `http`; the harness starts the server automatically.

### HTTP checks (`http`)

List of POST requests sent to **`http://localhost:8000/`** (plain text body). Cases that use `HttpServer` should call `startServer` with port `8000` so the probe can connect.

```yaml
http:
  - post: "hello"                    # request body
    expectBody: "hello is a good point!"   # exact response body
```

Status code must be **200**. Checks run after the server port is open.

### Stdout expectations

You can combine several matchers; **all** specified matchers must pass.

| Field | Description |
|-------|-------------|
| `expectStdout` | Path to a golden file (e.g. `regression/expected/basic-test1-termtest1.stdout`). **Exact** match after normalizing line endings and trimming trailing whitespace on the expected file. |
| `expectStdoutContains` | List of substrings that must each appear somewhere in stdout. |
| `expectStdoutLinesUnordered` | List of lines; each must appear as a **non-blank** line in stdout (order does not matter). Good when `Println` order is nondeterministic. |
| `expectStdoutMatches` | Single regex matched anywhere in stdout (e.g. `"[0-9]+"` for timer output). |

At least one expectation field is recommended per `run` block (for positive tests).

## Negative tests

Negative cases verify that compilation or execution **fails as expected**.

### Compile failure (case-level)

Use when a `.jul` file should **not** compile. No `programs` block is needed.

| Field | Default | Description |
|-------|---------|-------------|
| `expectCompileFailure` | `false` | When `true`, the harness expects **no program JARs** to be produced. |
| `expectCompileOutputContains` | `[]` | Optional substrings that must appear in compiler stdout (e.g. `"type errors"`, `"compile errors"`). |

```yaml
source: regression/input/type-errors/transit-bool-to-int.jul
expectCompileFailure: true
expectCompileOutputContains:
  - "Expected assignment to \"x\" (Int) but got expression of type Bool"
```

The test **passes** when compilation produces no JARs. If any JAR is produced, the test fails. When `expectCompileOutputContains` is set, each listed substring must appear in the compiler output.

### Run failure (program-level)

Use when a program **should compile** but its run should fail — for example, it hangs until timeout or violates an assertion.

| Field | Default | Description |
|-------|---------|-------------|
| `expectFailure` | `false` | When `true`, a failed run (timeout, stdout mismatch, HTTP error, etc.) counts as **success**. A run that completes without error fails the test. |
| `expectFailureOutputContains` | `[]` | Optional substrings that must appear in the failure message (e.g. `"timed out"`). |

```yaml
source: input/server/inc.jul
programs:
  - name: CounterServer
    expectFailure: true
    expectFailureOutputContains:
      - "timed out"
    run:
      timeoutMs: 1000   # server never exits; foreground run times out
```

`expectFailure` inverts the usual logic: the harness catches `AssertionError` from the run step and treats it as a pass. Use `expectFailureOutputContains` to pin the failure reason.

**Note:** Positive and negative expectations are not mixed on the same program — a program with `expectFailure: true` must not also expect successful stdout/HTTP checks to pass.

## Examples

### Terminal output only

[`cases/basic/test1.yaml`](cases/basic/test1.yaml) — compile and run until the program exits; check printed numbers in any order:

```yaml
source: regression/input/basic/test1.jul
programs:
  - name: TermTest1
    run:
      timeoutMs: 60000
      expectStdoutLinesUnordered:
        - "0"
        - "10"
```

### Stdin + stdout

[`cases/readln/kv.yaml`](cases/readln/kv.yaml) — two programs in one file, different stdin/expectations:

```yaml
source: regression/input/readln/kv.jul
programs:
  - name: KVOne
    run:
      stdin: ["A"]
      expectStdoutContains: ["apple"]
```

### HTTP server

[`cases/server/test-inc.yaml`](cases/server/test-inc.yaml):

```yaml
tags: [http]
source: regression/input/server/test-inc.jul
programs:
  - name: CounterServerTest
    run:
      durationMs: 4500
      http:
        - post: "hello"
          expectBody: "1"
        - post: "exit"
          expectBody: "4"
```

### Compile failure (negative)

[`cases/type-errors/transit-bool-to-int.yaml`](cases/type-errors/transit-bool-to-int.yaml):

```yaml
source: regression/input/type-errors/transit-bool-to-int.jul
expectCompileFailure: true
expectCompileOutputContains:
  - "Expected assignment to \"x\" (Int) but got expression of type Bool"
```

## Adding a new case

1. Add or update a program under `regression/input/` (keeping the same subdirectory layout as needed).
2. Create `regression/cases/<subdir>/<name>.yaml` pointing at that source (mirror the `input/` path).
3. For each `program` you care about, add a `run` block with stdin/HTTP/timeouts and expectations.
4. Run `./gradlew test` and fix expectations from the failure diff if needed.
5. Optionally store exact stdout in `regression/expected/` and reference it with `expectStdout`.

## Golden files

Put full expected terminal output in `regression/expected/`:

```yaml
expectStdout: regression/expected/my-program.stdout
```

Capture once by running the program locally, or copy the stdout section from a failing test diff.

## Limitations

- **Port 8000** is what the HTTP probe uses; cases must `startServer` on that port. Only one HTTP case runs at a time.
- **Compile failures** on positive cases are detected when no program JARs appear in the workspace (the compiler CLI may still exit 0 on semantic errors).
- Programs that never exit need `background: true` or an `http`/`durationMs` capture path, not foreground `timeoutMs` alone (unless using `expectFailure: true` to assert a timeout).

## Implementation reference

Parser: [`src/test/kotlin/julay/regression/RegressionCases.kt`](../src/test/kotlin/julay/regression/RegressionCases.kt)  
Runner: [`src/test/kotlin/julay/regression/RegressionRunner.kt`](../src/test/kotlin/julay/regression/RegressionRunner.kt)
