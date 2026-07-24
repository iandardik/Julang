"use strict";
const { spawn } = require("child_process");
const fs = require("fs");
const {
  getJavaPath,
  libraryPathArgs,
  resolveEntryFile,
  resolveJulaycJar,
} = require("./config");

const cache = new Map();

function clearAlphabetCache() {
  cache.clear();
}

function cacheKey(entryFile, scopeName) {
  return `${entryFile}::${scopeName}`;
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

async function fetchAlphabet(document, scopeName) {
  const jar = resolveJulaycJar();
  if (!jar) {
    return {
      ok: false,
      message:
        "julayc.jar not found. Build with `./gradlew shadowJar` or set `julay.julaycPath`.",
    };
  }

  const entryFile = resolveEntryFile(document);
  let mtimeMs = 0;
  try {
    mtimeMs = fs.statSync(entryFile).mtimeMs;
  } catch {
    return { ok: false, message: `Cannot read entry file: ${entryFile}` };
  }

  const key = cacheKey(entryFile, scopeName);
  const hit = cache.get(key);
  if (hit && hit.mtimeMs === mtimeMs) {
    return { ok: true, data: hit.data, entryFile };
  }

  const args = [
    "-jar",
    jar,
    "analyze",
    "-s",
    scopeName,
    "--json",
    ...libraryPathArgs(),
    entryFile,
  ];

  const { stdout, stderr, code } = await runProcess(getJavaPath(), args);
  if (code !== 0) {
    return {
      ok: false,
      message: (stderr || stdout || `julayc exited with ${code}`).trim(),
      entryFile,
    };
  }

  const jsonText = extractJson(stdout);
  if (!jsonText) {
    return {
      ok: false,
      message: `No JSON in julayc output:\n${stdout || stderr}`.trim(),
      entryFile,
    };
  }

  try {
    const data = JSON.parse(jsonText);
    cache.set(key, { mtimeMs, json: jsonText, data });
    return { ok: true, data, entryFile };
  } catch (e) {
    return {
      ok: false,
      message: `Failed to parse alphabet JSON: ${e}`,
      entryFile,
    };
  }
}

function formatOfferSignature(offer) {
  const args = offer.args && offer.args.length ? `(${offer.args.join(", ")})` : "()";
  return `${offer.name}${args}`;
}

function formatExternalMarkdown(scope) {
  const lines = [`### External alphabet of \`${scope.name}\``, ""];
  if (!scope.external || scope.external.length === 0) {
    lines.push("_Empty_ (no external actions).");
    return lines.join("\n");
  }
  for (const offer of scope.external) {
    const role = offer.isConstructor ? "constructor" : offer.modifier;
    lines.push(
      `- \`${formatOfferSignature(offer)}\` — ${role} (${offer.pclassKey})`,
    );
  }
  return lines.join("\n");
}

function identifierAt(document, position) {
  const range = document.getWordRangeAtPosition(position, /[A-Za-z_][A-Za-z0-9_]*/);
  if (!range) {
    return undefined;
  }
  return document.getText(range);
}

function declaredProcNames(document) {
  const text = document.getText();
  const names = new Set();
  const re = /^\s*(?:export\s+)?(?:proc|spec)\s+([A-Za-z_][A-Za-z0-9_]*)/gm;
  let m;
  while ((m = re.exec(text)) !== null) {
    names.add(m[1]);
  }
  return [...names].sort();
}

module.exports = {
  clearAlphabetCache,
  fetchAlphabet,
  formatOfferSignature,
  formatExternalMarkdown,
  identifierAt,
  declaredProcNames,
};
