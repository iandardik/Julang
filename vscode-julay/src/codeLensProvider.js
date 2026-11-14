"use strict";
const vscode = require("vscode");
const { fetchAlphabet } = require("./alphabet");

const PROC_DECL =
  /^\s*(?:export\s+)?(?:static\s+)?(proc|spec|procfun|api)\s+([A-Za-z_][A-Za-z0-9_]*)\b/;

class JulayCodeLensProvider {
  constructor() {
    this._onDidChange = new vscode.EventEmitter();
    this.onDidChangeCodeLenses = this._onDidChange.event;
  }

  refresh() {
    this._onDidChange.fire();
  }

  provideCodeLenses(document) {
    const lenses = [];
    for (let i = 0; i < document.lineCount; i++) {
      const line = document.lineAt(i);
      const m = PROC_DECL.exec(line.text);
      if (!m) {
        continue;
      }
      const name = m[2];
      const range = new vscode.Range(i, 0, i, line.text.length);
      const lens = new vscode.CodeLens(range);
      lens._julayScope = name;
      lenses.push(lens);
    }
    return lenses;
  }

  async resolveCodeLens(codeLens) {
    const scope = codeLens._julayScope || "";
    if (!scope) {
      return codeLens;
    }
    const editor = vscode.window.activeTextEditor;
    const doc = editor?.document;
    if (!doc || doc.languageId !== "julay") {
      codeLens.command = {
        title: `External alphabet of ${scope}`,
        command: "julay.showExternalAlphabet",
        arguments: [{ scope }],
      };
      return codeLens;
    }
    const result = await fetchAlphabet(doc, scope);
    if (!result.ok) {
      codeLens.command = {
        title: `External alphabet of ${scope} (error)`,
        command: "julay.showExternalAlphabet",
        arguments: [{ scope }],
      };
      return codeLens;
    }
    const alphabetScope =
      result.data.scopes.find((s) => s.name === scope) || result.data.scopes[0];
    const external = alphabetScope?.external || [];
    const n = external.length;
    const hasService = external.some((o) => o.modifier === "service");
    const hasClient = external.some((o) => o.modifier === "client");
    const hidden = alphabetScope?.compositionHidden || [];
    const hiddenSvc = hidden.some(
      (g) =>
        g.role === "service/client" ||
        (g.offers || []).some((o) => o.modifier === "service" || o.modifier === "client"),
    );
    let title = `External alphabet (${n} action${n === 1 ? "" : "s"})`;
    if (hasService || hasClient) {
      const bits = [];
      if (hasService) bits.push("service");
      if (hasClient) bits.push("client");
      title += ` · ${bits.join("/")}`;
    } else if (hiddenSvc) {
      title += " · service/client synced";
    } else if (hidden.length > 0) {
      title += ` · ${hidden.length} scoped`;
    }
    codeLens.command = {
      title,
      command: "julay.showExternalAlphabet",
      arguments: [{ scope }],
    };
    return codeLens;
  }
}

module.exports = { JulayCodeLensProvider };
