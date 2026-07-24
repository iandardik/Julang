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
let showLeafProcs = false;

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
      } else if (msg?.type === "toggleLeafProcs") {
        showLeafProcs = !!msg.value;
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

  panel.webview.html = alphabetHtml(
    alphabetScope,
    result.entryFile,
    showInternal,
    showLeafProcs,
  );
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

function alphabetHtml(scope, entryFile, internal, leafProcs) {
  const externalSection = renderOffers(
    scope.external || [],
    "No external actions.",
    leafProcs,
  );
  const sourceInternalSection = renderOffers(
    scope.sourceInternal || [],
    "No source-internal actions.",
    leafProcs,
  );
  const syncSection = renderSyncGroups(scope.compositionHidden || []);
  const graphSection = renderCompositionGraph(scope.compositionGraph);

  return wrapHtml(`
    <div class="toolbar">
      <label><input type="checkbox" id="internal" ${internal ? "checked" : ""}/> Show internal</label>
      <label><input type="checkbox" id="leafProcs" ${leafProcs ? "checked" : ""}/> Show leaf procs</label>
      <button id="refresh">Refresh</button>
    </div>
    <h1>Alphabet of <code>${esc(scope.name)}</code></h1>
    <p class="meta">Entry: <code>${esc(entryFile)}</code></p>

    ${graphSection}

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

/**
 * Horizontal node boxes with one labeled connector per sync edge.
 * Attachment points are offset toward each neighbor so adjacent edges
 * (A—B and B—C) do not share a vertical stem at B.
 */
function renderCompositionGraph(graph) {
  if (!graph || !Array.isArray(graph.nodes) || graph.nodes.length < 2) {
    return "";
  }
  const nodes = graph.nodes;
  const edges = Array.isArray(graph.edges) ? graph.edges : [];
  const n = nodes.length;
  const minBoxW = 72;
  const maxBoxW = 148;
  const boxPad = 16;
  // Uniform box width: wide enough for the longest name, but capped so neighbors stay separated.
  const boxW = Math.min(
    maxBoxW,
    Math.max(minBoxW, ...nodes.map((name) => estimateTextWidth(name) + boxPad)),
  );
  const displayNames = nodes.map((name) => ellipsizeToWidth(name, boxW - boxPad));
  const boxH = 36;
  const gap = 40;
  const padX = 20;
  const padTop = 24;
  const width = padX * 2 + n * boxW + (n - 1) * gap;

  const nodeX = (i) => padX + i * (boxW + gap);
  const nodeCenter = (i) => nodeX(i) + boxW / 2;
  const indexOf = (name) => nodes.indexOf(name);

  const normalized = edges
    .map((edge) => {
      const ia = indexOf(edge.a);
      const ib = indexOf(edge.b);
      if (ia < 0 || ib < 0) {
        return null;
      }
      const left = Math.min(ia, ib);
      const right = Math.max(ia, ib);
      return {
        left,
        right,
        aName: nodes[left],
        bName: nodes[right],
        actions: edge.actions || [],
        span: right - left,
      };
    })
    .filter(Boolean)
    .sort((e1, e2) => e1.span - e2.span || e1.left - e2.left || e1.right - e2.right);

  const portsLeft = nodes.map(() => []);
  const portsRight = nodes.map(() => []);
  normalized.forEach((edge, ei) => {
    portsRight[edge.left].push(ei);
    portsLeft[edge.right].push(ei);
  });

  function attachX(nodeIndex, edgeIndex, side) {
    const slots = side === "right" ? portsRight[nodeIndex] : portsLeft[nodeIndex];
    const slot = Math.max(0, slots.indexOf(edgeIndex));
    const count = Math.max(1, slots.length);
    const fan = (slot - (count - 1) / 2) * 10;
    const toward = side === "right" ? 1 : -1;
    return nodeCenter(nodeIndex) + toward * (boxW * 0.28) + fan;
  }

  const nodeRects = nodes
    .map((name, i) => {
      const x = nodeX(i);
      const y = padTop;
      const label = displayNames[i];
      return `
        <g>
          <title>${esc(name)}</title>
          <rect x="${x}" y="${y}" width="${boxW}" height="${boxH}" rx="6" class="node-box"/>
          <text x="${nodeCenter(i)}" y="${y + boxH / 2 + 4}" text-anchor="middle" class="node-label">${esc(label)}</text>
        </g>`;
    })
    .join("\n");

  const yBox = padTop + boxH;
  const lineH = 14;
  const boxPadX = 8;
  const boxPadY = 6;
  const laneGap = 10;
  const intervalPad = 6;

  // Pass 1: geometry for every edge (no Y yet).
  const layouts = normalized.map((edge, ei) => {
    const x1 = attachX(edge.left, ei, "right");
    const x2 = attachX(edge.right, ei, "left");
    const mid = (x1 + x2) / 2;
    const spanW = Math.abs(x2 - x1);
    const maxLabelW = Math.min(Math.max(spanW - 8, 150), width - 32);
    const lines = wrapActionLines(edge.actions, maxLabelW - 2 * boxPadX);
    const textBlockH = Math.max(1, lines.length) * lineH;
    const labelH = textBlockH + 2 * boxPadY;
    const labelW = Math.min(
      maxLabelW,
      Math.max(56, ...lines.map((ln) => estimateTextWidth(ln) + 2 * boxPadX)),
    );
    const labelX = mid - labelW / 2;
    return {
      x1,
      x2,
      mid,
      lines,
      labelH,
      labelW,
      labelX,
      xMin: labelX - intervalPad,
      xMax: labelX + labelW + intervalPad,
    };
  });

  // Pass 2: first-fit pack onto shared horizontal lanes when intervals don't overlap.
  const lanes = []; // { members: number[], maxH: number }
  layouts.forEach((layout, i) => {
    let placed = false;
    for (let li = 0; li < lanes.length; li++) {
      const lane = lanes[li];
      const overlaps = lane.members.some((j) => {
        const other = layouts[j];
        return layout.xMin < other.xMax && layout.xMax > other.xMin;
      });
      if (!overlaps) {
        lane.members.push(i);
        lane.maxH = Math.max(lane.maxH, layout.labelH);
        layout.lane = li;
        placed = true;
        break;
      }
    }
    if (!placed) {
      layout.lane = lanes.length;
      lanes.push({ members: [i], maxH: layout.labelH });
    }
  });

  // Pass 3: assign Y from packed lane heights, then draw.
  let laneY = yBox + 14;
  const laneTops = lanes.map((lane) => {
    const y = laneY;
    laneY += lane.maxH + laneGap;
    return y;
  });

  const edgeParts = layouts.map((layout) => {
    const labelY = laneTops[layout.lane];
    const railY = labelY + layout.labelH / 2;
    const textEls = layout.lines
      .map((ln, li) => {
        const ty = labelY + boxPadY + lineH * (li + 0.72);
        return `<text x="${layout.mid}" y="${ty}" text-anchor="middle" class="edge-label">${esc(ln)}</text>`;
      })
      .join("\n");
    return `
      <path d="M ${layout.x1} ${yBox} V ${railY} H ${layout.labelX}" class="edge-line" fill="none"/>
      <path d="M ${layout.labelX + layout.labelW} ${railY} H ${layout.x2} V ${yBox}" class="edge-line" fill="none"/>
      <rect x="${layout.labelX}" y="${labelY}" width="${layout.labelW}" height="${layout.labelH}" rx="4" class="edge-action-box"/>
      ${textEls}`;
  });

  const height = Math.max(padTop + boxH + 40, laneY + 8);

  const edgeList =
    normalized.length === 0
      ? `<p class="empty">No composition-hidden syncs among top-level children.</p>`
      : `<ul class="edge-list">${normalized
          .map((e) => {
            const acts = e.actions.join(", ");
            return `<li><code>${esc(e.aName)}</code> ― <code>${esc(e.bName)}</code>: ${esc(acts)}</li>`;
          })
          .join("")}</ul>`;

  return `
    <h2>Composition sync</h2>
    <p class="hint">Immediate children and actions internalized by <code>||</code> between them. Hover a node for its full name.</p>
    <div class="diagram-wrap">
      <svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Composition sync diagram">
        ${edgeParts.join("\n")}
        ${nodeRects}
      </svg>
    </div>
    ${edgeList}
  `;
}

function estimateTextWidth(text) {
  return Math.ceil(String(text).length * 6.4);
}

/** Truncate with ellipsis so the label fits inside maxPx. */
function ellipsizeToWidth(text, maxPx) {
  const s = String(text);
  if (estimateTextWidth(s) <= maxPx) {
    return s;
  }
  const maxChars = Math.max(1, Math.floor(maxPx / 6.4) - 1);
  return `${s.slice(0, maxChars)}…`;
}

/**
 * Wrap action names onto lines that fit maxWidthPx.
 * Prefers breaks after commas; ellipsizes if still too long (max 4 lines).
 */
function wrapActionLines(actions, maxWidthPx) {
  const maxChars = Math.max(10, Math.floor(maxWidthPx / 6.4));
  const maxLines = 4;
  if (!actions.length) {
    return ["—"];
  }

  const lines = [];
  let current = "";
  for (const action of actions) {
    const piece = String(action);
    const next = current ? `${current}, ${piece}` : piece;
    if (next.length <= maxChars) {
      current = next;
      continue;
    }
    if (current) {
      lines.push(current);
      current = "";
    }
    if (piece.length <= maxChars) {
      current = piece;
    } else {
      lines.push(`${piece.slice(0, Math.max(1, maxChars - 1))}…`);
    }
  }
  if (current) {
    lines.push(current);
  }

  if (lines.length <= maxLines) {
    return lines;
  }
  const kept = lines.slice(0, maxLines - 1);
  const rest = lines.slice(maxLines - 1).join(", ");
  kept.push(rest.length <= maxChars ? rest : `${rest.slice(0, Math.max(1, maxChars - 1))}…`);
  return kept;
}

function renderOffers(offers, empty, showLeaf) {
  if (!offers.length) {
    return `<p class="empty">${esc(empty)}</p>`;
  }
  const items = offers
    .map((o) => {
      const role = o.isConstructor ? "constructor" : o.modifier;
      const leaf = showLeaf
        ? ` (<code>${esc(o.pclassKey)}</code>)`
        : "";
      return `<li><code>${esc(formatOfferSignature(o))}</code> — ${esc(role)}${leaf}</li>`;
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
  .diagram-wrap { overflow-x: auto; margin: 0.75rem 0; }
  .node-box {
    fill: var(--vscode-editor-background, #1e1e1e);
    stroke: var(--vscode-focusBorder, #007acc);
    stroke-width: 1.5;
  }
  .node-label { fill: var(--vscode-foreground); font-family: var(--vscode-editor-font-family); font-size: 12px; }
  .edge-line { stroke: var(--vscode-descriptionForeground, #888); stroke-width: 1.25; }
  .edge-action-box {
    fill: var(--vscode-badge-background, #4d4d4d);
    stroke: var(--vscode-badge-background, #4d4d4d);
    stroke-width: 1;
  }
  .edge-label {
    fill: var(--vscode-badge-foreground, #ffffff);
    font-family: var(--vscode-editor-font-family);
    font-size: 11px;
  }
  .edge-list { margin-top: 0.25rem; }
</style>
</head>
<body>
${body}
<script>
  const vscode = acquireVsCodeApi();
  const box = document.getElementById('internal');
  if (box) box.addEventListener('change', () => vscode.postMessage({ type: 'toggleInternal', value: box.checked }));
  const leaf = document.getElementById('leafProcs');
  if (leaf) leaf.addEventListener('change', () => vscode.postMessage({ type: 'toggleLeafProcs', value: leaf.checked }));
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
