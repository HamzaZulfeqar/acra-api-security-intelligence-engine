#!/usr/bin/env python3
from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
NS={"m":"http://maven.apache.org/POM/4.0.0"}

def deps(path: Path):
    root=ET.parse(path).getroot()
    out=[]
    node=root.find("m:dependencies",NS)
    if node is None:
        return out
    for d in node.findall("m:dependency",NS):
        out.append({
            "groupId":(d.findtext("m:groupId",default="",namespaces=NS) or "").strip(),
            "artifactId":(d.findtext("m:artifactId",default="",namespaces=NS) or "").strip(),
            "version":(d.findtext("m:version",default="",namespaces=NS) or "").strip(),
            "scope":(d.findtext("m:scope",default="compile",namespaces=NS) or "compile").strip(),
        })
    return out

def main() -> None:
    root_deps=deps(ROOT/"pom.xml")
    core_deps=deps(ROOT/"core/pom.xml")
    ext_deps=deps(ROOT/"extension/burp-extension/pom.xml")

    if root_deps:
        raise AssertionError(f"root POM unexpectedly has dependencies: {root_deps}")
    if core_deps:
        raise AssertionError(f"acra-core unexpectedly has direct dependencies: {core_deps}")

    expected={
        ("io.acra","acra-core","${project.version}","compile"),
        ("net.portswigger.burp.extensions","montoya-api","${montoya.version}","provided"),
    }
    actual={(d["groupId"],d["artifactId"],d["version"],d["scope"]) for d in ext_deps}
    if actual!=expected:
        raise AssertionError(f"Burp extension direct dependency contract changed: {sorted(actual)}")

    for path in [ROOT/"pom.xml",ROOT/"core/pom.xml",ROOT/"extension/burp-extension/pom.xml"]:
        text=path.read_text(encoding="utf-8").lower()
        if "<repositories>" in text or "<pluginrepositories>" in text:
            raise AssertionError(f"custom Maven repository introduced: {path}")
        if "<scope>system</scope>" in text:
            raise AssertionError(f"system-scoped dependency introduced: {path}")
        if "-snapshot" in text:
            raise AssertionError(f"SNAPSHOT dependency/version introduced: {path}")

    print("SPRINT15_DEPENDENCY_CONTRACT PASS")
    print("SPRINT15_CORE_DIRECT_DEPENDENCIES count=0")
    print("SPRINT15_EXTENSION_DIRECT_DEPENDENCIES count=2")
    print("SPRINT15_DEPENDENCY io.acra:acra-core:${project.version} scope=compile")
    print("SPRINT15_DEPENDENCY net.portswigger.burp.extensions:montoya-api:${montoya.version} scope=provided")
    print("SPRINT15_CUSTOM_MAVEN_REPOSITORIES count=0")
    print("SPRINT15_SYSTEM_SCOPED_DEPENDENCIES count=0")
    print("SPRINT15_SNAPSHOT_DEPENDENCIES count=0")

if __name__=="__main__":
    main()
