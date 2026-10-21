#!/usr/bin/env python3
"""During a conflicted merge of an upstream compat branch: resolve Apple source conflicts.

For every conflicted file under firebase-<module>/src/{apple,ios,tvos,macos}*Main whose v3 change
(merge-base -> HEAD) only rewrote `cocoapods.*` imports to `swiftPMImport.*`, take the branch's version
(or its deletion). Then rewrite every remaining `cocoapods.FirebaseX.` reference in the module's
Apple sources to `swiftPMImport.dev.gitlive.firebase.<module>.`. Prints anything it could not resolve.

usage: resolve_apple.py <branch> <module>
"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
branch, module = sys.argv[1], sys.argv[2]
APPLE = re.compile(rf'^firebase-{re.escape(module)}/src/(apple|ios|tvos|macos)[A-Za-z]*/')


def git(*args, check=True):
    return subprocess.run(['git', *args], cwd=ROOT, capture_output=True, text=True, check=check).stdout


def only_import_rewrites(path, base):
    diff = git('diff', '-U0', base, 'HEAD', '--', path)
    for line in diff.splitlines():
        if line.startswith(('+++', '---', '@@', 'diff ', 'index ')):
            continue
        if line.startswith(('+', '-')):
            body = line[1:].strip()
            if not (body.startswith('import cocoapods.') or body.startswith('import swiftPMImport.') or body == ''):
                return False
    return True


base = git('merge-base', 'HEAD', branch).strip()
conflicted = git('diff', '--name-only', '--diff-filter=U').split()
unresolved = []
for path in conflicted:
    if not APPLE.match(path):
        continue
    if not only_import_rewrites(path, base):
        unresolved.append(path)
        continue
    exists_theirs = subprocess.run(['git', 'cat-file', '-e', f'{branch}:{path}'], cwd=ROOT).returncode == 0
    if exists_theirs:
        (ROOT / path).write_text(git('show', f'{branch}:{path}'))
        git('add', path)
    else:
        git('rm', '-q', '--', path)
    print(f'took branch version: {path}' if exists_theirs else f'took branch deletion: {path}')

# FirebaseCore (FIRApp, FIRTimestamp, ...) is imported by firebase-app; every other pod by the module itself.
core = re.compile(r'\bcocoapods\.FirebaseCore\.')
pattern = re.compile(r'\bcocoapods\.Firebase[A-Za-z]+\.')
target = f'swiftPMImport.dev.gitlive.firebase.{module}.'
for src in (ROOT / f'firebase-{module}' / 'src').glob('*/kotlin/**/*.kt'):
    text = src.read_text()
    if 'cocoapods.' in text:
        new = pattern.sub(target, core.sub('swiftPMImport.dev.gitlive.firebase.app.', text))
        if new != text:
            src.write_text(new)
            print(f'rewrote imports: {src.relative_to(ROOT)}')
        if 'cocoapods.' in new:
            unresolved.append(f'{src.relative_to(ROOT)} (still references cocoapods)')

for path in unresolved:
    print(f'UNRESOLVED: {path}')
