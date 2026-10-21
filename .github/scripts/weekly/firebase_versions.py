#!/usr/bin/env python3
"""Find the latest Firebase SDK releases and, with --write, move this build onto them.

What is tracked, and where it lives:
  Android  com.google.firebase:firebase-bom        gradle/libs.versions.toml  firebase-bom
  Android  firebase-appdistribution(-api), beta    gradle/libs.versions.toml  firebase-appdistribution
  iOS      firebase/firebase-ios-sdk (SwiftPM)     gradle/libs.versions.toml  firebase-ios-sdk
                                                   */apple/*/Package.swift    (the Objective-C wrapper packages)
  JS       the `firebase` npm package              npm("firebase", "x.y.z") in the module build files
  API      firebase-android-sdk api.txt revision   gradle.properties          androidSdk.apiRef

The api.txt revision is the newest "m<NNN> mergeback" commit on firebase-android-sdk's main branch: the point where a
Firebase Android release (the BoM's libraries) was merged back, so the API compared against is the released one rather
than whatever main has gained since. App Distribution is not in the BoM: it moves to the version that the same revision
records as released (`latestReleasedVersion` in firebase-appdistribution-api/gradle.properties), so its library and its
api.txt match.

The summary also lists firebase-android-sdk modules with a public api.txt that no module here is compared against and
that are not known below, which is how a new Firebase product shows up.

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
SWIFT_PIN = re.compile(r'(\.package\(url: "https://github\.com/firebase/firebase-ios-sdk\.git", from: )"[^"]+"')
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


def prerelease_key(version):
    """Orders 16.0.0-beta9 < 16.0.0-beta21 < 16.0.0."""
    base, _, pre = version.partition('-')
    return version_key(base), 0 if pre else 1, int(re.sub(r'\D', '', pre) or 0)


GOOGLE_MAVEN = 'https://dl.google.com/dl/android/maven2/com/google/firebase'


def latest_bom():
    metadata = fetch(f'{GOOGLE_MAVEN}/firebase-bom/maven-metadata.xml', 'application/xml')
    versions = [v for v in re.findall(r'<version>([^<]+)</version>', metadata) if STABLE.match(v)]
    return max(versions, key=version_key)


def latest_appdistribution(api_ref):
    """The App Distribution release that the api.txt at api_ref describes, if published; else the newest published."""
    metadata = fetch(f'{GOOGLE_MAVEN}/firebase-appdistribution-api/maven-metadata.xml', 'application/xml')
    published = re.findall(r'<version>([^<]+)</version>', metadata)
    properties = fetch(f'https://raw.githubusercontent.com/firebase/firebase-android-sdk/{api_ref}/firebase-appdistribution-api/gradle.properties', 'text/plain')
    released = re.search(r'^latestReleasedVersion=(\S+)', properties, flags=re.M)
    if released and released.group(1) in published:
        return released.group(1)
    return max(published, key=prerelease_key)


# api.txt files of firebase-android-sdk that no module is compared against, on purpose or not yet.
KNOWN_UNTRACKED = {
    'ai-logic/firebase-ai-ondevice/api.txt': 'AI Logic on-device inference: a beta library outside the BoM, its API deprecated for GenerativeModel.onDeviceExtension',
    'firebase-ml-modeldownloader/api.txt': 'ML Model Downloader, not planned: gone from iOS SDK 13 and BoM 35',
    'appcheck/firebase-appcheck-debug-testing/api.txt': 'an Android instrumentation-test helper',
    'firebase-sessions/api.txt': 'internal to Crashlytics and Performance Monitoring',
}
# Build infrastructure rather than products: encoders, transport, components, interop and processor modules.
INFRASTRUCTURE = re.compile(r'^(encoders|transport)/|^firebase-(abt|annotations|components|database-collection|datatransport)/'
                            r'|^protolite-well-known-types/|-interop/api\.txt$|-ksp-processor/api\.txt$')
API_MEMBER = re.compile(r'^\s+(method|ctor|field|property|enum_constant) ', flags=re.M)


def untracked_android_apis(api_ref):
    """firebase-android-sdk api.txt files with public members that no module's registerAndroidSourceCompat names."""
    tree = json.loads(fetch(f'https://api.github.com/repos/firebase/firebase-android-sdk/git/trees/{api_ref}?recursive=1'))
    paths = {entry['path'] for entry in tree['tree'] if entry['path'].endswith('/api.txt')}
    tracked = {path for build in ROOT.glob('*/build.gradle.kts') for path in re.findall(r'"([\w./-]+/api\.txt)"', build.read_text())}
    untracked = []
    for path in sorted(paths - tracked - KNOWN_UNTRACKED.keys()):
        if INFRASTRUCTURE.search(path):
            continue
        members = len(API_MEMBER.findall(fetch(f'https://raw.githubusercontent.com/firebase/firebase-android-sdk/{api_ref}/{path}', 'text/plain')))
        if members:  # registrar-only modules (crashlytics-ndk, messaging-directboot, ...) have none
            untracked.append((path, members))
    return untracked


def firebase_common_of(artifact, version):
    """The firebase-common version that a Firebase artifact's POM (or the BoM) names."""
    pom = fetch(f'{GOOGLE_MAVEN}/{artifact}/{version}/{artifact}-{version}.pom', 'application/xml')
    match = re.search(r'<artifactId>firebase-common</artifactId>\s*<version>([^<]+)</version>', pom)
    return match.group(1) if match else None


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
        ('App Distribution', toml_value('firebase-appdistribution'), latest_appdistribution(api_sha), 'beta, not in the BoM'),
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
            elif name == 'App Distribution':
                set_toml_value('firebase-appdistribution', latest)
            elif name == 'iOS SDK':
                set_toml_value('firebase-ios-sdk', latest)
                # The Objective-C wrapper packages of the Swift-only SDKs (Data Connect, AI Logic) depend on
                # firebase-ios-sdk from the same minimum, so that SwiftPM resolves one version for every module.
                for package in sorted(ROOT.glob('*/apple/*/Package.swift')):
                    package.write_text(SWIFT_PIN.sub(rf'\g<1>"{latest}"', package.read_text()))
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
        untracked = untracked_android_apis(api_sha)
        if untracked:
            lines += ['', '**Android SDK modules no module covers yet** (a new product, or a new part of one):', '']
            lines += [f'- `{path}`: {members} public members' for path, members in untracked]
        latest = {name: latest for name, _, latest, _ in rows}
        bom_common = firebase_common_of('firebase-bom', latest['Android BoM'])
        appdistribution_common = firebase_common_of('firebase-appdistribution-api', latest['App Distribution'])
        if bom_common and appdistribution_common and bom_common.split('.')[0] != appdistribution_common.split('.')[0]:
            lines += ['', f'**App Distribution lags the BoM:** {latest["App Distribution"]} is built on firebase-common '
                          f'{appdistribution_common}, BoM {latest["Android BoM"]} on {bom_common}. Keep the previous BoM or '
                          'wait for an App Distribution release on the new firebase-common.']
        args.summary.write_text('\n'.join(lines) + '\n')

    if 'GITHUB_OUTPUT' in os.environ:
        with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
            out.write(f'changed={"true" if changed else "false"}\n')
            out.write(f'android_api_changed={"true" if any(r[0].startswith("Android API") for r in changed) else "false"}\n')


if __name__ == '__main__':
    main()
