#!/usr/bin/env python3
"""During a conflicted upstream merge: resolve conflicts that only concern version pins.

This fork's weekly SDK job and upstream GitLive both edit the same lines (firebase-bom, firebase-ios-sdk and kotlin in
gradle/libs.versions.toml, androidSdk.apiRef in gradle.properties, the npm("firebase") pins of the build files), so
upstream merges often conflict there. A conflict hunk whose two sides consist only of such `key = "version"` /
`key=value` / `npm("name", "version")` lines, for the same keys, is resolved line by line:
  - the keys this fork owns (androidSdk.apiRef, forkVersion) keep this fork's value;
  - every other key takes the higher version of the two sides.
Any other hunk is left for a person. Files without conflict markers left are staged.

usage: resolve_versions.py <file>...   (prints what it resolved; exit 1 if a hunk was left)
"""
import re
import subprocess
import sys
from pathlib import Path

OWNED = {'androidSdk.apiRef', 'forkVersion'}
TOML = re.compile(r'^(\s*)([\w.-]+)(\s*=\s*)"([^"]+)"(\s*(?:#.*)?)$')
PROP = re.compile(r'^()([\w.-]+)(=)([^\s#]+)()$')
NPM = re.compile(r'^(.*npm\(")([^"]+)(",\s*")([^"]+)("\).*)$')
HUNK = re.compile(r'^<<<<<<< [^\n]*\n(.*?)^=======\n(.*?)^>>>>>>> [^\n]*\n', re.M | re.S)


def parse(line):
    """(key, value, rebuild(value) -> line) of a version line, or None."""
    for pattern, key_group, value_group in ((NPM, 2, 4), (TOML, 2, 4), (PROP, 2, 4)):
        match = pattern.match(line)
        if match:
            groups = list(match.groups())
            def rebuild(value, groups=groups, value_index=value_group - 1):
                parts = groups.copy()
                parts[value_index] = value
                return ''.join(parts) if pattern is not TOML else f'{parts[0]}{parts[1]}{parts[2]}"{parts[3]}"{parts[4]}'
            return match.group(key_group), match.group(value_group), rebuild
    return None


def version_key(value):
    return [int(p) if p.isdigit() else -1 for p in re.split(r'[.+-]', value)]


def resolve(ours, theirs):
    ours_lines, theirs_lines = ours.splitlines(), theirs.splitlines()
    parsed_ours = [parse(l) for l in ours_lines if l.strip()]
    parsed_theirs = {p[0]: p for p in (parse(l) for l in theirs_lines if l.strip()) if p}
    if not parsed_ours or None in parsed_ours or len(parsed_theirs) != len([l for l in theirs_lines if l.strip()]):
        return None
    if {p[0] for p in parsed_ours} != set(parsed_theirs):
        return None
    out = []
    for key, value, rebuild in parsed_ours:
        their_value = parsed_theirs[key][1]
        chosen = value if key in OWNED else max(value, their_value, key=version_key)
        out.append(rebuild(chosen))
    return '\n'.join(out) + '\n'


def main():
    left = False
    for name in sys.argv[1:]:
        path = Path(name)
        text = path.read_text()
        resolved_any = False

        def replace(match):
            nonlocal left, resolved_any
            result = resolve(match.group(1), match.group(2))
            if result is None:
                left = True
                return match.group(0)
            resolved_any = True
            return result

        new = HUNK.sub(replace, text)
        if resolved_any:
            path.write_text(new)
            print(f'resolved version pins: {name}')
        if '<<<<<<<' not in new:
            subprocess.run(['git', 'add', name], check=True)
    sys.exit(1 if left else 0)


if __name__ == '__main__':
    main()
