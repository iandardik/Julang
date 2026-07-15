#!/usr/bin/env python3
"""Sample RaftNode RSS / threads / heap / NMT into the debug NDJSON log."""

from __future__ import annotations

import json
import pathlib
import re
import subprocess
import sys
import time
import traceback

LOG_PATH = pathlib.Path("/Users/idardik/Documents/CMU/Julang/.cursor/debug-0488b3.log")
RAFT = pathlib.Path("/Users/idardik/Documents/CMU/Julang/input/raft")
JAVA = "/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home/bin/java"
SESSION = "0488b3"


def append(entry: dict) -> None:
    entry.setdefault("sessionId", SESSION)
    entry.setdefault("timestamp", int(time.time() * 1000))
    with LOG_PATH.open("a") as f:
        f.write(json.dumps(entry) + "\n")
        f.flush()


def read_pids() -> list[int]:
    return [int(x) for x in (RAFT / ".raft-pids").read_text().split() if x.strip()]


def alive(pid: int) -> bool:
    r = subprocess.run(["kill", "-0", str(pid)], capture_output=True)
    return r.returncode == 0


def rss_kb(pid: int) -> int:
    out = subprocess.check_output(["ps", "-o", "rss=", "-p", str(pid)], text=True).strip()
    return int(out)


def thread_count(pid: int) -> int:
    # macOS: one header + one line per thread
    out = subprocess.check_output(["ps", "-M", "-p", str(pid)], text=True)
    return max(0, len(out.splitlines()) - 1)


def heap_info(pid: int) -> dict:
    out = subprocess.check_output(
        ["jcmd", str(pid), "GC.heap_info"], text=True, stderr=subprocess.STDOUT
    )
    m = re.search(r"total (\d+)K, used (\d+)K", out)
    meta = re.search(r"Metaspace\s+used (\d+)K, committed (\d+)K", out)
    return {
        "heapTotalK": int(m.group(1)) if m else None,
        "heapUsedK": int(m.group(2)) if m else None,
        "metaUsedK": int(meta.group(1)) if meta else None,
        "metaCommittedK": int(meta.group(2)) if meta else None,
    }


def nmt_summary(pid: int) -> dict:
    out = subprocess.check_output(
        ["jcmd", str(pid), "VM.native_memory", "summary"],
        text=True,
        stderr=subprocess.STDOUT,
    )
    cats: dict[str, dict[str, str]] = {}
    for line in out.splitlines():
        m = re.match(
            r"-\s+(.+?)\s+\(reserved=([0-9.]+[KMGT]?B), committed=([0-9.]+[KMGT]?B)\)",
            line.strip(),
        )
        if m:
            cats[m.group(1)] = {"reserved": m.group(2), "committed": m.group(3)}
    threads = re.search(r"\(thread #(\d+)\)", out)
    total = re.search(r"Total: reserved=([^,]+), committed=([^\n]+)", out)
    return {
        "totalReserved": total.group(1) if total else None,
        "totalCommitted": total.group(2).strip() if total else None,
        "nmtThreadCount": int(threads.group(1)) if threads else None,
        "categories": {
            k: cats[k]
            for k in ("Java Heap", "Thread", "GC", "Internal", "Other", "Metaspace", "Code")
            if k in cats
        },
    }


def force_gc(pid: int) -> None:
    subprocess.run(["jcmd", str(pid), "GC.run"], check=False, capture_output=True)
    time.sleep(0.5)


def sample(
    phase: str,
    hypothesis: str,
    *,
    do_gc: bool = False,
    with_heap: bool = False,
    with_nmt: bool = False,
) -> None:
    pids = read_pids()
    for i, pid in enumerate(pids):
        if not alive(pid):
            append(
                {
                    "location": "profile_memory.py:sample",
                    "message": "pid_dead",
                    "hypothesisId": hypothesis,
                    "runId": phase,
                    "data": {"node": i, "pid": pid},
                }
            )
            print(f"{phase} node{i} DEAD pid={pid}", flush=True)
            continue
        if do_gc:
            force_gc(pid)
        data: dict = {
            "node": i,
            "pid": pid,
            "rssKb": rss_kb(pid),
            "threads": thread_count(pid),
            "forcedGc": do_gc,
        }
        if with_heap:
            data["heap"] = heap_info(pid)
        if with_nmt:
            data["nmt"] = nmt_summary(pid)
        append(
            {
                "location": "profile_memory.py:sample",
                "message": "memory_sample",
                "hypothesisId": hypothesis,
                "runId": phase,
                "data": data,
            }
        )
        heap_u = data.get("heap", {}).get("heapUsedK")
        print(
            f"{phase} node{i} pid={pid} rssKb={data['rssKb']} "
            f"threads={data['threads']} heapUsedK={heap_u} gc={do_gc}",
            flush=True,
        )


def main() -> None:
    LOG_PATH.write_text("")
    t0 = time.time()
    append(
        {
            "location": "profile_memory.py:main",
            "message": "profile_start",
            "hypothesisId": "A",
            "runId": "start",
            "data": {"pids": read_pids()},
        }
    )

    # Baseline with heap+NMT once
    sample("t0", "A", with_heap=True, with_nmt=True)

    # Idle growth: RSS+threads only (avoid hammering jcmd)
    for n in range(1, 13):
        time.sleep(10)
        sample(f"idle_{n * 10}s", "A")

    # Reclaim test
    sample("pre_gc", "A", with_heap=True)
    sample("post_gc", "A", do_gc=True, with_heap=True, with_nmt=True)

    # Client load
    client = RAFT / "RaftClient.jar"
    ok = 0
    for k in range(50):
        r = subprocess.run(
            [JAVA, "-jar", str(client), "http://127.0.0.1:5001", "append", f"entry-{k}"],
            capture_output=True,
            text=True,
        )
        if r.returncode == 0:
            ok += 1
    append(
        {
            "location": "profile_memory.py:load",
            "message": "append_batch",
            "hypothesisId": "C",
            "runId": "load",
            "data": {"requested": 50, "ok": ok},
        }
    )
    sample("after_50_appends", "C", with_heap=True)
    sample("after_50_appends_gc", "C", do_gc=True, with_heap=True, with_nmt=True)

    time.sleep(30)
    sample("post_load_30s", "A", with_heap=True)
    sample("post_load_30s_gc", "A", do_gc=True, with_heap=True, with_nmt=True)

    append(
        {
            "location": "profile_memory.py:main",
            "message": "profile_done",
            "hypothesisId": "A",
            "runId": "done",
            "data": {"elapsedSec": round(time.time() - t0, 1)},
        }
    )
    print(f"done elapsed={time.time() - t0:.1f}s -> {LOG_PATH}", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        append(
            {
                "location": "profile_memory.py:main",
                "message": "profile_exception",
                "hypothesisId": "A",
                "runId": "error",
                "data": {"error": str(e), "trace": traceback.format_exc()},
            }
        )
        traceback.print_exc()
        sys.exit(1)
