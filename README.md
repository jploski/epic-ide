# EPIC (Eclipse Perl Integration)

EPIC is an open-source Perl IDE for the Eclipse platform. This repository contains the source code, plugins, features, and Tycho build setup for EPIC.

---

## Project Structure

```text
.
├── pom.xml                     # Top-level Maven parent & aggregator POM
├── org.epic.feature.main/      # Feature definition
├── org.epic.repository/        # p2 update-site / repository module
│
├── org.epic.perleditor/        # Core Perl editor plugin
├── org.epic.debug/             # Perl debugger integration plugin
├── org.epic.regexp/            # Regular expression tester plugin
├── org.epic.lib/               # Bundled third-party libraries
├── org.epic.doc/               # User documentation plugin
├── org.epic.source/            # Source bundle packaging
│
└── org.epic.perleditor-test/   # Automated UI & unit tests
```

---

## Prerequisites

* **JDK 17+** (required for build, the minimum source code and output bytecode level is 1.8)
* **Apache Maven 3.9.x**
* **Xvfb** (optional, required for running headless UI tests on Linux)

---

## Building the Project

All builds are managed by the [Eclipse Tycho](https://github.com/eclipse-tycho/tycho) Maven plug-in.

Just run the Maven commands mentioned below from the root directory after Git clone.

### 1. Standard Build (with Tests)
Runs the full build and executes the test suite (this starts and remotely controls a test workbench on your desktop):
```bash
mvn clean verify
```

### 2. Fast Build (Skip Tests)
Builds all plugins, features, and the p2 repository without running the test suite:
```bash
mvn clean verify -DskipTests=true
```

### 3. Headless Build on Linux / CI (with Tests)
As an alternative to (1), runs the tests headless (in a virtual framebuffer, with no visible test workbench):
```bash
xvfb-run -a mvn clean verify
```

---

## Build Output

After a successful build, the p2 update site (installable in Eclipse via **Help → Install New Software...**) is generated in:

```text
org.epic.repository/target/repository/
```

A zipped version of the update site is also generated in `org.epic.repository/target/`.

---

## Release Steps

### Version Bump (Before Build)

To update the version across all `pom.xml`, `MANIFEST.MF`, `feature.xml`, and `category.xml` files simultaneously, use the Tycho versions plugin:

```bash
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<NEW_VERSION>-SNAPSHOT
```

*Example:*
```bash
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=0.7.12-SNAPSHOT
```
### Update Site Upload (After Build)

1. Upload the folder `org.epic.repository/target/repository/` to `htdocs/updates/testing/x.y.z` (x.y.z = version number, e.g. 0.7.12)
2. Register the new version number in `htdocs/updates/testing/compositeArtifacts.xml` and `htdocs/updates/testing/compositeContent.xml`.