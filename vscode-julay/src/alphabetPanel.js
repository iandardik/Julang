"use strict";
const vscode = require("vscode");
const {
  declaredProcNames,
  fetchAlphabet,
  groupOffersBySignatureAndRole,
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
      vscode.window.showErrorMessage("No proc/spec/procfun/api declarations found in this file.");
      return;
    }
    scope = await vscode.window.showQuickPick(names, {
      placeHolder: "Select a proc, spec, procfun, or api",
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
  const graphSection = renderCompositionGraph(scope.compositionGraph, scope.name);

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
 * Composition sync shell: graph data + mount points. Layout and node filtering
 * run in the webview so unchecking a proc reflows the diagram immediately.
 */
function renderCompositionGraph(graph, scopeName) {
  if (!graph || !Array.isArray(graph.nodes) || graph.nodes.length < 2) {
    return "";
  }
  const payload = JSON.stringify({
    scope: scopeName || "",
    nodes: graph.nodes,
    edges: Array.isArray(graph.edges) ? graph.edges : [],
  }).replace(/</g, "\\u003c");
  return `
    <h2>Composition sync</h2>
    <p class="hint">Immediate <code>||</code> children and their composition syncs; for a leaf scope, also procfuns that leaf calls. Solid lines are syncs; dotted arrows are spawn-await procfun calls (caller→callee). Uncheck a proc’s box to hide it and its edges.</p>
    <script type="application/json" id="compositionGraphData">${payload}</script>
    <div class="diagram-wrap" id="compositionSvgWrap"></div>
    <div id="compositionFilters" class="diagram-filters"></div>
    <div id="compositionEdgeList"></div>
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

/**
 * Build SVG + edge-list HTML for the visible subset of [graph].
 * Shared by the webview redraw path (inlined below) — kept here for tests/docs parity.
 */
function buildCompositionDiagramContent(nodes, edges) {
  if (!nodes || nodes.length < 2) {
    return {
      svg: `<p class="empty">Select at least two procs to show the diagram.</p>`,
      edgeList: "",
    };
  }
  const n = nodes.length;
  const minBoxW = 88;
  const maxBoxW = 160;
  const boxPad = 28;
  const boxW = Math.min(
    maxBoxW,
    Math.max(minBoxW, ...nodes.map((name) => estimateTextWidth(name) + boxPad)),
  );
  const displayNames = nodes.map((name) => ellipsizeToWidth(name, Math.max(24, boxW - boxPad - 18)));
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
      const actions = Array.isArray(edge.actions) ? edge.actions : [];
      const isCallEdge = actions.length === 0;
      return {
        left,
        right,
        aName: edge.a,
        bName: edge.b,
        callerIdx: ia,
        calleeIdx: ib,
        actions,
        isCallEdge,
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
      const checkY = y + (boxH - 14) / 2;
      return `
        <g>
          <title>${esc(name)}</title>
          <rect x="${x}" y="${y}" width="${boxW}" height="${boxH}" rx="6" class="node-box"/>
          <foreignObject x="${x + 6}" y="${checkY}" width="16" height="16">
            <div xmlns="http://www.w3.org/1999/xhtml" class="node-check-wrap">
              <input type="checkbox" class="node-check" data-node="${esc(name)}" checked/>
            </div>
          </foreignObject>
          <text x="${x + 24}" y="${y + boxH / 2 + 4}" text-anchor="start" class="node-label">${esc(label)}</text>
        </g>`;
    })
    .join("\n");

  const yBox = padTop + boxH;
  const lineH = 14;
  const boxPadX = 8;
  const boxPadY = 6;
  const laneGap = 10;
  const intervalPad = 6;

  const layouts = normalized.map((edge, ei) => {
    const x1 = attachX(edge.left, ei, "right");
    const x2 = attachX(edge.right, ei, "left");
    const mid = (x1 + x2) / 2;
    const spanW = Math.abs(x2 - x1);
    const spanMin = Math.min(x1, x2);
    const spanMax = Math.max(x1, x2);
    if (edge.isCallEdge) {
      // Caller→callee: start on the caller's port facing the callee, end on the callee.
      const callerIsLeft = edge.callerIdx === edge.left;
      const xStart = callerIsLeft ? x1 : x2;
      const xEnd = callerIsLeft ? x2 : x1;
      return {
        x1: xStart,
        x2: xEnd,
        mid,
        lines: [],
        isCallEdge: true,
        labelH: 8,
        labelW: 0,
        labelX: mid,
        // Pack against the full horizontal run so dotted rails do not share a
        // Y with a solid edge whose action bubble sits elsewhere on the span.
        xMin: spanMin - intervalPad,
        xMax: spanMax + intervalPad,
      };
    }
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
      isCallEdge: false,
      labelH,
      labelW,
      labelX,
      // Sync packing uses the full connector span as well (not just the label),
      // so solid/dotted horizontals that share a corridor get different lanes.
      xMin: Math.min(spanMin, labelX) - intervalPad,
      xMax: Math.max(spanMax, labelX + labelW) + intervalPad,
    };
  });

  // Lanes are kind-segregated: call (dotted) and sync (solid) never share a rail Y.
  const lanes = [];
  layouts.forEach((layout, i) => {
    let placed = false;
    for (let li = 0; li < lanes.length; li++) {
      const lane = lanes[li];
      if (lane.isCallEdge !== layout.isCallEdge) continue;
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
      lanes.push({
        members: [i],
        maxH: layout.labelH,
        isCallEdge: layout.isCallEdge,
      });
    }
  });

  let laneY = yBox + 14;
  const laneTops = lanes.map((lane) => {
    const y = laneY;
    laneY += lane.maxH + laneGap;
    return y;
  });

  const pathParts = [];
  const bubbleParts = [];
  layouts.forEach((layout) => {
    const labelY = laneTops[layout.lane];
    const railY = labelY + layout.labelH / 2;
    if (layout.isCallEdge) {
      pathParts.push(
        `<path d="M ${layout.x1} ${yBox} V ${railY} H ${layout.x2} V ${yBox}" class="edge-line edge-line-call" fill="none" marker-end="url(#call-arrow)"/>`,
      );
      return;
    }
    pathParts.push(
      `<path d="M ${layout.x1} ${yBox} V ${railY} H ${layout.labelX}" class="edge-line" fill="none"/>`,
      `<path d="M ${layout.labelX + layout.labelW} ${railY} H ${layout.x2} V ${yBox}" class="edge-line" fill="none"/>`,
    );
    const textEls = layout.lines
      .map((ln, li) => {
        const ty = labelY + boxPadY + lineH * (li + 0.72);
        return `<text x="${layout.mid}" y="${ty}" text-anchor="middle" class="edge-label">${esc(ln)}</text>`;
      })
      .join("\n");
    bubbleParts.push(`
      <rect x="${layout.labelX}" y="${labelY}" width="${layout.labelW}" height="${layout.labelH}" rx="4" class="edge-action-box"/>
      ${textEls}`);
  });

  const height = Math.max(padTop + boxH + 40, laneY + 8);
  const edgeParts = [...pathParts, ...bubbleParts];

  const edgeList =
    normalized.length === 0
      ? `<p class="empty">No composition-hidden syncs or procfun calls among visible procs.</p>`
      : `<ul class="edge-list">${normalized
          .map((e) => {
            if (e.isCallEdge) {
              return `<li><code>${esc(e.aName)}</code> → <code>${esc(e.bName)}</code> <span class="hint">(call)</span></li>`;
            }
            const acts = e.actions.join(", ");
            return `<li><code>${esc(e.aName)}</code> ― <code>${esc(e.bName)}</code>: ${esc(acts)}</li>`;
          })
          .join("")}</ul>`;

  return {
    svg: `<svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="Composition sync diagram">
        <defs>
          <marker id="call-arrow" viewBox="0 0 10 10" refX="7" refY="5" markerWidth="7" markerHeight="7" orient="auto">
            <path d="M 0 0 L 10 5 L 0 10 z" class="edge-call-arrow"/>
          </marker>
        </defs>
        ${edgeParts.join("\n")}
        ${nodeRects}
      </svg>`,
    edgeList,
  };
}

function renderOffers(offers, empty, showLeaf) {
  if (!offers.length) {
    return `<p class="empty">${esc(empty)}</p>`;
  }
  const items = groupOffersBySignatureAndRole(offers)
    .map(({ signature, role, offers: group }) => {
      let suffix = "";
      if (showLeaf) {
        const leaves = group.map((o) => esc(o.pclassKey)).join(", ");
        suffix = ` (${leaves})`;
      } else if (group.length > 1) {
        suffix = ` (${group.length})`;
      }
      return `<li><code>${esc(signature)}</code> — ${esc(role)}${suffix}</li>`;
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
      const role = g.role
        || (sample?.isConstructor
          ? "constructor"
          : sample?.isSession
            ? "session"
            : sample?.modifier || "ordinary");
      return `<li><code>${esc(g.name)}${esc(args)}</code> — ${esc(role)} — synchronized: ${peers}</li>`;
    })
    .join("\n");
  return `<ul>${items}</ul>`;
}

function wrapHtml(body) {
  // Geometry helpers are defined in this module; inject their source into the webview
  // so node filtering can reflow the diagram without a round-trip.
  const diagramLib = [
    estimateTextWidth,
    ellipsizeToWidth,
    wrapActionLines,
    esc,
    buildCompositionDiagramContent,
  ]
    .map((fn) => fn.toString())
    .join("\n");

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
  .diagram-filters {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem 0.75rem;
    margin: 0.35rem 0 0.5rem;
  }
  .diagram-filters:empty { display: none; margin: 0; }
  .diagram-filters .filters-label {
    opacity: 0.8;
    font-size: 0.85rem;
    width: 100%;
    margin: 0;
  }
  .diagram-filters label {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.35rem 0.65rem;
    border: 2px solid var(--vscode-panel-border, #555);
    border-radius: 6px;
    background: var(--vscode-input-background, #3c3c3c);
    cursor: pointer;
    user-select: none;
    font-family: var(--vscode-editor-font-family);
    font-size: 12px;
  }
  .node-check-wrap {
    margin: 0;
    padding: 0;
    width: 14px;
    height: 14px;
    line-height: 14px;
  }
  .node-check-wrap input {
    margin: 0;
    cursor: pointer;
    width: 14px;
    height: 14px;
  }
  .node-box {
    fill: var(--vscode-input-background, #3c3c3c);
    stroke: var(--vscode-panel-border, var(--vscode-foreground, #cccccc));
    stroke-width: 2;
  }
  .node-label { fill: var(--vscode-foreground); font-family: var(--vscode-editor-font-family); font-size: 12px; }
  .edge-line { stroke: var(--vscode-descriptionForeground, #888); stroke-width: 1.25; }
  .edge-line-call { stroke-dasharray: 4 3; }
  .edge-call-arrow { fill: var(--vscode-descriptionForeground, #888); }
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

  ${diagramLib}

  (function initCompositionDiagram() {
    const dataEl = document.getElementById('compositionGraphData');
    const filtersEl = document.getElementById('compositionFilters');
    const svgWrap = document.getElementById('compositionSvgWrap');
    const edgeListEl = document.getElementById('compositionEdgeList');
    if (!dataEl || !filtersEl || !svgWrap || !edgeListEl) return;

    let graph;
    try {
      graph = JSON.parse(dataEl.textContent);
    } catch (_) {
      return;
    }
    const allNodes = Array.isArray(graph.nodes) ? graph.nodes : [];
    const allEdges = Array.isArray(graph.edges) ? graph.edges : [];
    if (allNodes.length < 2) return;

    const state = vscode.getState() || {};
    const filterState = state.compositionNodeFilter || {};
    const scopeKey = graph.scope || "";
    const savedHidden = Array.isArray(filterState[scopeKey]) ? filterState[scopeKey] : [];
    const hidden = new Set(savedHidden.filter((n) => allNodes.includes(n)));

    function persist() {
      const next = Object.assign({}, vscode.getState() || {});
      const filters = Object.assign({}, next.compositionNodeFilter || {});
      filters[scopeKey] = Array.from(hidden);
      next.compositionNodeFilter = filters;
      vscode.setState(next);
    }

    function setHidden(name, isHidden) {
      if (isHidden) hidden.add(name);
      else hidden.delete(name);
      persist();
      redraw();
    }

    function redraw() {
      const visible = allNodes.filter((n) => !hidden.has(n));
      const visibleSet = new Set(visible);
      const edges = allEdges.filter(
        (e) => visibleSet.has(e.a) && visibleSet.has(e.b),
      );
      const built = buildCompositionDiagramContent(visible, edges);
      svgWrap.innerHTML = built.svg;
      edgeListEl.innerHTML = built.edgeList;

      const hiddenNodes = allNodes.filter((n) => hidden.has(n));
      filtersEl.innerHTML = hiddenNodes.length === 0
        ? ""
        : '<p class="filters-label">Hidden (check to show):</p>' +
          hiddenNodes
            .map((name) =>
              '<label data-node="' + esc(name) + '">' +
              '<input type="checkbox" data-node="' + esc(name) + '"/>' +
              '<span title="' + esc(name) + '">' + esc(name) + '</span></label>',
            )
            .join("");
    }

    svgWrap.addEventListener('change', (ev) => {
      const input = ev.target;
      if (!input || !input.classList || !input.classList.contains('node-check')) return;
      const name = input.getAttribute('data-node');
      if (!name) return;
      if (!input.checked) setHidden(name, true);
    });

    filtersEl.addEventListener('change', (ev) => {
      const input = ev.target;
      if (!input || input.type !== 'checkbox') return;
      const name = input.getAttribute('data-node');
      if (!name) return;
      if (input.checked) setHidden(name, false);
    });

    redraw();
  })();
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
