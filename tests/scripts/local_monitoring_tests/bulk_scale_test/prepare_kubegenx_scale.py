#!/usr/bin/env python3
"""
Prepare a kubegenX Thanos export for the Kruize bulk scale test, then print the
exact bulk_scale_test.sh / bulk_scale_test.py command.

Three modes:

  use     Use an export that is already Serving. Write configs.json and print
          the scale-test command. (default)

  export  Use an existing Ready dataset. Create the Thanos export, serve it,
          wait until Serving, then write configs and print the command.

  create  Create a template + dataset with the workloads / days / GPU ratio you
          ask for, wait until Ready, export + serve, then write configs and
          print the command.

Examples (on the f35 VM):

  BASE=https://$(oc get route kubegenx -n kube-gen-x -o jsonpath='{.spec.host}')

  # Already serving Thanos for wl5k
  python3 prepare_kubegenx_scale.py --mode use --base "$BASE" --dataset wl5k

  # Dataset Ready, need TSDB + Thanos
  python3 prepare_kubegenx_scale.py --mode export --base "$BASE" --dataset wl5k \\
    --anchor-time 2026-10-19T00:00:00Z --export-effort high

  # Full path: generate 5k / 15d / 5% GPU, then serve, then print the command
  python3 prepare_kubegenx_scale.py --mode create --base "$BASE" --dataset wl5k \\
    --workloads 5000 --days 15 --gpu-percent 5 --effort high --export-effort high
"""

from __future__ import annotations

import argparse
import json
import ssl
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from typing import Any


def http_json(method: str, url: str, insecure: bool, body: dict[str, Any] | None = None) -> tuple[int, Any]:
    ctx = ssl._create_unverified_context() if insecure else None
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=300) as resp:
            raw = resp.read()
            if not raw:
                return resp.status, None
            return resp.status, json.loads(raw)
    except urllib.error.HTTPError as err:
        raw = err.read().decode("utf-8", errors="replace")
        try:
            payload: Any = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            payload = raw
        return err.code, payload
    except urllib.error.URLError as err:
        raise SystemExit(f"{method} {url} failed: {err}") from err


def get_json(url: str, insecure: bool) -> Any:
    code, doc = http_json("GET", url, insecure)
    if code >= 400:
        raise SystemExit(f"GET {url} failed: HTTP {code}: {doc}")
    return doc


def post_json(url: str, insecure: bool, body: dict[str, Any]) -> Any:
    code, doc = http_json("POST", url, insecure, body)
    if code >= 400:
        raise SystemExit(f"POST {url} failed: HTTP {code}: {doc}")
    return doc


def delete_json(url: str, insecure: bool) -> Any:
    code, doc = http_json("DELETE", url, insecure)
    if code >= 400 and code != 404:
        raise SystemExit(f"DELETE {url} failed: HTTP {code}: {doc}")
    return doc


def parse_rfc3339(value: str) -> datetime:
    text = value.strip()
    if text.endswith("Z"):
        text = text[:-1] + "+00:00"
    return datetime.fromisoformat(text).astimezone(timezone.utc)


def format_enddate(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.000Z")


def format_rfc3339(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def days_between(start: datetime, end: datetime) -> float:
    return (end - start).total_seconds() / 86400.0


def build_configs(orgs_doc: dict[str, Any], max_configs: int | None) -> list[dict[str, Any]]:
    configs: list[dict[str, Any]] = []
    for org in orgs_doc.get("orgs") or []:
        org_id = org.get("orgId") or org.get("org_id")
        if not org_id:
            continue
        for cluster in org.get("clusters") or []:
            cluster_id = cluster.get("clusterId") or cluster.get("cluster_id")
            if not cluster_id:
                continue
            configs.append({
                "name": f"{org_id}_{cluster_id}",
                "labels": {"org_id": org_id, "cluster_id": cluster_id},
            })
            if max_configs is not None and len(configs) >= max_configs:
                return configs
    return configs


def thanos_urls(export: dict[str, Any], dataset: str, namespace: str) -> tuple[str, str]:
    export_name = export.get("name") or dataset
    svc = f"http://kgx-ex-{export_name}-query.{namespace}.svc.cluster.local:9090/"
    route = ""
    url = export.get("url") or ""
    if url:
        parsed = urllib.parse.urlparse(url)
        if parsed.scheme and parsed.netloc:
            route = f"{parsed.scheme}://{parsed.netloc}/"
    return svc, route


def probe_thanos(base: str, insecure: bool, sample_labels: dict[str, str]) -> None:
    query = "count(kube_pod_info{" + ",".join(
        f'{k}="{v}"' for k, v in sample_labels.items()
    ) + "})"
    api = urllib.parse.urljoin(base if base.endswith("/") else base + "/", "api/v1/query")
    full = api + "?" + urllib.parse.urlencode({"query": query})
    doc = get_json(full, insecure)
    if doc.get("status") != "success":
        raise SystemExit(f"Thanos probe failed: {doc}")
    results = (doc.get("data") or {}).get("result") or []
    if not results:
        print(f"WARNING: Thanos returned no series for {query}", file=sys.stderr)
        return
    print(f"Thanos probe ok: {query} -> {results[0].get('value')}")


def wait_dataset(base: str, name: str, insecure: bool, poll: float) -> dict[str, Any]:
    url = f"{base}/api/v1/datasets/{name}"
    while True:
        doc = get_json(url, insecure)
        phase = doc.get("phase")
        items = doc.get("items") or {}
        print(
            f"dataset {name}: {phase} percent={doc.get('percent')} "
            f"done={items.get('done')}/{items.get('total')} failed={items.get('failed')} leased={items.get('leased')}",
            flush=True,
        )
        if phase == "Ready":
            return doc
        if phase == "Failed":
            raise SystemExit(f"dataset {name} failed: {doc}")
        time.sleep(poll)


def wait_export(base: str, name: str, insecure: bool, poll: float) -> dict[str, Any]:
    url = f"{base}/api/v1/exports/{name}"
    while True:
        doc = get_json(url, insecure)
        phase = doc.get("phase")
        print(
            f"export {name}: {phase} blocks={doc.get('blocks')}/{doc.get('blocksTotal')} "
            f"workers={doc.get('workers')} url={doc.get('url') or '-'}",
            flush=True,
        )
        if phase == "Serving":
            if not doc.get("url"):
                time.sleep(poll)
                continue
            return doc
        if phase == "Failed":
            raise SystemExit(f"export {name} failed: {doc}")
        time.sleep(poll)


def template_spec(workloads: int, days: int, gpu_percent: float, seed: int) -> dict[str, Any]:
    ratio = max(0.0, min(1.0, gpu_percent / 100.0))
    gpu_on = ratio > 0
    features = ["cpu", "memory", "ksm"]
    if gpu_on:
        features.insert(2, "gpu")
    return {
        "seed": seed,
        "topology": {
            "mode": "TotalWorkloads",
            "totalWorkloads": workloads,
        },
        "time": {
            "step": "15s",
            "duration": f"{days}d",
            "chunk": "6h",
        },
        "gpu": {
            "enabled": gpu_on,
            "workloadRatio": ratio,
            "labelProfile": "kruize",
            "nodePools": ["p4d.24xlarge"],
            "migRatio": 0.3 if gpu_on else 0,
        },
        "features": features,
    }


def ensure_template(base: str, name: str, spec: dict[str, Any], insecure: bool, force: bool) -> None:
    url = f"{base}/api/v1/templates"
    code, doc = http_json("POST", url, insecure, {"name": name, "spec": spec})
    if code in (200, 201):
        print(f"template {name} created")
        return
    already = code in (409, 500) and "already" in str(doc).lower()
    # controller returns the raw k8s AlreadyExists string as 500
    already = already or (code == 500 and "AlreadyExists" in str(doc))
    if already and not force:
        print(f"template {name} already exists; reusing it")
        return
    if already and force:
        raise SystemExit(
            f"template {name} already exists. Delete it first:\n"
            f"  oc delete cm -n kube-gen-x kgx-tpl-{name}\n"
            f"then re-run with --mode create"
        )
    raise SystemExit(f"POST {url} failed: HTTP {code}: {doc}")


def ensure_dataset(base: str, name: str, effort: str, insecure: bool, force: bool) -> None:
    if force:
        print(f"force: deleting dataset {name} if present")
        delete_json(f"{base}/api/v1/datasets/{name}", insecure)
        time.sleep(2)
    code, doc = http_json(
        "POST",
        f"{base}/api/v1/datasets",
        insecure,
        {"name": name, "template": name, "effort": effort},
    )
    if code in (200, 201):
        print(f"dataset {name} created")
        return
    if code == 500 and "AlreadyExists" in str(doc):
        raise SystemExit(
            f"dataset {name} already exists. Pass --force to delete and recreate, "
            f"or use --mode export / --mode use"
        )
    raise SystemExit(f"create dataset failed: HTTP {code}: {doc}")


def ensure_export(
    base: str,
    dataset: str,
    anchor: str,
    anchor_time: str,
    effort: str,
    insecure: bool,
    force: bool,
) -> dict[str, Any]:
    code, existing = http_json("GET", f"{base}/api/v1/exports/{dataset}", insecure)
    if code == 200 and isinstance(existing, dict):
        phase = existing.get("phase")
        if phase == "Serving" and not force:
            print(f"export {dataset} already Serving; reusing it")
            return existing
        if force or phase in ("Failed", "Configured", "Queued", "Building"):
            print(f"force/replace: deleting export {dataset} (was {phase})")
            delete_json(f"{base}/api/v1/exports/{dataset}", insecure)
            time.sleep(2)
        elif phase == "Serving" and force:
            print(f"force: deleting Serving export {dataset}")
            delete_json(f"{base}/api/v1/exports/{dataset}", insecure)
            time.sleep(2)

    created = post_json(
        f"{base}/api/v1/exports",
        insecure,
        {
            "dataset": dataset,
            "type": "thanos",
            "anchor": anchor,
            "time": anchor_time,
            "effort": effort,
        },
    )
    print(f"export configured: {created}")
    served = post_json(f"{base}/api/v1/exports/{dataset}/serve", insecure, {})
    print(f"export serve: {served}")
    return served


def resolve_anchor_time(base: str, dataset: str, insecure: bool, explicit: str | None) -> str:
    if explicit:
        return explicit
    layout = get_json(f"{base}/api/v1/datasets/{dataset}/layout", insecure)
    epoch = layout.get("epoch")
    if not epoch:
        raise SystemExit("dataset layout has no epoch; pass --anchor-time")
    # layout epoch may be RFC3339 already
    if isinstance(epoch, str) and "T" in epoch:
        return epoch if epoch.endswith("Z") else format_rfc3339(parse_rfc3339(epoch))
    raise SystemExit(f"unexpected layout epoch {epoch!r}; pass --anchor-time")


def finish_and_print(
    base: str,
    dataset: str,
    namespace: str,
    out: str,
    workers: int,
    interval: int,
    max_configs: int | None,
    replicas: int,
    kruize_image: str,
    skipsetup: bool,
    probe: bool,
    insecure: bool,
) -> int:
    export = get_json(f"{base}/api/v1/exports/{dataset}", insecure)
    if export.get("phase") != "Serving":
        raise SystemExit(f"export {dataset} phase is {export.get('phase')!r}; need Serving")

    orgs_doc = get_json(f"{base}/api/v1/datasets/{dataset}/orgs", insecure)
    configs = build_configs(orgs_doc, max_configs)
    if not configs:
        raise SystemExit("no org_id/cluster_id pairs found in /orgs")

    with open(out, "w", encoding="utf-8") as fh:
        json.dump(configs, fh, indent=2)
        fh.write("\n")

    start = parse_rfc3339(export["startTime"])
    end = parse_rfc3339(export["endTime"])
    days = days_between(start, end)
    days_int = int(round(days))
    if abs(days - days_int) > 1e-6:
        print(f"WARNING: export window is {days:.4f} days; rounding to {days_int} for -d", file=sys.stderr)

    svc_url, route_url = thanos_urls(export, dataset, namespace)
    enddate = format_enddate(end)
    blocks = int((days_int * 24) / interval) if interval else 0

    print(f"wrote {len(configs)} configs to {out}")
    print(f"export window: {export['startTime']} -> {export['endTime']} ({days_int}d)")
    print(f"tsdb blocks with -t {interval}: {blocks}")
    print(f"thanos in-cluster: {svc_url}")
    print(f"thanos route:      {route_url or '<empty>'}")

    if probe:
        probe_thanos(route_url or svc_url, insecure, configs[0]["labels"])

    skip = " --skipsetup" if skipsetup else ""
    print()
    print("# Run from tests/scripts/local_monitoring_tests/bulk_scale_test")
    print(
        f'./bulk_scale_test.sh -i {kruize_image} '
        f'-w {workers} -d {days_int} -t {interval} '
        f'-s "{enddate}" -a {replicas} '
        f'-k {out} --url="{svc_url}"{skip}'
    )
    print()
    print("# Or call Python directly after Kruize already has datasource thanos:")
    print(
        f'python3 bulk_scale_test.py --workers {workers} --days_of_res {days_int} '
        f'--enddate {enddate} --interval {interval} '
        f'--resultsdir ./kubegenx_scale_results --prometheus 0 --configs {out}'
    )
    print()
    print("# First 3 configs:")
    for row in configs[:3]:
        print(json.dumps(row))
    if len(configs) > 3:
        print(f"# ... and {len(configs) - 3} more")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "--mode",
        choices=["use", "export", "create"],
        default="use",
        help="use=existing Serving Thanos; export=Ready dataset -> TSDB+Thanos; create=generate dataset then export",
    )
    parser.add_argument("--base", required=True, help="kubegenX controller base URL")
    parser.add_argument("--dataset", default="wl5k", help="dataset / export name")
    parser.add_argument("--namespace", default="kube-gen-x", help="OpenShift namespace for Thanos Service DNS")
    parser.add_argument("--out", default="configs.kubegenx.json", help="configs.json for bulk_scale_test -k")
    parser.add_argument("--workers", type=int, default=20, help="workers printed in the scale-test command")
    parser.add_argument("--interval", type=int, default=6, help="TSDB block hours for the scale-test command")
    parser.add_argument("--max-configs", type=int, default=0, help="cap org/cluster pairs (0 = all)")
    parser.add_argument("--replicas", type=int, default=3, help="Kruize replicas for bulk_scale_test.sh -a")
    parser.add_argument("--kruize-image", default="quay.io/kusumach/autotune_operator:0.12_rc3")
    parser.add_argument("--skipsetup", action="store_true", help="add --skipsetup to the printed shell command")
    parser.add_argument("--probe-thanos", action="store_true", help="probe Thanos with the first config labels")
    parser.add_argument("--poll", type=float, default=15.0, help="seconds between progress polls")
    parser.add_argument("--force", action="store_true", help="delete conflicting dataset/export before create/export")
    parser.add_argument("--secure", action="store_true", help="require valid TLS (default is curl -k style)")

    # create mode
    parser.add_argument("--workloads", type=int, default=5000, help="create: totalWorkloads")
    parser.add_argument("--days", type=int, default=15, help="create: duration in days")
    parser.add_argument("--gpu-percent", type=float, default=5.0, help="create: GPU workload percent (0 disables GPU)")
    parser.add_argument("--seed", type=int, default=424242, help="create: template seed")
    parser.add_argument("--effort", default="high", help="create: dataset generation effort")

    # export / create serve
    parser.add_argument("--anchor", default="start", choices=["start", "end"], help="export calendar anchor")
    parser.add_argument("--anchor-time", default="", help="RFC3339 for the anchored edge; default = dataset epoch")
    parser.add_argument("--export-effort", default="high", help="conversion effort for Thanos blocks")

    args = parser.parse_args()
    insecure = not args.secure
    base = args.base.rstrip("/")
    max_configs = args.max_configs if args.max_configs > 0 else None

    if args.mode == "create":
        spec = template_spec(args.workloads, args.days, args.gpu_percent, args.seed)
        print(
            f"create: workloads={args.workloads} days={args.days} "
            f"gpu_percent={args.gpu_percent} effort={args.effort}"
        )
        ensure_template(base, args.dataset, spec, insecure, args.force)
        ensure_dataset(base, args.dataset, args.effort, insecure, args.force)
        wait_dataset(base, args.dataset, insecure, args.poll)
        anchor_time = resolve_anchor_time(base, args.dataset, insecure, args.anchor_time or None)
        ensure_export(base, args.dataset, args.anchor, anchor_time, args.export_effort, insecure, args.force)
        wait_export(base, args.dataset, insecure, args.poll)

    elif args.mode == "export":
        ds = get_json(f"{base}/api/v1/datasets/{args.dataset}", insecure)
        phase = ds.get("phase")
        if phase != "Ready":
            raise SystemExit(f"dataset {args.dataset} phase is {phase!r}; need Ready (or use --mode create)")
        anchor_time = resolve_anchor_time(base, args.dataset, insecure, args.anchor_time or None)
        print(f"export: dataset={args.dataset} anchor={args.anchor} time={anchor_time} effort={args.export_effort}")
        ensure_export(base, args.dataset, args.anchor, anchor_time, args.export_effort, insecure, args.force)
        wait_export(base, args.dataset, insecure, args.poll)

    else:
        print(f"use: expecting export {args.dataset} to already be Serving")

    return finish_and_print(
        base=base,
        dataset=args.dataset,
        namespace=args.namespace,
        out=args.out,
        workers=args.workers,
        interval=args.interval,
        max_configs=max_configs,
        replicas=args.replicas,
        kruize_image=args.kruize_image,
        skipsetup=args.skipsetup,
        probe=args.probe_thanos,
        insecure=insecure,
    )


if __name__ == "__main__":
    raise SystemExit(main())
