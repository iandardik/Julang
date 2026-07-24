"use strict";
const vscode = require("vscode");
const {
  fetchAlphabet,
  formatExternalMarkdown,
  identifierAt,
  declaredProcNames,
} = require("./alphabet");

const KEYWORDS = new Set([
  "if", "else", "let", "when", "in", "all", "exists",
  "import", "export", "obj", "sort", "proc", "compile", "spec", "invariant",
  "var", "const", "constructor", "transition", "internal", "provider", "client",
  "session", "guard", "transit", "error", "before", "after", "fun",
  "true", "false",
]);

function looksLikeProcReference(document, position, name) {
  const line = document.lineAt(position.line).text;
  if (/\b(proc|spec)\s+\w/.test(line) && line.includes(name)) {
    return true;
  }
  if (line.includes("||") || /:=/.test(line)) {
    return true;
  }
  return false;
}

class JulayHoverProvider {
  async provideHover(document, position) {
    const name = identifierAt(document, position);
    if (!name || KEYWORDS.has(name)) {
      return undefined;
    }

    const declared = new Set(declaredProcNames(document));
    if (!declared.has(name) && !looksLikeProcReference(document, position, name)) {
      return undefined;
    }

    const result = await fetchAlphabet(document, name);
    if (!result.ok) {
      if (!looksLikeProcReference(document, position, name) && !declared.has(name)) {
        return undefined;
      }
      return new vscode.Hover(
        new vscode.MarkdownString(
          `**Julay**\n\nCould not analyze \`${name}\`:\n\n\`\`\`\n${result.message}\n\`\`\``,
        ),
      );
    }

    const scope =
      result.data.scopes.find((s) => s.name === name) || result.data.scopes[0];
    if (!scope) {
      return undefined;
    }

    const md = new vscode.MarkdownString(formatExternalMarkdown(scope));
    md.appendMarkdown(
      `\n\n_Use **Julay: Show External Alphabet** for source-internal and sync-hidden details._`,
    );
    return new vscode.Hover(md);
  }
}

module.exports = { JulayHoverProvider };
