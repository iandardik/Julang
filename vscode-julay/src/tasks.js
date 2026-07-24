"use strict";
const { spawn } = require("child_process");
const path = require("path");
const vscode = require("vscode");
const {
  getConfiguredEntryFile,
  getJavaPath,
  libraryPathArgs,
  resolveEntryFile,
  resolveJulaycJar,
  workspaceRoot,
} = require("./config");

class JulayTaskProvider {
  static get type() {
    return "julay";
  }

  provideTasks() {
    return [
      this.makeTask("compile", "Julay: Compile current file", "compile"),
      this.makeTask("compileEntry", "Julay: Compile entry file", "compileEntry"),
      this.makeTask(
        "analyzeAlphabet",
        "Julay: Analyze alphabet (prompt for scope)",
        "analyzeAlphabet",
      ),
    ];
  }

  resolveTask(task) {
    const def = task.definition;
    if (def.type !== JulayTaskProvider.type) {
      return undefined;
    }
    return this.makeTask(def.task, task.name, def.task, def.scope);
  }

  makeTask(_id, name, kind, scope) {
    const definition = {
      type: JulayTaskProvider.type,
      task: kind,
      scope,
    };
    const execution = new vscode.CustomExecution(async () => {
      return new JulayTaskTerminal(kind, scope);
    });
    const task = new vscode.Task(
      definition,
      vscode.TaskScope.Workspace,
      name,
      "julay",
      execution,
    );
    task.group =
      kind === "compile" || kind === "compileEntry"
        ? vscode.TaskGroup.Build
        : undefined;
    task.presentationOptions = {
      reveal: vscode.TaskRevealKind.Always,
      panel: vscode.TaskPanelKind.Shared,
    };
    return task;
  }
}

class JulayTaskTerminal {
  constructor(kind, scope) {
    this.kind = kind;
    this.scope = scope;
    this.writeEmitter = new vscode.EventEmitter();
    this.closeEmitter = new vscode.EventEmitter();
    this.onDidWrite = this.writeEmitter.event;
    this.onDidClose = this.closeEmitter.event;
  }

  open() {
    void this.run();
  }

  close() {}

  async run() {
    const jar = resolveJulaycJar();
    if (!jar) {
      this.write(
        "julayc.jar not found. Build with ./gradlew shadowJar or set julay.julaycPath.\r\n",
      );
      this.closeEmitter.fire(1);
      return;
    }

    const java = getJavaPath();
    const editor = vscode.window.activeTextEditor;
    let targetFile;

    if (this.kind === "compile") {
      if (!editor || editor.document.languageId !== "julay") {
        this.write("Open a .jul file to compile.\r\n");
        this.closeEmitter.fire(1);
        return;
      }
      targetFile = editor.document.uri.fsPath;
    } else if (this.kind === "compileEntry") {
      if (editor && editor.document.languageId === "julay") {
        targetFile = resolveEntryFile(editor.document);
      } else {
        const configured = getConfiguredEntryFile();
        const root = workspaceRoot();
        if (configured && root) {
          targetFile = path.isAbsolute(configured)
            ? configured
            : path.join(root, configured);
        }
      }
      if (!targetFile) {
        this.write("Set julay.entryFile or open a .jul file.\r\n");
        this.closeEmitter.fire(1);
        return;
      }
    } else {
      if (!editor || editor.document.languageId !== "julay") {
        this.write("Open a .jul file to analyze.\r\n");
        this.closeEmitter.fire(1);
        return;
      }
      targetFile = resolveEntryFile(editor.document);
      let scope = this.scope;
      if (!scope) {
        scope = await vscode.window.showInputBox({
          prompt: "Proc/spec name (-s)",
          placeHolder: "TermTest1",
        });
      }
      if (!scope) {
        this.closeEmitter.fire(1);
        return;
      }
      const args = [
        "-jar",
        jar,
        "analyze",
        "-s",
        scope,
        "--json",
        ...libraryPathArgs(),
        targetFile,
      ];
      await this.exec(java, args);
      return;
    }

    const args = ["-jar", jar, ...libraryPathArgs(), targetFile];
    await this.exec(java, args);
  }

  exec(command, args) {
    return new Promise((resolve) => {
      this.write(`> ${command} ${args.join(" ")}\r\n`);
      const child = spawn(command, args, { shell: false });
      child.stdout.on("data", (d) =>
        this.write(d.toString().replace(/\n/g, "\r\n")),
      );
      child.stderr.on("data", (d) =>
        this.write(d.toString().replace(/\n/g, "\r\n")),
      );
      child.on("close", (code) => {
        this.closeEmitter.fire(code ?? 0);
        resolve();
      });
      child.on("error", (err) => {
        this.write(err.message + "\r\n");
        this.closeEmitter.fire(1);
        resolve();
      });
    });
  }

  write(msg) {
    this.writeEmitter.fire(msg);
  }
}

module.exports = { JulayTaskProvider };
