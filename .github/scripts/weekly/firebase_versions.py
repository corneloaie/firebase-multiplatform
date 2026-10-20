#!/usr/bin/env python3
"""Find the latest Firebase SDK releases and, with --write, move this build onto them.

What is tracked, and where it lives:
  Android  com.google.firebase:firebase-bom        gradle/libs.versions.toml  firebase-bom
  iOS      firebase/firebase-ios-sdk (SwiftPM)     gradle/libs.versions.toml  firebase-ios-sdk
  JS       the `firebase` npm package              npm("firebase", "x.y.z") in the module build files
  API      firebase-android-sdk api.txt revision   gradle.properties          androidSdk.apiRef

The api.txt revision is the newest "m<NNN> mergeback" commit on firebase-android-sdk's main branch: the point where a
Firebase Android release (the BoM's libraries) was merged back, so the API compared against is the released one rather
than whatever main has gained since.

Usage (from the repository root):
    python3 .github/scripts/weekly/firebase_versions.py           # print current -> latest
    python3 .github/scripts/weekly/firebase_versions.py --write   # update the files
    ... --summary FILE    also write a Markdown summary (for the pull request body)

Set GITHUB_TOKEN to raise the GitHub API rate limit. With GITHUB_OUTPUT set (in Actions), writes `changed=true|false`.
"""
import argparse
import json
import os
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TOML = ROOT / 'gradle' / 'libs.versions.toml'
PROPERTIES = ROOT / 'gradle.properties'
NPM_PIN = re.compile(r'npm\("firebase",\s*"([^"]+)"\)')
STABLE = re.compile(r'^\d+\.\d+\.\d+$')


def fetch(url, accept='application/json'):
    request = urllib.request.Request(url, headers={'Accept': accept, 'User-Agent': 'firebase-multiplatform-weekly'})
    token = os.environ.get('GITHUB_TOKEN')
    if token and url.startswith('https://api.github.com/'):
        request.add_header('Authorization', f'Bearer {token}')
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read().decode()


def version_key(version):
    return tuple(int(part) for part in version.split('.'))


def latest_bom():
    metadata = fetch('https://dl.google.com/dl/android/maven2/com/google/firebase/firebase-bom/maven-metadata.xml', 'application/xml')
    versions = [v for v in re.findall(r'<version>([^<]+)</version>', metadata) if STABLE.match(v)]
    return max(versions, key=version_key)


def latest_ios():
    releases = json.loads(fetch('https://api.github.com/repos/firebase/firebase-ios-sdk/releases?per_page=30'))
    versions = [r['tag_name'].lstrip('v') for r in releases if not r['prerelease'] and not r['draft'] and STABLE.match(r['tag_name'].lstrip('v'))]
    return max(versions, key=version_key)


def latest_js():
    return json.loads(fetch('https://registry.npmjs.org/firebase'))['dist-tags']['latest']


def latest_android_api_ref():
    """The newest `m<NNN> mergeback` commit on firebase-android-sdk main, as (sha, milestone, date)."""
    for page in range(1, 6):
        commits = json.loads(fetch(f'https://api.github.com/repos/firebase/firebase-android-sdk/commits?sha=main&per_page=100&page={page}'))
        for commit in commits:
            title = commit['commit']['message'].split('\n')[0]
            match = re.match(r'^(m\d+) mergeback\b', title, flags=re.IGNORECASE)
            if match:
                return commit['sha'], match.group(1), commit['commit']['committer']['date'][:10]
    sys.exit('no "mNNN mergeback" commit found in the last 500 commits of firebase-android-sdk main')


def toml_value(key):
    return re.search(rf'^{re.escape(key)}\s*=\s*"([^"]+)"', TOML.read_text(), flags=re.M).group(1)


def set_toml_value(key, value):
    text = TOML.read_text()
    TOML.write_text(re.sub(rf'^({re.escape(key)}\s*=\s*)"[^"]+"', rf'\g<1>"{value}"', text, count=1, flags=re.M))


def property_value(key):
    return re.search(rf'^{re.escape(key)}=(.*)$', PROPERTIES.read_text(), flags=re.M).group(1).strip()


def set_property_value(key, value):
    text = PROPERTIES.read_text()
    PROPERTIES.write_text(re.sub(rf'^({re.escape(key)}=).*$', rf'\g<1>{value}', text, count=1, flags=re.M))


def build_files():
    return sorted(p for p in ROOT.glob('*/build.gradle.kts') if NPM_PIN.search(p.read_text()))


def current_js():
    pins = {m for p in build_files() for m in NPM_PIN.findall(p.read_text())}
    if len(pins) != 1:
        print(f'warning: the npm("firebase") pins disagree: {sorted(pins)}', file=sys.stderr)
    return max(pins, key=version_key)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--write', action='store_true', help='update the build files')
    parser.add_argument('--summary', type=Path, help='write a Markdown summary to this file')
    args = parser.parse_args()

    api_sha, milestone, api_date = latest_android_api_ref()
    rows = [
        # name, current, latest, note
        ('Android BoM', toml_value('firebase-bom'), latest_bom(), 'com.google.firebase:firebase-bom'),
        ('iOS SDK', toml_value('firebase-ios-sdk'), latest_ios(), 'SwiftPM minimum; resolves to the newest release of that major'),
        ('JS SDK', current_js(), latest_js(), 'npm firebase'),
        ('Android API (api.txt)', property_value('androidSdk.apiRef'), api_sha, f'firebase-android-sdk {milestone} mergeback, {api_date}'),
    ]
    changed = [row for row in rows if row[1] != row[2]]

    for name, current, latest, note in rows:
        marker = '->' if current != latest else '=='
        print(f'{name:24} {current[:12]:>14} {marker} {latest[:12]:<14} {note}')

    if args.write:
        for name, current, latest, _ in changed:
            if name == 'Android BoM':
                set_toml_value('firebase-bom', latest)
            elif name == 'iOS SDK':
                set_toml_value('firebase-ios-sdk', latest)
            elif name == 'JS SDK':
                for path in build_files():
                    path.write_text(NPM_PIN.sub(f'npm("firebase", "{latest}")', path.read_text()))
            elif name.startswith('Android API'):
                set_property_value('androidSdk.apiRef', latest)

    if args.summary:
        lines = ['| SDK | Current | Latest | |', '|---|---|---|---|']
        for name, current, latest, note in rows:
            shown = (lambda v: v[:12] if name.startswith('Android API') else v)
            lines.append(f'| {name} | `{shown(current)}` | `{shown(latest)}` | {"**updated** " if current != latest else ""}{note} |')
        majors = [name for name, current, latest, _ in changed if not name.startswith('Android API') and current.split('.')[0] != latest.split('.')[0]]
        if majors:
            lines += ['', f'**Major version change:** {", ".join(majors)}. Expect breaking API changes; CI shows what no longer compiles.']
        args.summary.write_text('\n'.join(lines) + '\n')

    if 'GITHUB_OUTPUT' in os.environ:
        with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
            out.write(f'changed={"true" if changed else "false"}\n')
            out.write(f'android_api_changed={"true" if any(r[0].startswith("Android API") for r in changed) else "false"}\n')


if __name__ == '__main__':
    main()
