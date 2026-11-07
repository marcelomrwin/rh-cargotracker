# MTA Analysis — Cargo Tracker EAP 7 to EAP 8

This document describes how to use the Migration Toolkit for Applications (MTA) command-line interface to analyze the Cargo Tracker application running on JBoss EAP 7 and identify the changes required to migrate it to JBoss EAP 8.

A pre-generated analysis report is available in the [`mta-report/`](mta-report/index.html) directory.

## About MTA

The Migration Toolkit for Applications (MTA) is a Red Hat tool that analyzes applications and generates detailed reports identifying migration issues, their severity, and guidance on how to resolve them.

For installation instructions, refer to the official documentation:
**"Installing the migration toolkit for applications"** — available at the [Red Hat Customer Portal](https://access.redhat.com/documentation/en-us/migration_toolkit_for_applications).

## Prerequisites

- MTA CLI installed (`mta-cli` binary available in your `$PATH`)
- Podman installed and running
- Logged in to `registry.redhat.io`:
  ```shell
  podman login registry.redhat.io
  ```
- The application binary (`.war` file) or source code available locally

## Running the analysis

### Step 1 — Verify available targets

```shell
mta-cli rules list-targets
```

Confirm that `eap8` and `eap81` appear in the output.

### Step 2 — Build the application

Build the WAR file from the `jee7-eap7` branch:

```shell
mvn clean package -DskipTests
```

The WAR file will be generated at `target/cargo-tracker.war`.

### Step 3 — Run the analysis

```shell
mta-cli analyze \
  --input <path_to_cargo-tracker.war> \
  --output <path_to_output_directory> \
  --source eap7 \
  --target eap8 \
  --target eap81 \
  --run-local=false \
  --overwrite
```

Where:
- `--input`: path to the WAR file or source directory
- `--output`: directory where the analysis report will be generated
- `--source eap7`: the source platform (JBoss EAP 7)
- `--target eap8 --target eap81`: the target platforms (JBoss EAP 8 and 8.1)
- `--run-local=false`: enables container runtime for the analysis engine (requires Podman)
- `--overwrite`: overwrites an existing output directory

> **Note:** When `--run-local=false` is set, the MTA CLI uses container images from `registry.redhat.io` to run the analysis engine. The analysis of non-Java applications always runs in container mode.

### Step 4 — Access the report

When the analysis completes, the CLI prints:

```
Report created: <output_directory>/static-report/index.html
```

Open `index.html` in a browser to review the findings.

## Analysis results summary

The analysis of `cargo-tracker.war` (branch `jee7-eap7`) against targets `eap8` and `eap81` identified **319 incidents** across the following categories:

| Incidents | Category |
|---------:|---------|
| 276 | Java imports: `javax.*` packages must be replaced with `jakarta.*` equivalents |
| 14 | XML deployment descriptors: Java EE namespaces must be updated to Jakarta EE |
| 6 | Hibernate: implicit name determination for sequences/tables has changed |
| 5 | `web.xml`: references to `javax`-prefixed class names must be updated |
| 9 | Maven: `pom.xml` dependencies and namespaces referencing `javax` must be updated |
| 1 | Maven: `javax.activation` groupId must be replaced |

For the complete list of issues with file locations and remediation guidance, see the [MTA report](mta-report/index.html).

## Next steps

After resolving the identified issues, the application can be migrated to JBoss EAP 8.
See the [`jakartaee10-eap8`](https://github.com/marcelomrwin/rh-cargotracker/tree/jakartaee10-eap8) branch for the migrated version.
