"use strict";
const fs = require("fs");
const path = require("path");
const vscode = require("vscode");

function getJavaPath() {
  return vscode.workspace.getConfiguration("julay").get("javaPath") || "java";
}

function getConfiguredJulaycPath() {
  return vscode.workspace.getConfiguration("julay").get("julaycPath") || "";
}

function getConfiguredEntryFile() {
  return vscode.workspace.getConfiguration("julay").get("entryFile") || "";
}

function getExtraLibraryPaths() {
  return vscode.workspace.getConfiguration("julay").get("extraLibraryPaths") || [];
}

function workspaceRoot() {
  return vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
}

function resolveJulaycJar() {
  const configured = getConfiguredJulaycPath().trim();
  if (configured) {
    const abs = path.isAbsolute(configured)
      ? configured
      : path.join(workspaceRoot() || "", configured);
    if (fs.existsSync(abs)) {
      return abs;
    }
  }
  const root = workspaceRoot();
  if (!root) {
    return undefined;
  }
  const candidates = [
    path.join(root, "build", "libs", "julayc.jar"),
    path.join(root, "julayc.jar"),
  ];
  return candidates.find((p) => fs.existsSync(p));
}

function documentHasCompile(filePath) {
  try {
    const text = fs.readFileSync(filePath, "utf8");
    return /^\s*compile\b/m.test(text);
  } catch {
    return false;
  }
}

function resolveEntryFile(document) {
  const root = workspaceRoot();
  const configured = getConfiguredEntryFile().trim();
  if (configured && root) {
    const abs = path.isAbsolute(configured) ? configured : path.join(root, configured);
    if (fs.existsSync(abs)) {
      return abs;
    }
  }

  const current = document.uri.fsPath;
  if (documentHasCompile(current)) {
    return current;
  }

  const dir = path.dirname(current);
  const mainJul = path.join(dir, "main.jul");
  if (fs.existsSync(mainJul)) {
    return mainJul;
  }

  let walk = dir;
  for (let i = 0; i < 4; i++) {
    const candidate = path.join(walk, "main.jul");
    if (fs.existsSync(candidate)) {
      return candidate;
    }
    const parent = path.dirname(walk);
    if (parent === walk) {
      break;
    }
    walk = parent;
  }

  return current;
}

function libraryPathArgs() {
  const root = workspaceRoot();
  const args = [];
  for (const p of getExtraLibraryPaths()) {
    const abs = path.isAbsolute(p) ? p : path.join(root || "", p);
    if (fs.existsSync(abs)) {
      args.push("-L", abs);
    }
  }
  return args;
}

module.exports = {
  getJavaPath,
  getConfiguredJulaycPath,
  getConfiguredEntryFile,
  getExtraLibraryPaths,
  workspaceRoot,
  resolveJulaycJar,
  resolveEntryFile,
  libraryPathArgs,
};
