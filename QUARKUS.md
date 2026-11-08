# Migrating Cargo Tracker to Quarkus

## Why Quarkus?

The Cargo Tracker application has already been migrated from EAP 7.4 to EAP 8.1 and is running in a container on OpenShift. The next step in the modernization journey is to convert it to Quarkus - a Kubernetes-native Java framework designed for cloud environments.

Quarkus offers:
- Fast startup and low memory footprint compared to traditional application servers
- Developer-first experience with live reload
- Native binary compilation via GraalVM
- Built-in support for cloud-native patterns (health, metrics, tracing, config)
- No application server required - the framework is embedded in the application

Resources:
- MTA + Quarkus: https://quarkus.io/blog/migrating-existing-apps-to-quarkus-with-mta/
- MTA use cases: https://developers.redhat.com/products/mta/use-cases
- Quarkus guides: https://quarkus.io/guides/

---

## Using MTA to guide the migration

MTA analyzes the EAP 8.1 application and identifies what needs to change to run on Quarkus.

### Verify available Quarkus target

```shell
mta-cli rules list-targets | grep -i quarkus
```

### Run the analysis

```shell
mta-cli analyze \
  --input <path_to_cargo-tracker.war> \
  --output <path_to_output> \
  --target quarkus \
  --run-local=false \
  --overwrite
```

> **Note:** Do not specify `--source eap8` - the Quarkus ruleset targets any Java EE application regardless of source platform.

The MTA report identifies which APIs and patterns need to be replaced and provides guidance for each incident.

---

## What needs to change

| Feature | EAP 8.1 | Quarkus equivalent | Effort |
|---------|---------|-------------------|--------|
| CDI | Jakarta CDI | Quarkus Arc (CDI subset) | Low |
| JAX-RS | Jakarta RESTEasy | `quarkus-resteasy-reactive` | Low |
| JPA / Hibernate | Jakarta JPA | `quarkus-hibernate-orm` | Low-Medium |
| Bean Validation | Jakarta Validation | `quarkus-hibernate-validator` | Low |
| JDBC datasource | Galleon layer | `quarkus-jdbc-postgresql` | Low |
| JSF / Primefaces | Jakarta Faces 4.0 | **No equivalent** - requires UI rewrite | **High** |
| JMS / ActiveMQ | Jakarta Messaging | `quarkus-smallrye-reactive-messaging` | High |
| EJB (stateless, MDB) | Jakarta EJB | CDI + `@ApplicationScoped` + `@Incoming` | Medium |
| EJB timers | Jakarta EJB Timer | `@Scheduled` | Low |
| Configuration | `standalone.xml` / `web.xml` | `application.properties` | Low |

> **Note:** The JSF/Primefaces UI layer has no direct Quarkus equivalent. The presentation layer requires a rewrite using either Qute templates (Quarkus native) or a separate frontend framework.

---

## Key code changes

Based on Red Hat's production experience migrating Java EE applications to Quarkus:

1. **Dependencies** - Replace the EAP BOM with Quarkus BOM and extensions. Each Jakarta API maps to a specific Quarkus extension.

2. **CDI strictness** - Quarkus validates CDI at build time. Circular dependencies that worked in EAP fail at compile time.

3. **Injection** - `@PersistenceContext` becomes `@Inject EntityManager`. Prefer package-private injection points.

4. **Startup logic** - `@Startup` EJBs become `@ApplicationScoped` beans listening to `StartupEvent`.

5. **Configuration** - Everything moves to `application.properties`. No more XML server configuration.

6. **Build** - Replace EAP maven plugin with Quarkus Maven plugin:
   ```xml
   <groupId>io.quarkus</groupId>
   <artifactId>quarkus-maven-plugin</artifactId>
   ```

---

## MTA Analysis Report

The analysis of `cargo-tracker.war` identified **13 incidents** across 1 ruleset:

| Incidents | Rule | Description |
|----------:|------|-------------|
| 1 | `jakarta-cdi-to-quarkus-00000` | Replace `jakarta.enterprise:jakarta.enterprise.cdi-api` dependency |
| 1 | `jakarta-cdi-to-quarkus-00020` | Replace `jakarta.inject:jakarta.inject-api` dependency |
| 1 | `jakarta-cdi-to-quarkus-00030` | `beans.xml` descriptor content is ignored by Quarkus |
| 1 | `jakarta-faces-to-quarkus-00000` | Replace Jakarta Faces dependency with MyFaces |
| 1 | `javaee-pom-to-quarkus-00000` | Project artifact extension must be `jar` (not `war`) |
| 1 | `javaee-pom-to-quarkus-00010` | Adopt Quarkus BOM |
| 1 | `javaee-pom-to-quarkus-00020` | Adopt Quarkus Maven plugin |
| 1 | `javaee-pom-to-quarkus-00030` | Adopt Maven Compiler plugin configuration |
| 1 | `javaee-pom-to-quarkus-00040` | Adopt Maven Surefire plugin |
| 1 | `javaee-pom-to-quarkus-00050` | Adopt Maven Failsafe plugin |
| 1 | `javaee-pom-to-quarkus-00060` | Add Maven profile for native build |
| 1 | `javaee-pom-to-quarkus-00070` | Configure Quarkus hibernate-orm extension |
| 1 | `persistence-to-quarkus-00000` | Move persistence configuration to `application.properties` |

The full interactive report is available in [`quarkus-mta-report/`](quarkus-mta-report/index.html).
