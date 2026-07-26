# Julay VS Code / Cursor extension

Editor support for `.jul` files: syntax highlighting, snippets, compile/analyze tasks, go-to-definition, **external alphabet** viewing, and **on-save diagnostics**.

## Prerequisites

1. Build the compiler from the repo root:

```bash
./gradlew shadowJar
# → build/libs/julayc.jar
```

2. Open this repository (or a Julay project) as the workspace so the extension can find `build/libs/julayc.jar`.

## Install (local)

### Option A — Extension Development Host

1. Open the Julang repo in VS Code or Cursor.
2. Run **Developer: Install Extension from Location…** and choose the `vscode-julay` folder  
   **or** symlink / add the folder under your extensions directory:
   - VS Code: `~/.vscode/extensions/julay-dev` → this folder
   - Cursor: `~/.cursor/extensions/julay-dev` → this folder
3. Reload the window.

### Option B — Package a `.vsix` (needs Node/`vsce`)

```bash
cd vscode-julay
npx @vscode/vsce package
code --install-extension julay-0.1.0.vsix
# or: cursor --install-extension julay-0.1.0.vsix
```

## Features

| Feature | How |
|---------|-----|
| Syntax highlighting | Automatic for `*.jul` |
| Snippets | Prefixes: `proc`, `procclass`, `procfun`, `transition`, `internal`, `provider`, `client`, `session`, `spec`, … |
| Go to definition | `F12` / Cmd-click on proc names, actions, and `import path.Name` (not jar stdlib) |
| Hover alphabet | Hover a proc / spec / **procfun** name → external alphabet |
| Alphabet panel | Command **Julay: Show External Alphabet** |
| CodeLens | Above `proc` / `spec` / **procfun** declarations |
| On-save diagnostics | Save a `.jul` file → Problems panel + squiggles (full error/warning messages) |
| Re-check | Command **Julay: Re-check** |
| Tasks | **Terminal → Run Task…** → Julay: Compile / Compile entry / Analyze alphabet |

### Alphabet panel

- **Composition sync** diagram (when the proc has `||` children): immediate top-level components as nodes, with one edge per syncing pair labeled by the actions composition-hidden between them (e.g. `Z := X || Y` shows `X`—`Y`, not leaves under `X`/`Y`).
- Default list view: **external** alphabet only.
- Toggle **Show internal** to reveal two separate sections:
  1. **Source-internal** — tagged `internal` in the proc definition
  2. **Synchronized (composition-hidden)** — internalized by `||`, listing the synced peers (e.g. `S ‖ T`)

**Procfun scopes:** analyzing a `procfun` alone lists its user actions under **external** (including `internal` / bare-return steps). Synthetic `F_call` / `F_ret` are hidden. **Parent procs** also show non-synthetic actions from any procfun **called** under them, even if that helper is not listed in `||` (see [Procfuns](../docs/language/procfun.md)).

Backed by:

```bash
java -jar build/libs/julayc.jar analyze -s Name --json path/to/entry.jul
```

The JSON includes `compositionGraph` (`nodes` + `edges` with `actions`) plus `external` / `sourceInternal` / `compositionHidden`.

### On-save diagnostics

On save (or **Julay: Re-check**), the extension runs:

```bash
java -jar build/libs/julayc.jar check --json path/to/saved.jul
```

Errors appear as red squiggles; soft sync-peer / unsynced-ordinary messages as yellow warnings. Hover a squiggle or open the **Problems** panel to read the full message. The saved file is the check entry (same as alphabet analyze) — use **Julay: Compile entry** for project-entry compile.

## Settings

| Setting | Meaning |
|---------|---------|
| `julay.julaycPath` | Path to `julayc.jar` (default: search workspace) |
| `julay.javaPath` | Java executable (default `java`) |
| `julay.entryFile` | Entry `.jul` when analyzing from a non-entry module |
| `julay.extraLibraryPaths` | Extra `-L` module search dirs |

## See also

- [Tooling](../docs/tooling.md) — `julayc` CLI including `--json`
- [Composition and actions](../docs/language/composition-and-actions.md) — external vs composition-hidden alphabets
- [Procfuns](../docs/language/procfun.md) — standalone vs parent alphabet folding
