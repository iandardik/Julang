"use strict";
const fs = require("fs");
const path = require("path");
const vscode = require("vscode");
const {
  getExtraLibraryPaths,
  resolveEntryFile,
  workspaceRoot,
} = require("./config");

const PROC_DECL =
  /^\s*(?:export\s+)?(?:proc|spec)\s+([A-Za-z_][A-Za-z0-9_]*)\b/;
const ACTION_DECL =
  /^\s*(?:internal\s+|provider\s+|client\s+|session\s+)*(?:transition|constructor)\s+([A-Za-z_][A-Za-z0-9_]*)\b/;
const IMPORT_RE =
  /^\s*import\s+([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)\s*$/;

function findDeclInText(text, uri, name) {
  const lines = text.split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    const m = PROC_DECL.exec(lines[i]);
    if (m && m[1] === name) {
      const col = lines[i].indexOf(name);
      return new vscode.Location(uri, new vscode.Position(i, Math.max(0, col)));
    }
  }
  return undefined;
}

function findActionInDocument(document, name) {
  for (let i = 0; i < document.lineCount; i++) {
    const line = document.lineAt(i).text;
    const m = ACTION_DECL.exec(line);
    if (m && m[1] === name) {
      const col = line.indexOf(name);
      return new vscode.Location(
        document.uri,
        new vscode.Position(i, Math.max(0, col)),
      );
    }
  }
  return undefined;
}

function moduleSearchDirs(document) {
  const dirs = [];
  const root = workspaceRoot();
  const entry = resolveEntryFile(document);

  function addAncestors(startFile) {
    let dir = path.dirname(startFile);
    for (let i = 0; i < 8; i++) {
      dirs.push(dir);
      if (root && path.resolve(dir) === path.resolve(root)) {
        break;
      }
      const parent = path.dirname(dir);
      if (parent === dir) {
        break;
      }
      dir = parent;
    }
  }

  addAncestors(entry);
  addAncestors(document.uri.fsPath);
  for (const p of getExtraLibraryPaths()) {
    const abs = path.isAbsolute(p) ? p : path.join(root || "", p);
    dirs.push(abs);
  }
  if (root) {
    dirs.push(root);
  }
  return [...new Set(dirs)];
}

function resolveImport(document, fullPath, word) {
  if (fullPath.startsWith("julay.")) {
    return undefined;
  }

  const parts = fullPath.split(".");
  const exportName = parts[parts.length - 1];
  const moduleParts = parts.slice(0, -1);
  if (moduleParts.length === 0) {
    return undefined;
  }

  const searchDirs = moduleSearchDirs(document);
  const relJul = path.join(...moduleParts) + ".jul";
  for (const dir of searchDirs) {
    const candidate = path.join(dir, relJul);
    if (!fs.existsSync(candidate)) {
      continue;
    }
    const text = fs.readFileSync(candidate, "utf8");
    const uri = vscode.Uri.file(candidate);
    const target =
      word === exportName || word === parts[parts.length - 1]
        ? exportName
        : word;
    const loc = findDeclInText(text, uri, target);
    if (loc) {
      return loc;
    }
    return new vscode.Location(uri, new vscode.Position(0, 0));
  }
  return undefined;
}

function findInSearchPath(document, name) {
  const dirs = moduleSearchDirs(document);
  for (const dir of dirs) {
    let entries;
    try {
      entries = fs.readdirSync(dir);
    } catch {
      continue;
    }
    for (const ent of entries) {
      if (!ent.endsWith(".jul")) {
        continue;
      }
      const filePath = path.join(dir, ent);
      try {
        const text = fs.readFileSync(filePath, "utf8");
        const loc = findDeclInText(text, vscode.Uri.file(filePath), name);
        if (loc) {
          return loc;
        }
      } catch {
        // ignore
      }
    }
  }
  return undefined;
}

class JulayDefinitionProvider {
  provideDefinition(document, position) {
    const wordRange = document.getWordRangeAtPosition(
      position,
      /[A-Za-z_][A-Za-z0-9_]*/,
    );
    if (!wordRange) {
      return undefined;
    }
    const name = document.getText(wordRange);
    const line = document.lineAt(position.line).text;

    const importMatch = IMPORT_RE.exec(line.trimEnd());
    if (importMatch) {
      return resolveImport(document, importMatch[1], name);
    }

    const sameFile = findDeclInText(document.getText(), document.uri, name);
    if (sameFile) {
      return sameFile;
    }

    const actionLoc = findActionInDocument(document, name);
    if (actionLoc) {
      return actionLoc;
    }

    return findInSearchPath(document, name);
  }
}

module.exports = { JulayDefinitionProvider };
