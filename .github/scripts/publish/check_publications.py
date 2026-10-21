#!/usr/bin/env python3
"""Checks the publications in a Maven repository before they go to Maven Central, where a release is permanent.

    python3 .github/scripts/publish/check_publications.py ~/.m2/repository

Run after `./gradlew :<module>:publishToMavenLocal` for every module (the Publish workflow does). It fails when:
  - a module that applies the publish plugin has no publication under io/github/corneloaie, or the versions differ;
  - a POM or Gradle module file refers to a sibling module under GitLive's group (dev.gitlive) instead of the fork's,
    which would make consumers resolve GitLive's artifacts. dev.gitlive:firebase-java-sdk is a real dependency;
  - a POM names a developer other than the fork's maintainer, or points its url or scm at GitLive's repository.
"""
import json
import os
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
GROUP = 'io.github.corneloaie'
UPSTREAM_GROUP = 'dev.gitlive'
UPSTREAM_ALLOWED = {'firebase-java-sdk'}
DEVELOPER = 'corneloaie'
FORK_URL = 'https://github.com/corneloaie/firebase-multiplatform'
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}


def published_modules():
    return sorted(p.parent.name for p in ROOT.glob('*/build.gradle.kts') if 'alias(libs.plugins.publish)' in p.read_text())


def check_pom(path, problems):
    pom = ElementTree.parse(path).getroot()
    for dependency in pom.iterfind('.//m:dependency', NS):
        group = dependency.findtext('m:groupId', namespaces=NS)
        artifact = dependency.findtext('m:artifactId', namespaces=NS)
        if group == UPSTREAM_GROUP and artifact not in UPSTREAM_ALLOWED:
            problems.append(f'{path.name}: depends on {group}:{artifact}')
    developers = [d.findtext('m:id', namespaces=NS) for d in pom.iterfind('.//m:developers/m:developer', NS)]
    if developers != [DEVELOPER]:
        problems.append(f'{path.name}: developers are {developers}, expected [{DEVELOPER!r}]')
    for field in ('m:url', 'm:scm/m:url'):
        if pom.findtext(field, namespaces=NS) != FORK_URL:
            problems.append(f'{path.name}: {field[2:]} is {pom.findtext(field, namespaces=NS)!r}')


def check_module(path, problems):
    module = json.loads(path.read_text())
    for variant in module.get('variants', []):
        for dependency in variant.get('dependencies', []) + variant.get('dependencyConstraints', []):
            if dependency.get('group') == UPSTREAM_GROUP and dependency.get('module') not in UPSTREAM_ALLOWED:
                problems.append(f'{path.name}: variant {variant["name"]} depends on {dependency["group"]}:{dependency["module"]}')
        available = variant.get('available-at')
        if available and available.get('group') != GROUP:
            problems.append(f'{path.name}: variant {variant["name"]} is available at {available.get("group")}:{available.get("module")}')


def main():
    repository = Path(sys.argv[1]).expanduser() / Path(*GROUP.split('.'))
    problems = []
    versions = {}
    artifacts = sorted(p for p in repository.iterdir() if p.is_dir()) if repository.is_dir() else []
    for artifact in artifacts:
        for version in (v for v in artifact.iterdir() if v.is_dir()):
            versions.setdefault(version.name, []).append(artifact.name)
            poms = list(version.glob('*.pom'))
            modules = list(version.glob('*.module'))
            if not poms:
                problems.append(f'{artifact.name} {version.name}: no POM')
            for pom in poms:
                check_pom(pom, problems)
            for module in modules:
                check_module(module, problems)
    names = {a.name for a in artifacts}
    for module in published_modules():
        if module not in names:
            problems.append(f'{module}: not published')
    if len(versions) > 1:
        problems.append(f'more than one version: { {v: len(a) for v, a in versions.items()} }')

    print(f'{len(artifacts)} artifacts under {GROUP}, version {", ".join(versions) or "-"}, from {len(published_modules())} modules')
    for problem in problems:
        print(f'::error::{problem}' if 'GITHUB_ACTIONS' in os.environ else f'error: {problem}')
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
