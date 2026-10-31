---
name: add-dependency
description: Evaluate and add a new Maven dependency. Use whenever a task seems to need a library that is not already in pom.xml.
---

# Add a dependency

Argument: the library or need (e.g. "WireMock", "HTTP retry").

## 1. Is it needed at all?
- Check whether Spring Boot, the JDK or an existing dependency already covers it
  (e.g. RestClient + a simple retry loop vs. a retry library).
- If it is borderline, propose the no-dependency option first.

## 2. Is the version managed by the Spring Boot BOM?
- Run `mvn help:effective-pom -q -Doutput=target/effective-pom.xml` and search
  `<dependencyManagement>` for the artifact.
- If it's managed: add it WITHOUT a `<version>` and skip to step 4.
- If its project ships its own BOM (e.g. Spring Modulith, Testcontainers): import that BOM
  in `<dependencyManagement>` and don't version the individual artifacts.

## 3. If it's not managed: verify compatibility from sources, not from memory
- Use the library's official docs, release notes or compatibility matrix. Cite the URL.
- Confirm it supports ALL of these:
    - Java 25
    - Spring Boot 4.x / Spring Framework 7
    - Jakarta EE namespace (`jakarta.*`, not `javax.*`)
    - the Jackson major version Boot 4 uses, if the library touches JSON
      (check `mvn dependency:tree` afterwards for a second Jackson major on the classpath)
- Pick the newest stable version that satisfies all of these. If you can't confirm one
  of them, say so explicitly. Don't guess.

## 4. Present it, then STOP
- Report: the artifact, its scope (compile / test / runtime), whether the version is managed
  or pinned (and why), the source links from step 3, and the alternative you rejected.
- Wait for my approval.

## 5. After approval: prove it
- Add it to pom.xml.
- Run `mvn dependency:tree -Dincludes=<groupId>` and report any version conflicts
  or duplicate libraries.
- Run `mvn verify`. If it's red because of the dependency, report it and remove the dependency.
  Don't work around it.