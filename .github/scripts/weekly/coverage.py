#!/usr/bin/env python3
"""Summarise the Firebase Android SDK API coverage of every module, from the `api/android-sdk-compat.txt` reports that
`./gradlew :<module>:androidSourceCompatDump` writes.

A report lists each public member of the Android SDK's api.txt with a status: OK (provided as is), MAP (provided with a
mapped type), MISS (not provided), OMIT (deliberately not mirrored; counts as unavailable), and uncounted ones such as
PLAT (the signature needs an Android/JVM-only type) or HIDE (hidden in the Android SDK). Coverage is
(OK + MAP) / (OK + MAP + MISS + OMIT).

Usage (from the repository root):
    python3 .github/scripts/weekly/coverage.py                     # print the table
    python3 .github/scripts/weekly/coverage.py --readme README.md   # rewrite the summary and table between the
                                                                    # coverage markers; platforms come from each
                                                                    # module's supportedTargets in gradle.properties
    python3 .github/scripts/weekly/coverage.py --compare origin/main --summary FILE
                                       # Markdown with the change against a git ref and every member that became
                                       # MISS/OMIT, for a pull request body
"""
import argparse
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
REPORT = 'api/android-sdk-compat.txt'
PLATFORMS = [('android', 'Android'), ('ios', 'iOS'), ('macos', 'macOS'), ('tvos', 'tvOS'), ('jvm', 'JVM'), ('js', 'JS'),
             ('wasmjs', 'Wasm')]
HEADER = re.compile(r'^# (\d+)% of (\d+) public members available \((\d+) identical, (\d+) mapped, (\d+) missing, (\d+) omitted\)')
START, END = '<!-- coverage:start -->', '<!-- coverage:end -->'
PRODUCTS = {
    'firebase-ai': ('AI Logic', 'https://firebase.google.com/docs/ai-logic'),
    'firebase-analytics': ('Analytics', 'https://firebase.google.com/docs/analytics'),
    'firebase-app': ('App (core)', 'https://firebase.google.com/docs/reference/android/com/google/firebase/FirebaseApp'),
    'firebase-appcheck': ('App Check', 'https://firebase.google.com/docs/app-check'),
    'firebase-appdistribution': ('App Distribution', 'https://firebase.google.com/docs/app-distribution'),
    'firebase-auth': ('Authentication', 'https://firebase.google.com/docs/auth'),
    'firebase-config': ('Remote Config', 'https://firebase.google.com/docs/remote-config'),
    'firebase-crashlytics': ('Crashlytics', 'https://firebase.google.com/docs/crashlytics'),
    'firebase-database': ('Realtime Database', 'https://firebase.google.com/docs/database'),
    'firebase-dataconnect': ('Data Connect', 'https://firebase.google.com/docs/data-connect'),
    'firebase-firestore': ('Cloud Firestore', 'https://firebase.google.com/docs/firestore'),
    'firebase-functions': ('Cloud Functions', 'https://firebase.google.com/docs/functions'),
    'firebase-inappmessaging': ('In-App Messaging', 'https://firebase.google.com/docs/in-app-messaging'),
    'firebase-installations': ('Installations', 'https://firebase.google.com/docs/projects/manage-installations'),
    'firebase-messaging': ('Cloud Messaging', 'https://firebase.google.com/docs/cloud-messaging'),
    'firebase-ml-modeldownloader': ('ML Model Downloader', 'https://firebase.google.com/docs/ml/android/use-custom-models'),
    'firebase-perf': ('Performance Monitoring', 'https://firebase.google.com/docs/perf-mon'),
    'firebase-storage': ('Cloud Storage', 'https://firebase.google.com/docs/storage'),
}


def parse(text):
    """(header numbers, {(class, member): status}) of a report's text."""
    header, members, cls = None, {}, None
    for line in text.splitlines():
        match = HEADER.match(line)
        if match:
            header = tuple(int(g) for g in match.groups())
        elif line.startswith('#') or not line.strip():
            continue
        elif not line.startswith(' '):
            cls = line.split('  ')[0].strip()
        else:
            status, _, member = line.strip().partition(' ')
            members[(cls, member.strip())] = status
    return header, members


def reports(ref=None):
    if ref is None:
        return {p.parent.parent.name: p.read_text() for p in sorted(ROOT.glob(f'*/{REPORT}'))}
    names = subprocess.run(['git', 'ls-tree', '-r', '--name-only', ref], cwd=ROOT, capture_output=True, text=True, check=True).stdout.split()
    return {n.split('/')[0]: subprocess.run(['git', 'show', f'{ref}:{n}'], cwd=ROOT, capture_output=True, text=True, check=True).stdout
            for n in names if n.endswith(REPORT) and n.count('/') == 2}


def badge(percent):
    colour = 'brightgreen' if percent == 100 else 'green' if percent > 60 else 'orange'
    return f'![{percent}%](https://img.shields.io/badge/-{percent}%25-{colour}?style=flat-square)'


def toml_version(key):
    match = re.search(rf'^{re.escape(key)}\s*=\s*"([^"]+)"', (ROOT / 'gradle' / 'libs.versions.toml').read_text(), flags=re.M)
    return match.group(1) if match else '?'


def js_sdk_version():
    match = re.search(r'npm\("firebase",\s*"([^"]+)"\)', (ROOT / 'firebase-common' / 'build.gradle.kts').read_text())
    return match.group(1) if match else '?'


def gradle_property(key):
    match = re.search(rf'^{re.escape(key)}=(.*)$', (ROOT / 'gradle.properties').read_text(), flags=re.M)
    return match.group(1).strip() if match else None


def table(current):
    """The summary line and the table of every module: its Android API coverage and the platforms it supports."""
    lines = ['| Product | Artifact | Android API | Members | ' + ' | '.join(label for _, label in PLATFORMS) + ' |',
             '|---|---|:---:|:---:|' + ':---:|' * len(PLATFORMS)]
    products = available_total = total_total = 0
    for module, text in current.items():
        header, _ = parse(text)
        if header is None:
            continue
        percent, total, identical, mapped, missing, omitted = header
        products += 1
        available_total += identical + mapped
        total_total += total
        name, url = PRODUCTS.get(module, (module, ''))
        product = f'[{name}]({url})' if url else name
        targets = (gradle_property(f'{module}.supportedTargets') or '').split(',')
        platforms = ' | '.join('✅' if key in targets else '–' for key, _ in PLATFORMS)
        lines.append(f'| {product} | `{module}` | [{badge(percent)}]({module}/{REPORT}) | {identical + mapped} / {total} | '
                     f'{platforms} |')
    ref = gradle_property('androidSdk.apiRef') or 'main'
    percent = available_total * 100 // total_total if total_total else 0
    summary = (f'**{products} Firebase products: {available_total:,} of {total_total:,} Android API members available from '
               f'common code ({percent}%)**, measured against firebase-android-sdk '
               f'[`{ref[:7]}`](https://github.com/firebase/firebase-android-sdk/tree/{ref}). Built on the Firebase Android '
               f'BoM {toml_version("firebase-bom")} (App Distribution {toml_version("firebase-appdistribution")}), '
               f'iOS SDK {toml_version("firebase-ios-sdk")} and JS SDK {js_sdk_version()}.')
    return summary + '\n\n' + '\n'.join(lines)


def compare(current, previous, ref):
    out = [f'### Android API coverage against `{ref}`', '', '| Module | Before | After |', '|---|---:|---:|']
    regressions = []
    for module in sorted(set(current) | set(previous)):
        now, then = parse(current.get(module, '')), parse(previous.get(module, ''))
        after = f'{now[0][0]}% of {now[0][1]}' if now[0] else '-'
        before = f'{then[0][0]}% of {then[0][1]}' if then[0] else '-'
        out.append(f'| {module} | {before} | {after} |')
        for key, status in now[1].items():
            if status in ('MISS', 'OMIT') and then[1].get(key) != status:
                regressions.append(f'- `{module}` {status} `{key[0]}#{key[1]}`')
    out += ['', f'### Newly missing or omitted members ({len(regressions)})', '']
    out += regressions or ['None.']
    return '\n'.join(out) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--readme', type=Path, help='rewrite the table between the coverage markers of this file')
    parser.add_argument('--compare', metavar='REF', help='git ref to compare the reports against')
    parser.add_argument('--summary', type=Path, help='write the comparison as Markdown to this file')
    args = parser.parse_args()
    current = reports()
    rendered = table(current)
    print(rendered)
    if args.readme:
        text = args.readme.read_text()
        before, start, rest = text.partition(START)
        _, end, after = rest.partition(END)
        if not start or not end:
            raise SystemExit(f'{args.readme} has no {START} ... {END} section')
        args.readme.write_text(f'{before}{START}\n{rendered}\n{END}{after}')
    if args.compare:
        markdown = compare(current, reports(args.compare), args.compare)
        if args.summary:
            args.summary.write_text(markdown)
        else:
            print(markdown)


if __name__ == '__main__':
    main()
