"use strict";
const vscode = require("vscode");
const { clearAlphabetCache } = require("./alphabet");
const { showExternalAlphabet } = require("./alphabetPanel");
const { JulayCodeLensProvider } = require("./codeLensProvider");
const { JulayDefinitionProvider } = require("./definitionProvider");
const { JulayHoverProvider } = require("./hoverProvider");
const { JulayTaskProvider } = require("./tasks");

function activate(context) {
  const codeLens = new JulayCodeLensProvider();

  context.subscriptions.push(
    vscode.languages.registerHoverProvider("julay", new JulayHoverProvider()),
    vscode.languages.registerDefinitionProvider(
      "julay",
      new JulayDefinitionProvider(),
    ),
    vscode.languages.registerCodeLensProvider("julay", codeLens),
    vscode.tasks.registerTaskProvider(
      JulayTaskProvider.type,
      new JulayTaskProvider(),
    ),
    vscode.commands.registerCommand(
      "julay.showExternalAlphabet",
      (args) => showExternalAlphabet(args),
    ),
    vscode.commands.registerCommand("julay.refreshAlphabet", () => {
      clearAlphabetCache();
      codeLens.refresh();
      vscode.window.showInformationMessage("Julay alphabet cache cleared.");
    }),
    vscode.workspace.onDidSaveTextDocument((doc) => {
      if (doc.languageId === "julay") {
        clearAlphabetCache();
        codeLens.refresh();
      }
    }),
  );
}

function deactivate() {}

module.exports = { activate, deactivate };
