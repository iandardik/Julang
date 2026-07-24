"use strict";
const vscode = require("vscode");
const {
  declaredProcNames,
  fetchAlphabet,
  formatOfferSignature,
  identifierAt,
  clearAlphabetCache,
} = require("./alphabet");

let panel;
let lastScope;
let lastDocument;
let showInternal = false;

async function showExternalAlphabet(args) {
  const editor = vscode.window.activeTextEditor;
  let document = editor?.document;
  if (args?.documentUri) {
    const uri = vscode.Uri.parse(args.documentUri);
    document = await vscode.workspace.openTextDocument(uri);
  }
  if (!document || document.languageId !== "julay") {
    vscode.window.showErrorMessage("Open a .jul file to show its alphabet.");
    return;
  }

  let scope = args?.scope;
  if (!scope && editor) {
    scope = identifierAt(document, editor.selection.active);
  }
  if (!scope) {
    const names = declaredProcNames(document);
    if (names.length === 0) {
      vscode.window.showErrorMessage("No proc/spec declarations found in this file.");
      return;
    }
    scope = await vscode.window.showQuickPick(names, {
      placeHolder: "Select a proc or spec",
    });
  }
  if (!scope) {
    return;
  }

  lastScope = scope;
  lastDocument = document;
  await renderPanel(document, scope);
}

async function renderPanel(document, scope) {
  if (!panel) {
    panel = vscode.window.createWebviewPanel(
      "julayAlphabet",
      `Alphabet: ${scope}`,
      vscode.ViewColumn.Beside,
      { enableScripts: true, retainContextWhenHidden: true },
    );
    panel.onDidDispose(() => {
      panel = undefined;
    });
    panel.webview.onDidReceiveMessage(async (msg) => {
      if (msg?.type === "toggleInternal") {
        showInternal = !!msg.value;
        if (lastDocument && lastScope) {
          await renderPanel(lastDocument, lastScope);
        }
      } else if (msg?.type === "refresh") {
        if (lastDocument && lastScope) {
          clearAlphabetCache();
          await renderPanel(lastDocument, lastScope);
        }
      }
    });
  }

  panel.title = `Alphabet: ${scope}`;
  panel.webview.html = loadingHtml(scope);

  const result = await fetchAlphabet(document, scope);
  if (!result.ok) {
    panel.webview.html = errorHtml(scope, result.message, result.entryFile);
    return;
  }

  const alphabetScope =
    result.data.scopes.find((s) => s.name === scope) || result.data.scopes[0];
  if (!alphabetScope) {
    panel.webview.html = errorHtml(
      scope,
      "No scope data in analyze output.",
      result.entryFile,
    );
    return;
  }

  panel.webview.html = alphabetHtml(alphabetScope, result.entryFile, showInternal);
}

function loadingHtml(scope) {
  return wrapHtml(
    `<h1>External alphabet of <code>${esc(scope)}</code></h1><p>Analyzing…</p>`,
  );
}

function errorHtml(scope, message, entryFile) {
  return wrapHtml(`
    <h1>Alphabet of <code>${esc(scope)}</code></h1>
    ${entryFile ? `<p class="meta">Entry: <code>${esc(entryFile)}</code></p>` : ""}
    <pre class="error">${esc(message)}</pre>
  `);
}

function alphabetHtml(scope, entryFile, internal) {
  const externalSection = renderOffers(scope.external || [], "No external actions.");
  const sourceInternalSection = renderOffers(
    scope.sourceInternal || [],
    "No source-internal actions.",
  );
  const syncSection = renderSyncGroups(scope.compositionHidden || []);

  return wrapHtml(`
    <div class="toolbar">
      <label><input type="checkbox" id="internal" ${internal ? "checked" : ""}/> Show internal</label>
      <button id="refresh">Refresh</button>
    </div>
    <h1>Alphabet of <code>${esc(scope.name)}</code></h1>
    <p class="meta">Entry: <code>${esc(entryFile)}</code></p>

    <h2>External</h2>
    ${externalSection}

    <div id="internalSections" style="display:${internal ? "block" : "none"}">
      <h2>Source-internal</h2>
      <p class="hint">Marked <code>internal</code> in the proc definition.</p>
      ${sourceInternalSection}

      <h2>Synchronized (composition-hidden)</h2>
      <p class="hint">Internalized by <code>||</code> synchronization — not source <code>internal</code>.</p>
      ${syncSection}
    </div>
  `);
}

function renderOffers(offers, empty) {
  if (!offers.length) {
    return `<p class="empty">${esc(empty)}</p>`;
  }
  const items = offers
    .map((o) => {
      const role = o.isConstructor ? "constructor" : o.modifier;
      return `<li><code>${esc(formatOfferSignature(o))}</code> — ${esc(role)} (<code>${esc(o.pclassKey)}</code>)</li>`;
    })
    .join("\n");
  return `<ul>${items}</ul>`;
}

function renderSyncGroups(groups) {
  if (!groups.length) {
    return `<p class="empty">No composition-hidden syncs.</p>`;
  }
  const items = groups
    .map((g) => {
      const peers = (g.peers || [])
        .map((p) => {
          const label =
            p.introducingAssembly && p.introducingAssembly !== p.pclassKey
              ? `${p.pclassKey} (from ${p.introducingAssembly})`
              : p.pclassKey;
          return esc(label);
        })
        .join(" ‖ ");
      const args = g.args && g.args.length ? `(${g.args.join(", ")})` : "()";
      const sample = (g.offers || [])[0];
      const role = sample?.isConstructor
        ? "constructor"
        : sample?.isSession
          ? "session"
          : sample?.modifier || "ordinary";
      return `<li><code>${esc(g.name)}${esc(args)}</code> — ${esc(role)} — synchronized: ${peers}</li>`;
    })
    .join("\n");
  return `<ul>${items}</ul>`;
}

function wrapHtml(body) {
  return `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8"/>
<style>
  body { font-family: var(--vscode-font-family); color: var(--vscode-foreground); padding: 1rem 1.25rem; }
  h1 { font-size: 1.25rem; }
  h2 { font-size: 1.05rem; margin-top: 1.5rem; border-bottom: 1px solid var(--vscode-widget-border, #444); padding-bottom: 0.25rem; }
  code { font-family: var(--vscode-editor-font-family); }
  .meta, .hint, .empty { opacity: 0.8; font-size: 0.9rem; }
  .error { background: var(--vscode-inputValidation-errorBackground, #400); padding: 0.75rem; white-space: pre-wrap; }
  .toolbar { display: flex; gap: 1rem; align-items: center; margin-bottom: 1rem; }
  button { cursor: pointer; }
  ul { padding-left: 1.25rem; }
  li { margin: 0.35rem 0; }
</style>
</head>
<body>
${body}
<script>
  const vscode = acquireVsCodeApi();
  const box = document.getElementById('internal');
  if (box) box.addEventListener('change', () => vscode.postMessage({ type: 'toggleInternal', value: box.checked }));
  const refresh = document.getElementById('refresh');
  if (refresh) refresh.addEventListener('click', () => vscode.postMessage({ type: 'refresh' }));
</script>
</body>
</html>`;
}

function esc(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

module.exports = { showExternalAlphabet };
