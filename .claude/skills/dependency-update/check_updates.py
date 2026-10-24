#!/usr/bin/env python3
"""List the dependency updates available to firebase-multiplatform.

Read-only: it never edits a file. The weekly dependency routine (.claude/skills/dependency-update/SKILL.md) runs it
first and decides from its output. Adapted from InfoTraffic's tools/deps/check_updates.py.

Covers
  * every [versions] ref of gradle/libs.versions.toml that a library or plugin uses, against the repositories the
    build resolves from (Google Maven for androidx / com.android / com.google, Maven Central, and the Gradle Plugin
    Portal for plugin markers). A ref shared by several artifacts only offers versions that ALL of them publish, so a
    lockstep set (kotlin: the Gradle plugin and the serialization plugin; the coroutines modules) moves together.
  * for each Maven Central candidate, the kotlin-stdlib its Gradle module metadata requires: a library built against
    a newer Kotlin than the catalog's does not compile here.
  * group:artifact:version coordinates hard-coded in build scripts, and plugins given a version in settings scripts
  * the Gradle wrapper
  * every external GitHub Action in .github/workflows and .github/actions/*/action.yml, against its repository's tags
  * pins.txt next to this script: what is held below a version line, held for a person, or moved by the weekly workflow

A lookup that fails is reported as UNKNOWN, never as up to date.

usage: check_updates.py [--root <repo>] [--json] [--all]   (Python 3.9, stdlib only; HTTP goes through curl)
"""

import argparse
import concurrent.futures
import glob
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

GOOGLE = "https://dl.google.com/android/maven2"
CENTRAL = "https://repo1.maven.org/maven2"
PORTAL = "https://plugins.gradle.org/m2"
GOOGLE_GROUPS = ("androidx", "com.android", "com.google")

# Qualifier ranks. dev/snapshot builds are never candidates.
PRE_RANK = {
    "alpha": 1, "a": 1, "eap": 1,
    "m": 2, "milestone": 2,
    "beta": 3, "b": 3,
    "pre": 4, "preview": 4,
    "rc": 5, "cr": 5,
}
STABLE_QUALIFIERS = ("", "final", "release", "ga")
NEVER = ("dev", "snapshot", "nightly")


# --------------------------------------------------------------------------------------------------------------------
# Versions
# --------------------------------------------------------------------------------------------------------------------

class V(object):
    """A comparable version. stable=False for anything with a pre-release or unknown qualifier."""

    def __init__(self, raw):
        self.raw = raw
        m = re.match(r"^v?(\d+(?:\.\d+)*)(.*)$", raw.strip())
        if not m:
            self.ok = False
            return
        self.ok = True
        self.nums = tuple(int(x) for x in m.group(1).split("."))
        rest = m.group(2).lstrip("-.+_").lower()
        self.excluded = any(rest.startswith(n) or ("-" + n) in rest for n in NEVER)
        if rest in STABLE_QUALIFIERS:
            self.stable, self.rank, self.qnum = True, 9, 0
        else:
            q = re.match(r"^([a-z]+)[-.]?(\d*)", rest)
            if q and q.group(1) in PRE_RANK:
                self.stable, self.rank = False, PRE_RANK[q.group(1)]
                self.qnum = int(q.group(2)) if q.group(2) else 0
            else:
                # An unknown shape is never offered as stable.
                self.stable, self.rank, self.qnum = False, 0, 0

    def key(self):
        padded = self.nums + (0,) * (5 - len(self.nums))
        return (padded[:5], self.rank, self.qnum)

    def __lt__(self, other):
        return self.key() < other.key()

    def __eq__(self, other):
        return isinstance(other, V) and self.key() == other.key()

    def __hash__(self):
        return hash(self.key())

    def major(self):
        return self.nums[0]

    def line(self):
        return self.nums[:2]


def parse_versions(raws):
    out = []
    for r in raws:
        v = V(r)
        if v.ok and not v.excluded:
            out.append(v)
    return sorted(set(out))


def update_kind(cur, new):
    """patch / minor / major / prerelease. A 0.x minor step counts as major (0.x breaks APIs)."""
    if new.nums[0] != cur.nums[0]:
        return "major"
    if cur.nums[0] == 0 and new.line() != cur.line():
        return "major"
    if new.line() != cur.line():
        return "minor"
    if (new.nums + (0,) * 5)[:5] != (cur.nums + (0,) * 5)[:5]:
        return "patch" if new.stable else "prerelease"
    return "prerelease"


def pick_target(cur, available):
    """The version the routine should try, or None.

    Stable track: the newest stable. Pre-release track: the newest stable above the current one if there is one, else
    the newest pre-release on the SAME major.minor line.
    """
    newer = [v for v in available if cur < v]
    if not newer:
        return None
    stables = [v for v in newer if v.stable]
    if cur.stable:
        return stables[-1] if stables else None
    if stables:
        return stables[-1]
    same_line = [v for v in newer if v.line() == cur.line()]
    return same_line[-1] if same_line else None


# --------------------------------------------------------------------------------------------------------------------
# Pins
# --------------------------------------------------------------------------------------------------------------------

def parse_pins(path):
    """{key: [{rule, why}]}. Lines starting with '#' are comments (a reason may contain '#', as in owner/repo#123)."""
    pins = {}
    if not os.path.exists(path):
        return pins
    with open(path) as fh:
        for n, raw in enumerate(fh, 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split(None, 2)
            if len(parts) < 2 or not (parts[1] in ("hold", "weekly") or re.match(r"^<\d+(\.\d+)*$", parts[1])):
                raise ValueError("pins.txt:%d: expected <key> <<X.Y|hold|weekly> <why>, got %r" % (n, line))
            pins.setdefault(parts[0], []).append({"rule": parts[1], "why": parts[2] if len(parts) > 2 else ""})
    return pins


def apply_pins(result, cur, available, rules):
    """Sets target/kind from the versions the pins allow, and records what they hold back."""
    ceilings = [r for r in rules if r["rule"].startswith("<")]
    allowed = [v for v in available if all(v < V(c["rule"][1:]) for c in ceilings)]
    unpinned = pick_target(cur, available)
    target = pick_target(cur, allowed)
    result["latest_stable"] = ([v for v in available if v.stable] or [None])[-1]
    result["latest_stable"] = result["latest_stable"].raw if result["latest_stable"] else None
    if unpinned and (target is None or target < unpinned):
        result["held"].append("%s: %s" % (unpinned.raw, "; ".join(
            "%s (%s)" % (c["rule"], c["why"]) for c in ceilings)))
    for r in rules:
        if r["rule"] == "hold" and target:
            result["held"].append("%s: hold (%s)" % (target.raw, r["why"]))
            target = None
    if target:
        result["target"] = target.raw
        result["kind"] = update_kind(cur, target)
        if result["kind"] == "major":
            same_major = [v for v in allowed if cur < v and v.stable and v.major() == cur.major()
                          and (cur.major() != 0 or v.line() == cur.line())]
            result["same_major_target"] = same_major[-1].raw if same_major else None
    return target


# --------------------------------------------------------------------------------------------------------------------
# HTTP / git
# --------------------------------------------------------------------------------------------------------------------

def curl(url, timeout=25):
    """Body on HTTP 200, None on 404/410, raises on anything else (network, 5xx)."""
    for _ in range(3):
        try:
            p = subprocess.run(
                ["curl", "-sS", "-L", "--max-time", str(timeout), "-w", "\n%{http_code}", url],
                stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout + 5,
            )
        except subprocess.TimeoutExpired:
            continue
        if p.returncode != 0:
            continue
        body, _, code = p.stdout.decode("utf-8", "replace").rpartition("\n")
        if code == "200":
            return body
        if code in ("404", "410"):
            return None
    raise IOError("lookup failed: " + url)


def maven_path(group, artifact):
    return group.replace(".", "/") + "/" + artifact


def repos_for(group, plugin_marker):
    repos = []
    if group.startswith(GOOGLE_GROUPS):
        repos.append(GOOGLE)
    repos.append(CENTRAL)
    if plugin_marker:
        repos.append(PORTAL)
    return repos


def maven_versions(group, artifact, plugin_marker=False):
    """(set of version strings, repos that had it) across every repo that may serve the artifact."""
    found, hits, errors = set(), [], []
    for repo in repos_for(group, plugin_marker):
        url = "%s/%s/maven-metadata.xml" % (repo, maven_path(group, artifact))
        try:
            body = curl(url)
        except IOError as e:
            errors.append(str(e))
            continue
        if body is None:
            continue
        try:
            root = ET.fromstring(body)
        except ET.ParseError:
            errors.append("bad metadata: " + url)
            continue
        vs = [el.text.strip() for el in root.iter("version") if el.text]
        if vs:
            hits.append(repo)
            found.update(vs)
    if not hits and errors:
        raise IOError("; ".join(errors))
    return found, hits


def kotlin_stdlib_required(group, artifact, version, repos):
    """The kotlin-stdlib version a candidate's Gradle module metadata requires, or None."""
    for repo in repos:
        url = "%s/%s/%s/%s-%s.module" % (repo, maven_path(group, artifact), version, artifact, version)
        try:
            body = curl(url)
            meta = json.loads(body) if body else None
        except (IOError, ValueError):
            continue
        if not meta:
            continue
        found = set()
        for variant in meta.get("variants", []):
            for dep in variant.get("dependencies", []):
                if dep.get("group") == "org.jetbrains.kotlin" and dep.get("module") in ("kotlin-stdlib",
                                                                                       "kotlin-stdlib-common"):
                    req = (dep.get("version") or {}).get("requires") or (dep.get("version") or {}).get("strictly")
                    if req:
                        found.add(req)
        return sorted(found, key=V)[-1] if found else None
    return None


def git_tags(repo):
    """Raw tag names of a GitHub repository."""
    p = subprocess.run(
        ["git", "ls-remote", "--tags", "--refs", "https://github.com/%s.git" % repo],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=90,
    )
    if p.returncode != 0:
        raise IOError("git ls-remote failed: " + repo)
    return [line.split("\t")[-1].replace("refs/tags/", "") for line in p.stdout.decode().splitlines()]


# --------------------------------------------------------------------------------------------------------------------
# Catalog
# --------------------------------------------------------------------------------------------------------------------

def parse_catalog(path):
    """versions: {ref: {value, line, comment}}, users: {ref: [coordinate dicts]}."""
    versions, users = {}, {}
    section, pending = None, []
    with open(path) as fh:
        lines = fh.read().splitlines()
    for idx, raw in enumerate(lines, 1):
        line = raw.strip()
        if not line:
            pending = []
            continue
        if line.startswith("#"):
            pending.append(line[1:].strip() if len(line) > 1 else "")
            continue
        sec = re.match(r"^\[(\w+)\]$", line)
        if sec:
            section, pending = sec.group(1), []
            continue
        kv = re.match(r'^([A-Za-z0-9_.-]+)\s*=\s*(.+)$', line)
        if not kv:
            pending = []
            continue
        key, value = kv.group(1), kv.group(2)
        if section == "versions":
            sv = re.match(r'^"([^"]+)"', value)
            if sv:
                versions[key] = {"value": sv.group(1), "line": idx, "comment": "\n".join(pending)}
        elif section in ("libraries", "plugins"):
            ref = re.search(r'version\.ref\s*=\s*"([^"]+)"', value)
            if ref:
                if section == "libraries":
                    mod = re.search(r'module\s*=\s*"([^":]+):([^"]+)"', value)
                    g = re.search(r'group\s*=\s*"([^"]+)"', value)
                    n = re.search(r'name\s*=\s*"([^"]+)"', value)
                    coord = (mod.group(1), mod.group(2)) if mod else ((g.group(1), n.group(1)) if g and n else None)
                    if coord:
                        users.setdefault(ref.group(1), []).append(
                            {"alias": key, "group": coord[0], "artifact": coord[1], "plugin": False})
                else:
                    pid = re.search(r'id\s*=\s*"([^"]+)"', value)
                    if pid:
                        users.setdefault(ref.group(1), []).append({
                            "alias": key, "group": pid.group(1),
                            "artifact": pid.group(1) + ".gradle.plugin", "plugin": True,
                        })
        pending = []
    return versions, users


def check_ref(ref, info, coords, rules, catalog_kotlin):
    cur = V(info["value"])
    result = {
        "ref": ref, "current": info["value"], "line": info["line"], "comment": info["comment"],
        "artifacts": ["%s:%s" % (c["group"], c["artifact"].replace(".gradle.plugin", "")) +
                      (" (plugin)" if c["plugin"] else "") for c in coords],
        "pins": rules, "status": "unknown", "target": None, "kind": None, "latest_stable": None,
        "held": [], "errors": [], "warnings": [],
    }
    if not cur.ok:
        result["errors"].append("unparseable current version")
        return result
    common, served_by = None, {}
    for c in coords:
        try:
            vs, hits = maven_versions(c["group"], c["artifact"], c["plugin"])
        except IOError as e:
            result["errors"].append(str(e))
            continue
        if not hits:
            # e.g. a plugin with no marker anywhere: don't let it veto the ref.
            result["warnings"].append("no metadata for %s:%s" % (c["group"], c["artifact"]))
            continue
        served_by[(c["group"], c["artifact"])] = hits
        common = set(vs) if common is None else common & set(vs)
    if common is None:
        return result
    if info["value"] not in common:
        result["warnings"].append("the current version is not published by every artifact using this ref")
    result["status"] = "partial" if result["errors"] else "ok"
    target = apply_pins(result, cur, parse_versions(common), rules)

    # A library built against a newer Kotlin than the catalog's does not compile here.
    if target and catalog_kotlin:
        for c in coords:
            hits = served_by.get((c["group"], c["artifact"]))
            if c["plugin"] or not hits or CENTRAL not in hits:
                continue
            stdlib = kotlin_stdlib_required(c["group"], c["artifact"], target.raw, [CENTRAL])
            if stdlib and V(catalog_kotlin) < V(stdlib):
                result["warnings"].append("%s %s needs kotlin-stdlib %s, newer than the catalog's Kotlin %s: "
                                          "bump kotlin first" % (c["artifact"], target.raw, stdlib, catalog_kotlin))
            break
    return result


# --------------------------------------------------------------------------------------------------------------------
# Build scripts, wrapper, GitHub Actions
# --------------------------------------------------------------------------------------------------------------------

COORD = re.compile(r'"([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):(\d[A-Za-z0-9_.+-]*)"')
SETTINGS_PLUGIN = re.compile(r'id\("([^"]+)"\)\s+version\s+"([^"]+)"')
SKIP_DIRS = ("build", ".gradle", ".git", "node_modules", ".claude", ".kotlin")


def hard_coded(root, pins):
    found = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for fn in filenames:
            if not fn.endswith(".gradle.kts"):
                continue
            path = os.path.join(dirpath, fn)
            with open(path) as fh:
                for n, line in enumerate(fh, 1):
                    if line.strip().startswith("//"):
                        continue
                    for m in COORD.finditer(line):
                        g, a, v = m.groups()
                        found.append({"key": "%s:%s" % (g, a), "group": g, "artifact": a, "plugin": False,
                                      "current": v, "file": os.path.relpath(path, root), "line": n})
                    for m in SETTINGS_PLUGIN.finditer(line):
                        pid, v = m.groups()
                        found.append({"key": pid, "group": pid, "artifact": pid + ".gradle.plugin", "plugin": True,
                                      "current": v, "file": os.path.relpath(path, root), "line": n})

    def one(entry):
        result = dict(entry, status="unknown", target=None, kind=None, held=[], errors=[], warnings=[])
        rules = pins.get(entry["key"], [])
        if any(r["rule"] == "weekly" for r in rules):
            result["status"] = "weekly"
            return result
        cur = V(entry["current"])
        try:
            vs, hits = maven_versions(entry["group"], entry["artifact"], entry["plugin"])
        except IOError as e:
            result["errors"].append(str(e))
            return result
        if not hits or not cur.ok:
            result["errors"].append("no metadata" if not hits else "unparseable current version")
            return result
        result["status"] = "ok"
        apply_pins(result, cur, parse_versions(vs), rules)
        return result

    with concurrent.futures.ThreadPoolExecutor(8) as ex:
        return list(ex.map(one, found))


def wrapper_check(root):
    with open(os.path.join(root, "gradle/wrapper/gradle-wrapper.properties")) as fh:
        m = re.search(r"gradle-([0-9][^-]*?)-(?:bin|all)\.zip", fh.read())
    cur = m.group(1) if m else None
    try:
        latest = json.loads(curl("https://services.gradle.org/versions/current"))["version"]
    except (IOError, ValueError, KeyError, TypeError):
        return {"current": cur, "latest": None, "status": "unknown", "update": False, "kind": None}
    update = bool(cur and V(cur) < V(latest))
    return {"current": cur, "latest": latest, "status": "ok", "update": update,
            "kind": update_kind(V(cur), V(latest)) if update else None}


USES = re.compile(r'^\s*-?\s*uses:\s*([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+)(/[^@\s]*)?@([^\s#]+)')


def actions_check(root, pins):
    files = []
    for pattern in (".github/workflows/*.yml", ".github/workflows/*.yaml",
                    ".github/actions/*/action.yml", ".github/actions/*/action.yaml"):
        files.extend(sorted(glob.glob(os.path.join(root, pattern))))
    uses = {}
    for path in files:
        with open(path) as fh:
            for n, line in enumerate(fh, 1):
                m = USES.match(line)
                if m:
                    uses.setdefault(m.group(1), []).append(
                        {"file": os.path.relpath(path, root), "line": n, "ref": m.group(3),
                         "action": m.group(1) + (m.group(2) or "")})

    def one(repo):
        places = uses[repo]
        refs = sorted(set(p["ref"] for p in places))
        result = {"repo": repo, "refs": refs, "places": places, "status": "unknown", "target": None, "kind": None,
                  "held": [], "errors": []}
        rules = pins.get(repo, [])
        if any(r["rule"] == "weekly" for r in rules):
            result["status"] = "weekly"
            return result
        if any(re.match(r"^[0-9a-f]{40}$", r) for r in refs):
            result["errors"].append("pinned to a commit SHA: bump it by hand")
            return result
        try:
            tags = git_tags(repo)
        except (IOError, subprocess.TimeoutExpired) as e:
            result["errors"].append(str(e))
            return result
        result["status"] = "ok"
        stable = [V(t) for t in tags if re.match(r"^v?\d+(\.\d+)*$", t)]
        ceilings = [r for r in rules if r["rule"].startswith("<")]
        allowed = sorted(v for v in stable if all(v < V(c["rule"][1:]) for c in ceilings))
        if not allowed:
            return result
        newest = allowed[-1]
        # Keep each ref's style: a major tag (v6) moves to the newest major tag, a full version to the newest tag.
        major_only = all(re.match(r"^v?\d+$", r) for r in refs)
        prefix = "v" if refs[0].startswith("v") else ""
        target = "%s%d" % (prefix, newest.major()) if major_only else prefix + newest.raw.lstrip("v")
        if major_only and target not in tags:
            target = prefix + newest.raw.lstrip("v")
        oldest = min(V(r) for r in refs)
        if all(r == target for r in refs):
            return result
        if any(r["rule"] == "hold" for r in rules):
            result["held"].append("%s: hold (%s)" % (target, "; ".join(r["why"] for r in rules)))
            return result
        result["target"] = target
        result["kind"] = "major" if oldest.major() < V(target).major() else (
            "minor" if oldest.line() < V(target).line() else "patch")
        return result

    with concurrent.futures.ThreadPoolExecutor(8) as ex:
        return list(ex.map(one, sorted(uses)))


# --------------------------------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------------------------------

KIND_ORDER = {"patch": 0, "minor": 1, "prerelease": 2, "major": 3}


def render(report, show_all):
    out = []
    w = out.append
    w("# Dependency updates (%s)\n" % report["root"])

    refs = report["catalog"]
    upd = [r for r in refs if r["target"]]
    unknown = [r for r in refs if r["errors"]]
    w("## Version catalog: %d update(s), %d held, %d unknown, %d refs checked\n" % (
        len(upd), len([r for r in refs if r["held"]]), len(unknown), len(refs)))
    if upd:
        w("| ref | current | target | kind | artifacts | notes |")
        w("|---|---|---|---|---|---|")
        for r in sorted(upd, key=lambda r: (KIND_ORDER.get(r["kind"], 4), r["ref"])):
            notes = []
            if r.get("same_major_target"):
                notes.append("same major: %s" % r["same_major_target"])
            if r["held"]:
                notes.append("held: " + "; ".join(r["held"]))
            if r["comment"]:
                notes.append("comment above libs.versions.toml:%d" % r["line"])
            w("| %s | %s | %s | %s | %s | %s |" % (r["ref"], r["current"], r["target"], r["kind"],
                                                   ", ".join(r["artifacts"]), "; ".join(notes)))
        w("")
    held = [r for r in refs if r["held"] and not r["target"]]
    if held:
        w("### Held back by pins.txt")
        for r in held:
            w("- `%s` %s: %s" % (r["ref"], r["current"], "; ".join(r["held"])))
        w("")
    if show_all:
        current = [r for r in refs if not r["target"] and not r["held"] and not r["errors"]]
        if current:
            w("Up to date: " + ", ".join("%s %s" % (r["ref"], r["current"]) for r in current) + "\n")
    warnings = [(r["ref"], x) for r in refs for x in r["warnings"]]
    if warnings:
        w("### Warnings")
        for ref, x in warnings:
            w("- `%s`: %s" % (ref, x))
        w("")
    if unknown:
        w("### UNKNOWN (the lookup failed: unchecked, NOT up to date)")
        for r in unknown:
            w("- `%s` %s: %s" % (r["ref"], r["current"], "; ".join(r["errors"])))
        w("")
    if report["weekly"]:
        w("Moved by the weekly workflow, not checked: %s\n" % ", ".join(report["weekly"]))

    hc = [h for h in report["hard_coded"] if h["status"] != "weekly"]
    hc_shown = [h for h in hc if h["target"] or h["held"] or h["errors"] or show_all]
    w("## Hard-coded in build scripts: %d update(s)\n" % len([h for h in hc if h["target"]]))
    if hc_shown:
        w("| coordinate | where | current | target | kind | notes |")
        w("|---|---|---|---|---|---|")
        for h in hc_shown:
            w("| %s | %s:%d | %s | %s | %s | %s |" % (
                h["key"], h["file"], h["line"], h["current"], h["target"] or "-", h["kind"] or "-",
                "; ".join(h["held"] + ["UNKNOWN: " + e for e in h["errors"]])))
        w("")

    wr = report["wrapper"]
    w("## Gradle wrapper: %s, latest %s%s\n" % (
        wr["current"], wr["latest"] or "UNKNOWN", " (update: %s)" % wr["kind"] if wr["update"] else ""))

    acts = [a for a in report["actions"] if a["status"] != "weekly"]
    w("## GitHub Actions: %d update(s)\n" % len([a for a in acts if a["target"]]))
    shown = [a for a in acts if a["target"] or a["held"] or a["errors"] or show_all]
    if shown:
        w("| action | current | target | kind | files to change | notes |")
        w("|---|---|---|---|---|---|")
        for a in shown:
            stale = sorted(set("%s:%d" % (p["file"], p["line"]) for p in a["places"]
                               if a["target"] and p["ref"] != a["target"]))
            w("| %s | %s | %s | %s | %s | %s |" % (
                a["repo"], ", ".join(a["refs"]), a["target"] or "-", a["kind"] or "-", ", ".join(stale),
                "; ".join(a["held"] + ["UNKNOWN: " + e for e in a["errors"]])))
        w("")

    commented = [r for r in upd if r["comment"]]
    if commented:
        w("## Comments above refs with updates (read before bumping)\n")
        for r in commented:
            w("### `%s` (libs.versions.toml:%d)" % (r["ref"], r["line"]))
            w("```")
            w(r["comment"])
            w("```")
    return "\n".join(out)


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--root", default=os.path.abspath(os.path.join(here, "..", "..", "..")))
    ap.add_argument("--json", action="store_true", help="machine-readable output")
    ap.add_argument("--all", action="store_true", help="also list what is up to date")
    args = ap.parse_args()
    root = os.path.abspath(args.root)

    pins = parse_pins(os.path.join(here, "pins.txt"))
    versions, users = parse_catalog(os.path.join(root, "gradle/libs.versions.toml"))
    catalog_kotlin = versions.get("kotlin", {}).get("value")
    weekly = sorted(r for r in versions if r in users and any(p["rule"] == "weekly" for p in pins.get(r, [])))
    refs = [r for r in versions if r in users and r not in weekly]
    with concurrent.futures.ThreadPoolExecutor(12) as ex:
        futures = {r: ex.submit(check_ref, r, versions[r], users[r], pins.get(r, []), catalog_kotlin) for r in refs}
        catalog = [futures[r].result() for r in refs]
    report = {
        "root": root,
        "catalog": catalog,
        "weekly": weekly,
        "unchecked_refs": [r for r in versions if r not in users],
        "hard_coded": hard_coded(root, pins),
        "wrapper": wrapper_check(root),
        "actions": actions_check(root, pins),
    }
    if args.json:
        json.dump(report, sys.stdout, indent=2, default=str)
        sys.stdout.write("\n")
    else:
        print(render(report, args.all))


if __name__ == "__main__":
    main()
