"use strict";
const { spawn } = require("child_process");
const path = require("path");
const vscode = require("vscode");
const {
  getJavaPath,
  libraryPathArgs,
  resolveEntryFile,
  resolveJulaycJar,
} = require("./config");

let collection;
let checkGeneration = 0;
/** @type {Set<string>} */
let lastDiagnosticUris = new Set();

function getCollection() {
  if (!collection) {
    collection = vscode.languages.createDiagnosticCollection("julay");
  }
  return collection;
}

function runProcess(command, args) {
  return new Promise((resolve) => {
    const child = spawn(command, args, { shell: false });
    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (d) => {
      stdout += d.toString();
    });
    child.stderr.on("data", (d) => {
      stderr += d.toString();
    });
    child.on("error", (err) => {
      resolve({ stdout, stderr: err.message, code: 1 });
    });
    child.on("close", (code) => {
      resolve({ stdout, stderr, code });
    });
  });
}

function extractJson(stdout) {
  const start = stdout.indexOf("{");
  if (start < 0) {
    return undefined;
  }
  return stdout.slice(start).trim();
}

function lineRange(startLine, endLine) {
  const start = Math.max(0, (startLine || 1) - 1);
  const end = Math.max(start, (endLine || startLine || 1) - 1);
  return new vscode.Range(start, 0, end, Number.MAX_SAFE_INTEGER);
}

function toUri(filePath) {
  if (!filePath) {
    return undefined;
  }
  return vscode.Uri.file(path.resolve(filePath));
}

function mapDiagnostic(d) {
  const severity =
    d.severity === "warning"
      ? vscode.DiagnosticSeverity.Warning
      : vscode.DiagnosticSeverity.Error;
  const diag = new vscode.Diagnostic(
    lineRange(d.startLine, d.endLine),
    d.message || "Julay diagnostic",
    severity,
  );
  diag.source = "julay";
  if (Array.isArray(d.related) && d.related.length > 0) {
    diag.relatedInformation = d.related
      .map((r) => {
        const uri = toUri(r.file);
        if (!uri) {
          return undefined;
        }
        return new vscode.DiagnosticRelatedInformation(
          new vscode.Location(uri, lineRange(r.startLine, r.endLine)),
          r.message || "related location",
        );
      })
      .filter(Boolean);
  }
  return diag;
}

function applyDiagnostics(entryFile, diagnostics) {
  const coll = getCollection();
  const byUri = new Map();

  for (const d of diagnostics) {
    const uri = toUri(d.file) || toUri(entryFile);
    if (!uri) {
      continue;
    }
    const key = uri.toString();
    if (!byUri.has(key)) {
      byUri.set(key, { uri, items: [] });
    }
    byUri.get(key).items.push(mapDiagnostic(d));
  }

  for (const key of lastDiagnosticUris) {
    if (!byUri.has(key)) {
      coll.delete(vscode.Uri.parse(key));
    }
  }

  const next = new Set();
  for (const { uri, items } of byUri.values()) {
    coll.set(uri, items);
    next.add(uri.toString());
  }
  lastDiagnosticUris = next;

  if (byUri.size === 0) {
    coll.clear();
    lastDiagnosticUris = new Set();
  }
}

async function runCheckOnDocument(document) {
  if (!document || document.languageId !== "julay") {
    return;
  }

  const jar = resolveJulaycJar();
  const coll = getCollection();
  if (!jar) {
    const uri = document.uri;
    coll.set(uri, [
      new vscode.Diagnostic(
        new vscode.Range(0, 0, 0, Number.MAX_SAFE_INTEGER),
        "julayc.jar not found. Build with `./gradlew shadowJar` or set `julay.julaycPath`.",
        vscode.DiagnosticSeverity.Error,
      ),
    ]);
    return;
  }

  // Prefer configured / nearby entry (main.jul or julay.entryFile) so checking a
  // leaf module still sees the full composition, matching alphabet analyze.
  const entryFile = resolveEntryFile(document);
  const generation = ++checkGeneration;
  const args = [
    "-jar",
    jar,
    "check",
    "--json",
    ...libraryPathArgs(entryFile),
    entryFile,
  ];

  const { stdout, stderr, code } = await runProcess(getJavaPath(), args);
  if (generation !== checkGeneration) {
    return;
  }

  const jsonText = extractJson(stdout);
  if (!jsonText) {
    const message = (
      stderr ||
      stdout ||
      `julayc check failed (exit ${code})`
    ).trim();
    coll.set(document.uri, [
      new vscode.Diagnostic(
        new vscode.Range(0, 0, 0, Number.MAX_SAFE_INTEGER),
        message || "julayc check produced no JSON",
        vscode.DiagnosticSeverity.Error,
      ),
    ]);
    return;
  }

  let data;
  try {
    data = JSON.parse(jsonText);
  } catch (e) {
    coll.set(document.uri, [
      new vscode.Diagnostic(
        new vscode.Range(0, 0, 0, Number.MAX_SAFE_INTEGER),
        `Failed to parse julayc check JSON: ${e}`,
        vscode.DiagnosticSeverity.Error,
      ),
    ]);
    return;
  }

  applyDiagnostics(entryFile, data.diagnostics || []);
}

async function recheckActiveEditor() {
  const editor = vscode.window.activeTextEditor;
  if (!editor || editor.document.languageId !== "julay") {
    vscode.window.showInformationMessage("Open a .jul file to re-check.");
    return;
  }
  await runCheckOnDocument(editor.document);
  vscode.window.setStatusBarMessage("Julay: check complete", 2000);
}

function registerDiagnostics(context) {
  const coll = getCollection();
  context.subscriptions.push(coll);
  context.subscriptions.push(
    vscode.commands.registerCommand("julay.recheck", () => recheckActiveEditor()),
  );
}

module.exports = {
  registerDiagnostics,
  runCheckOnDocument,
  recheckActiveEditor,
  getCollection,
};
