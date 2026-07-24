"use strict";
const vscode = require("vscode");
const { fetchAlphabet } = require("./alphabet");

const PROC_DECL =
  /^\s*(?:export\s+)?(proc|spec)\s+([A-Za-z_][A-Za-z0-9_]*)\b/;

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
    const n = alphabetScope?.external?.length ?? 0;
    codeLens.command = {
      title: `External alphabet (${n} action${n === 1 ? "" : "s"})`,
      command: "julay.showExternalAlphabet",
      arguments: [{ scope }],
    };
    return codeLens;
  }
}

module.exports = { JulayCodeLensProvider };
