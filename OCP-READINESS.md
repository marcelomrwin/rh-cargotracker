# OpenShift Cloud Readiness Analysis

This document describes the MTA analysis performed to assess the cloud readiness of the Cargo Tracker application before deploying it to OpenShift Container Platform.

This is the second MTA analysis in the migration journey:

| Phase | Source | Target | Purpose |
|-------|--------|--------|---------|
| 1 | `eap7` | `eap8`, `eap81` | Identify changes required to migrate from JBoss EAP 7 to EAP 8.1 |
| 2 | `eap8` | `cloud-readiness` | Identify changes required to containerize the EAP 8.1 app for OpenShift |

---

## Running the analysis

```shell
mta-cli analyze \
  --input <path_to_cargo-tracker.war> \
  --output <path_to_output> \
  --source eap8 \
  --target cloud-readiness \
  --run-local=false \
  --overwrite
```

---

## Results

The analysis identified **3 incidents** across 1 rule:

| Incidents | Rule | Description |
|----------:|------|-------------|
| 3 | `localhost-http-00001` | Local HTTP Calls - the application accesses local resources by HTTP |

### Details

All 3 incidents point to the same issue: the `webapp.graphTraversalUrl` property references `localhost`, which prevents the application from scaling horizontally in OpenShift. When multiple pods are running, each pod would call itself instead of routing through the OpenShift Service.

Affected locations:
- `pom.xml` line 49 (default property value)
- `pom.xml` line 226 (Payara profile override)
- `src/main/webapp/WEB-INF/web.xml` line 66 (substituted value in descriptor)

---

## Changes applied

The fix was already applied in this branch by replacing the hardcoded `localhost` with an environment variable that defaults to `localhost` for local testing:

**Before (`pom.xml`):**
```xml
<webapp.graphTraversalUrl>
    http://localhost:8080/cargo-tracker/rest/graph-traversal/shortest-path
</webapp.graphTraversalUrl>
```

**After (`pom.xml`):**
```xml
<webapp.graphTraversalUrl>
    ${env.CARGO_ENDPOINT:http://localhost:8080}/cargo-tracker/rest/graph-traversal/shortest-path
</webapp.graphTraversalUrl>
```

At runtime on OpenShift, the `CARGO_ENDPOINT` environment variable is set to the OpenShift Service name, allowing the application to route requests correctly across pods.

Additionally, EAP 8 does not allow variable substitution in deployment descriptors by default. This is enabled in the container image via `jboss-cli.sh`:

```shell
/subsystem=ee:write-attribute(name=spec-descriptor-property-replacement,value=true)
```

This command is already included in the `ocp/s2i/Dockerfile` used for the OpenShift build.

For the full OpenShift deployment procedure, see [ocp-deployment.md](ocp-deployment.md).
