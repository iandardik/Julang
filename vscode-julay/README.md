# Julay VS Code / Cursor extension

Editor support for `.jul` files: syntax highlighting, snippets, compile/analyze tasks, go-to-definition, and **external alphabet** viewing.

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
| Snippets | Prefixes: `proc`, `procclass`, `transition`, `internal`, `provider`, `client`, `session`, `spec`, … |
| Go to definition | `F12` / Cmd-click on proc names, actions, and `import path.Name` (not jar stdlib) |
| Hover alphabet | Hover a proc/spec name → external alphabet |
| Alphabet panel | Command **Julay: Show External Alphabet** |
| CodeLens | Above `proc` / `spec` declarations |
| Tasks | **Terminal → Run Task…** → Julay: Compile / Compile entry / Analyze alphabet |

### Alphabet panel

- **Composition sync** diagram (when the proc has `||` children): immediate top-level components as nodes, with one edge per syncing pair labeled by the actions composition-hidden between them (e.g. `Z := X || Y` shows `X`—`Y`, not leaves under `X`/`Y`).
- Default list view: **external** alphabet only.
- Toggle **Show internal** to reveal two separate sections:
  1. **Source-internal** — tagged `internal` in the proc definition
  2. **Synchronized (composition-hidden)** — internalized by `||`, listing the synced peers (e.g. `S ‖ T`)

Backed by:

```bash
java -jar build/libs/julayc.jar analyze -s Name --json path/to/entry.jul
```

The JSON includes `compositionGraph` (`nodes` + `edges` with `actions`) plus `external` / `sourceInternal` / `compositionHidden`.

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
