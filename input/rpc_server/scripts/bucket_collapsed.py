#!/usr/bin/env python3
"""Bucket async-profiler collapsed stacks for Julay rpc / rendezvous profiles.

Usage:
  bucket_collapsed.py COLLAPSED_FILE [OUT_FILE] [--fine] [--keywords]

Default: coarse buckets (legacy profile_rpc.sh table).
--fine: also print SyncChannel/Select sub-stage breakdown (exclusive).
--keywords: non-exclusive keyword/class hits (useful for alloc).
"""
from __future__ import annotations

import argparse
import sys
from collections import defaultdict
from pathlib import Path


def is_syncchannel_select(stack: str) -> bool:
    return (
        "SyncChannel" in stack
        or "julay/concurrency/Select" in stack
        or "julay.concurrency.Select" in stack
        or "SelectCoordinator" in stack
        or "SelectGroup" in stack
        or "SelectCaseOffer" in stack
        or "runOffers" in stack
    )


def fine_rendezvous_bucket(stack: str) -> str:
    """Exclusive sub-bucket for stacks that are SyncChannel/Select-related."""
    # Most-specific first (plan table). Note: selectOfferLocked contains "selectOffer".
    if "parkPass" in stack or "offerOne" in stack:
        return "Select park / offer"
    if any(
        k in stack
        for k in (
            "tryLeadParked",
            "runComputeExclusive",
            "reparkMissing",
            "awaitCompletionOrNudge",
        )
    ):
        return "Select lead / wake"
    if any(
        k in stack
        for k in (
            "runSelectCompute",
            "finishAfterCompute",
            "extractSyncPayload",
            "buildPayload",
            "SyncResolveFast",
        )
    ):
        return "Compute / payload"
    if any(
        k in stack
        for k in (
            "pickCandidateLocked",
            "selectOfferLocked",
            "antiOk",
            "constraintsOf",
        )
    ):
        return "Match under lock"
    if "selectOffer" in stack:
        return "Select park / offer"
    if any(
        k in stack
        for k in (
            "runOffers",
            "SelectCoordinator",
            "forCoordinator",
            "SelectGroup",
            "scrambleInPlace",
            "pruneDeadHandles",
        )
    ):
        return "Select setup"
    if "syncFast" in stack:
        return "syncFast path"
    if any(
        k in stack
        for k in (
            "Participant",
            "awaitValue",
            "completeValue",
            "withChannelLock",
        )
    ):
        return "Participant / wait"
    return "SyncChannel other"


def coarse_bucket(stack: str) -> str:
    if (
        "com/microsoft/z3" in stack
        or "com.microsoft.z3" in stack
        or "withEphemeralContext" in stack
        or "SyncResolveZ3" in stack
    ):
        return "Z3 / Context / SyncResolveZ3"
    if "SyncResolveFast" in stack:
        return "SyncResolveFast"
    if is_syncchannel_select(stack):
        return "SyncChannel / Select"
    if (
        "invokeProcFun" in stack
        or "julay/program/Proc" in stack
        or "julay.program.Proc" in stack
        or "runOneStep" in stack
        or "ProcFunHandlerPool" in stack
        or "runHttpHandlerLoop" in stack
    ):
        return "invokeProcFun / Proc"
    if "JulHttpServer" in stack or "julay/program/library/Http" in stack:
        return "HTTP / JulHttpServer / bridge"
    if (
        "sun/net/httpserver" in stack
        or "sun.net.httpserver" in stack
        or "SocketDispatcher" in stack
    ):
        return "JDK HttpServer / NIO"
    if "RpcServerNative" in stack:
        return "native Protocol / handler"
    if (
        "kotlinx/coroutines" in stack
        or "LockSupport.park" in stack
        or "__psynch_cvwait" in stack
        or "__psynch_cvsignal" in stack
    ):
        return "thread park / wait"
    if (
        "semaphore_wait_trap" in stack
        or "AttachListener" in stack
        or "attach_listener" in stack
        or "signal_thread_entry" in stack
        or ("__ulock_wait" in stack and "julay" not in stack and "httpserver" not in stack)
    ):
        return "JVM idle / helper threads"
    if "GC" in stack or "[gc_" in stack or "PhaseChaitin" in stack or "PhaseIdealLoop" in stack:
        return "GC / JIT"
    return "Other"


def fine_top_bucket(stack: str) -> str:
    """Coarse parents, but SyncChannel/Select replaced by fine sub-buckets."""
    coarse = coarse_bucket(stack)
    if coarse == "SyncChannel / Select":
        return fine_rendezvous_bucket(stack)
    return coarse


KEYWORD_HITS = (
    "Constraint",
    "Participant",
    "SyncPayload",
    "CompletableDeferred",
    "Optional",
    "syncFast",
    "selectOffer",
    "tryLeadParked",
    "runSelectCompute",
    "pickCandidateLocked",
    "awaitCompletionOrNudge",
    "parkPass",
    "runOffers",
    "SelectCoordinator",
    "Mutex",
    "withChannelLock",
)


def load_stacks(path: Path) -> list[tuple[str, int]]:
    rows: list[tuple[str, int]] = []
    with path.open(encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            sp = line.rsplit(" ", 1)
            if len(sp) != 2 or not sp[1].isdigit():
                continue
            rows.append((sp[0], int(sp[1])))
    return rows


def format_table(
    totals: dict[str, int],
    grand: int,
    title: str,
    exclude_idle: bool = False,
) -> list[str]:
    lines = [title]
    idle = totals.get("JVM idle / helper threads", 0)
    denom = (grand - idle) if exclude_idle else grand
    label = "among non-idle samples" if exclude_idle else "all samples"
    lines.append(f"--- {label} ---")
    rows = sorted(totals.items(), key=lambda kv: -kv[1])
    for name, n in rows:
        if exclude_idle and name == "JVM idle / helper threads":
            continue
        pct = (100.0 * n / denom) if denom else 0.0
        lines.append(f"{pct:6.2f}%  {n:8d}  {name}")
    return lines


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("collapsed", type=Path, help="async-profiler .collapsed file")
    p.add_argument("out", nargs="?", type=Path, help="write summary here (also printed)")
    p.add_argument("--fine", action="store_true", help="include SyncChannel/Select sub-buckets")
    p.add_argument("--keywords", action="store_true", help="include non-exclusive keyword hits")
    p.add_argument(
        "--fine-only",
        action="store_true",
        help="print only fine top-level buckets (no coarse SyncChannel aggregate)",
    )
    args = p.parse_args(argv)

    stacks = load_stacks(args.collapsed)
    grand = sum(n for _, n in stacks)

    coarse_totals: dict[str, int] = defaultdict(int)
    fine_totals: dict[str, int] = defaultdict(int)
    fine_among_sc: dict[str, int] = defaultdict(int)
    sc_total = 0
    kw_hits: dict[str, int] = defaultdict(int)

    for stack, n in stacks:
        c = coarse_bucket(stack)
        coarse_totals[c] += n
        if args.fine or args.fine_only:
            f = fine_top_bucket(stack)
            fine_totals[f] += n
            if c == "SyncChannel / Select":
                sc_total += n
                fine_among_sc[fine_rendezvous_bucket(stack)] += n
        if args.keywords:
            for k in KEYWORD_HITS:
                if k in stack:
                    kw_hits[k] += n

    idle = coarse_totals.get("JVM idle / helper threads", 0)
    app_grand = grand - idle
    out_lines: list[str] = [
        f"total_samples={grand}",
        f"app_samples={app_grand}  (excludes JVM idle / helper threads)",
    ]

    if not args.fine_only:
        out_lines.extend(format_table(coarse_totals, grand, "=== coarse buckets ==="))
        out_lines.extend(
            format_table(coarse_totals, grand, "=== coarse (non-idle) ===", exclude_idle=True)
        )

    if args.fine or args.fine_only:
        out_lines.append("")
        out_lines.append(f"syncchannel_select_samples={sc_total}")
        out_lines.extend(
            format_table(
                fine_among_sc if not args.fine_only else {
                    k: v for k, v in fine_totals.items()
                    if k.startswith("Select ")
                    or k.startswith("syncFast")
                    or k.startswith("Match ")
                    or k.startswith("Compute ")
                    or k.startswith("Participant ")
                    or k.startswith("SyncChannel ")
                },
                sc_total if not args.fine_only else grand,
                "=== fine rendezvous (among SyncChannel/Select) ==="
                if not args.fine_only
                else "=== fine rendezvous sub-buckets (all samples that matched) ===",
            )
        )
        if not args.fine_only:
            # Also show fine mixed into full profile for convenience
            out_lines.extend(
                format_table(
                    fine_totals,
                    grand,
                    "=== fine top-level (SyncChannel split; all samples) ===",
                    exclude_idle=False,
                )
            )

    if args.keywords:
        out_lines.append("")
        out_lines.append("=== keyword hits (non-exclusive) ===")
        for k, n in sorted(kw_hits.items(), key=lambda kv: -kv[1]):
            pct = (100.0 * n / grand) if grand else 0.0
            out_lines.append(f"{pct:6.2f}%  {n:8d}  {k}")

    # Attribution quality for SyncChannel split
    if (args.fine or args.fine_only) and sc_total:
        other = fine_among_sc.get("SyncChannel other", 0)
        named = sc_total - other
        pct_named = 100.0 * named / sc_total
        out_lines.append("")
        out_lines.append(
            f"fine_named_share_of_syncchannel={pct_named:.1f}%  "
            f"(named={named} other={other} of {sc_total})"
        )

    text = "\n".join(out_lines) + "\n"
    if args.out:
        args.out.write_text(text, encoding="utf-8")
    print(text, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
