# Migrating Cargo Tracker from EAP 8.1 to Quarkus 3.40.1

## Overview

This document describes the complete migration of the Cargo Tracker application from JBoss EAP 8.1 (Jakarta EE 10) to Quarkus 3.40.1 — a Kubernetes-native Java framework. Each step is documented with the rationale, what changed, and how.

**Source branch:** `jakartaee10-eap8` (EAP 8.1 / Jakarta EE 10 / Java 17)
**Target branch:** `quarkus` (Quarkus 3.40.1.1 / Java 17)

**Target version:** `io.quarkus.platform:quarkus-bom:3.40.1.1`

---

## MTA Analysis

Before writing any code, MTA was used to identify migration issues:

```shell
mta-cli analyze \
  --input <path_to_cargo-tracker.war> \
  --output <path_to_output> \
  --target quarkus \
  --run-local=false \
  --overwrite
```

> **Note:** Do not specify `--source eap8`. The Quarkus ruleset targets any Java EE application regardless of source platform.

The analysis identified **13 incidents** — mostly structural changes to `pom.xml`. However, MTA does not detect messaging, batch processing, UI, transaction management, or lazy loading issues, which represent the bulk of the effort.

---

## Step 1 — Foundation: Maven project structure

Transform `pom.xml` from a Jakarta EE WAR project (managed by EAP BOM) to a Quarkus project.

### What changed

| Before (EAP 8.1) | After (Quarkus) |
|-------------------|-----------------|
| `<packaging>war</packaging>` | `<packaging>quarkus</packaging>` |
| `org.jboss.bom:jboss-eap-ee-with-tools` BOM | `io.quarkus.platform:quarkus-bom:3.40.1.1` BOM |
| Jakarta EE APIs as `provided` dependencies | Quarkus extensions as `compile` dependencies |
| `maven-war-plugin` | `quarkus-maven-plugin` |
| `maven.compiler.source/target` | `maven.compiler.release=17` |

### Quarkus extensions added

| Extension | Replaces |
|-----------|----------|
| `quarkus-arc` | `jakarta.enterprise:jakarta.enterprise.cdi-api` |
| `quarkus-rest` + `quarkus-rest-jackson` | `jakarta.ws.rs:jakarta.ws.rs-api` |
| `quarkus-rest-client` + `quarkus-rest-client-jackson` | JAX-RS `Client` + JSON deserialization |
| `quarkus-hibernate-orm` + `quarkus-agroal` + `quarkus-jdbc-postgresql` | `jakarta.persistence:jakarta.persistence-api` + server-managed datasource |
| `quarkus-hibernate-validator` | `jakarta.validation:jakarta.validation-api` |
| `quarkus-narayana-jta` | `jakarta.transaction:jakarta.transaction-api` (implicit in EJBs) |
| `quarkus-websockets` | `jakarta.websocket:jakarta.websocket-api` |
| `quarkus-scheduler` | `jakarta.ejb:jakarta.ejb-api` (`@Schedule`) |
| `quarkus-undertow` | Servlet container (required for JSF) |
| `quarkus-primefaces:3.12.5` | JSF/PrimeFaces (includes MyFaces transitively) |

### Key difference

In EAP, all Jakarta EE APIs are `provided` by the application server. In Quarkus, there is no application server — each capability is an explicit extension dependency.

### Third-party dependency upgrade

`commons-lang3` was upgraded from `3.4` to `3.17.0` — the old version was missing classes required by the JUnit 5 platform at runtime (`UncheckedException`).

---

## Step 2 — JPA / Domain layer migration

### What changed

| Before (EAP 8.1) | After (Quarkus) |
|-------------------|-----------------|
| `@PersistenceContext EntityManager em` | `@Inject EntityManager em` |
| `persistence.xml` with JTA datasource | `application.properties` |
| `hibernate.enable_lazy_load_no_trans=true` | Not supported — collections changed to EAGER (see Step 8) |

### Files

- **Deleted:** `src/main/resources/META-INF/persistence.xml`
- **Created:** `src/main/resources/application.properties`

### application.properties (datasource)

```properties
quarkus.datasource.db-kind=postgresql

# Dev Services auto-provisions a PostgreSQL container in dev/test mode.
# For production, set QUARKUS_DATASOURCE_JDBC_URL, _USERNAME, _PASSWORD.

quarkus.hibernate-orm.database.generation=drop-and-create
```

> **Dev Services:** In dev mode, Quarkus automatically starts a PostgreSQL container via Docker/Testcontainers. No manual database setup required.

### 4 repositories changed

- `JpaCargoRepository` — `@PersistenceContext` → `@Inject`
- `JpaHandlingEventRepository` — same
- `JpaLocationRepository` — same
- `JpaVoyageRepository` — same

---

## Step 3 — EJB to CDI migration

Quarkus does not support EJBs. All `@Stateless`, `@Singleton`, and `@Schedule` annotations must be replaced with CDI equivalents.

### What changed

| Before (EAP 8.1) | After (Quarkus) | Reference |
|-------------------|-----------------|-----------|
| `@Stateless` | `@ApplicationScoped` + `@Transactional` | All service beans |
| `@Singleton @Startup @PostConstruct` | `@ApplicationScoped` + `void onStart(@Observes StartupEvent ev)` | `SampleDataGenerator` |
| `@Schedule(minute="*/2", hour="*")` | `@Scheduled(every = "2m")` | `UploadDirectoryScanner` |

### Critical: `@Transactional` is mandatory

In EAP, `@Stateless` provides implicit container-managed transactions — every method runs in a transaction by default. In Quarkus with `@ApplicationScoped`, **there is no implicit transaction**. Without `@Transactional`, any JPA operation outside a transaction will fail silently or throw `LazyInitializationException`.

`@Transactional` was added to **11 beans**:

| Layer | Beans |
|-------|-------|
| Repositories (4) | `JpaCargoRepository`, `JpaHandlingEventRepository`, `JpaLocationRepository`, `JpaVoyageRepository` |
| Application services (3) | `DefaultBookingService`, `DefaultCargoInspectionService`, `DefaultHandlingEventService` |
| Facade (1) | `DefaultBookingServiceFacade` |
| REST endpoints (2) | `CargoMonitoringService`, `HandlingReportService` |
| Domain factory (1) | `HandlingEventFactory` |

### 7 classes changed (annotations)

| Class | Before | After |
|-------|--------|-------|
| `DefaultBookingService` | `@Stateless` | `@ApplicationScoped @Transactional` |
| `DefaultCargoInspectionService` | `@Stateless` | `@ApplicationScoped @Transactional` |
| `DefaultHandlingEventService` | `@Stateless` | `@ApplicationScoped @Transactional` |
| `ExternalRoutingService` | `@Stateless` | `@ApplicationScoped` |
| `SampleDataGenerator` | `@Singleton @Startup` | `@ApplicationScoped` + `StartupEvent` |
| `UploadDirectoryScanner` | `@Stateless @Schedule` | `@ApplicationScoped @Scheduled` |
| `RealtimeCargoTrackingService` | `@Singleton` | `@ApplicationScoped` |

### REST endpoints (3 classes)

`CargoMonitoringService`, `HandlingReportService`, `GraphTraversalService` — dropped `@Stateless`, added `@ApplicationScoped`. The `RestConfiguration` class (JAX-RS `Application`) was replaced by `quarkus.rest.path=/rest` in `application.properties`.

> **CDI strictness:** Quarkus validates CDI at build time. Circular dependencies and ambiguous injection points that work silently in EAP will cause compile-time errors in Quarkus.

---

## Step 4 — Messaging: JMS to CDI Events

### Why CDI Events instead of JMS?

The original application uses JMS (5 queues, 1 producer, 5 MDB consumers) managed by the EAP application server. For the Quarkus migration, three options were evaluated:

| Option | Pros | Cons |
|--------|------|------|
| **CDI Events (chosen)** | No external broker needed, in-process, simple | Not distributed, in-memory only |
| Artemis JMS via `quarkus-artemis-jms` | Closest to original architecture | Requires running Artemis broker |
| Reactive Messaging (SmallRye) | Cloud-native, backpressure | Biggest rewrite, different paradigm |

**Decision:** CDI Events — the demo runs locally without external infrastructure. For production, Artemis JMS or Reactive Messaging would be appropriate.

### What changed

#### Producer: `JmsApplicationEvents`

| Before (EAP 8.1) | After (Quarkus) |
|-------------------|-----------------|
| `@Inject JMSContext jmsContext` | `@Inject Event<CargoHandledEvent>` (one per event type) |
| `@Resource(lookup = "java:app/jms/...")` per queue | CDI event types replace queue destinations |
| `jmsContext.createProducer().send(queue, msg)` | `event.fireAsync(new CargoHandledEvent(...))` |

#### Consumers: 5 MDB classes

| Before (EAP 8.1) | After (Quarkus) |
|-------------------|-----------------|
| `implements MessageListener` | Plain CDI bean |
| `@MessageDriven` (Step 3 already converted to `@ApplicationScoped`) | `@ApplicationScoped` |
| `void onMessage(Message message)` | `void onCargoHandled(@ObservesAsync CargoHandledEvent event)` |

#### New event types (Java records)

3 record classes created in `infrastructure/messaging/jms/`:

```java
public record CargoHandledEvent(String trackingId) {}
public record CargoMisdirectedEvent(String trackingId) {}
public record CargoDeliveredEvent(String trackingId) {}
```

`HandlingEventRegistrationAttempt` (existing class) is fired directly as a CDI event.

#### Dependencies removed from `pom.xml`

```xml
<!-- Removed -->
<dependency>
    <groupId>io.quarkiverse.artemis</groupId>
    <artifactId>quarkus-artemis-bom</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkiverse.artemis</groupId>
    <artifactId>quarkus-artemis-jms</artifactId>
</dependency>
```

#### `web.xml` — removed JMS destinations

All 5 `<jms-destination>` entries and the `<resource-ref>` for the datasource were removed — Quarkus manages these via `application.properties` and CDI.

---

## Step 5 — Jakarta Batch to plain Java

### Why?

Quarkus does not support Jakarta Batch (JSR 352). The file processing subsystem used a batch job (`EventFilesProcessorJob.xml`) with a reader, writer, and listeners managed by the Jakarta Batch runtime.

### What changed

The entire Jakarta Batch subsystem was **consolidated into `UploadDirectoryScanner`** — a single `@ApplicationScoped` bean that does everything the batch job did:

| Before (EAP 8.1) — 6 files | After (Quarkus) — 1 file |
|-----------------------------|--------------------------|
| `EventFilesProcessorJob.xml` — batch job descriptor | Deleted |
| `EventItemReader` — reads CSV lines, parses events | Logic moved to `UploadDirectoryScanner.parseLine()` |
| `EventItemWriter` — writes to `ApplicationEvents` + archive | Logic moved to `UploadDirectoryScanner.processFile()` |
| `FileProcessorJobListener` — logs job start/end | Logging in `processFiles()` |
| `LineParseExceptionListener` — writes failed lines | Logic moved to `writeFailedLine()` |
| `EventFilesCheckpoint` — batch checkpoint state | Deleted (not needed without batch resumability) |

The `@Scheduled(every = "2m")` annotation (from Step 3) triggers the processing. The flow is:

1. Scan `/tmp/uploads` for CSV files
2. Read each line, parse into `HandlingEventRegistrationAttempt`
3. Fire CDI event via `applicationEvents.receivedHandlingEventRegistrationAttempt()`
4. Archive processed lines to `/tmp/archive`
5. Write unparseable lines to `/tmp/failed`
6. Delete processed files

---

## Step 6 — External routing service: JNDI to ConfigProperty

### What changed

`ExternalRoutingService` used a JNDI `@Resource` lookup to get the Graph Traversal URL from `web.xml`:

| Before (EAP 8.1) | After (Quarkus) |
|-------------------|-----------------|
| `@Resource(lookup = "java:app/configuration/GraphTraversalUrl")` | `@ConfigProperty(name = "cargo-tracker.graph-traversal-url")` |
| Value in `web.xml` `<env-entry>` | Value in `application.properties` |

```properties
# application.properties
cargo-tracker.graph-traversal-url=http://localhost:8080/rest/graph-traversal/shortest-path
```

---

## Step 7 — JSF / PrimeFaces UI on Quarkus

### The challenge

Quarkus has no built-in JSF support. The Cargo Tracker UI consists of 10 backing beans, 23 XHTML views, `faces-config.xml` navigation rules, and a JSF Flow.

### Solution: `quarkus-primefaces` extension

The [Quarkiverse PrimeFaces extension](https://docs.quarkiverse.io/quarkus-primefaces/dev/) integrates Apache MyFaces (JSF implementation) and PrimeFaces into Quarkus.

**Version chosen:** `3.12.5` — this keeps PrimeFaces 12 (same version as EAP 8), avoiding breaking changes from PrimeFaces 13+ (package renames, API changes in Dashboard model, etc).

### Dependencies added to `pom.xml`

```xml
<!-- Servlet container - required for JSF/Faces -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-undertow</artifactId>
</dependency>

<!-- PrimeFaces 12 on Quarkus (includes MyFaces transitively) -->
<dependency>
    <groupId>io.quarkiverse.primefaces</groupId>
    <artifactId>quarkus-primefaces</artifactId>
    <version>3.12.5</version>
</dependency>
```

### Moving web resources

**Critical:** Quarkus does not serve files from `src/main/webapp/`. All web resources must be in `src/main/resources/META-INF/resources/`.

```
# Before (EAP 8.1)
src/main/webapp/
├── index.xhtml
├── admin/
├── booking/
├── public/
├── resources/           (CSS, images)
└── WEB-INF/

# After (Quarkus)
src/main/resources/META-INF/resources/
├── index.xhtml
├── index.html           (redirect to index.xhtml)
├── admin/
├── booking/
├── public/
├── resources/           (CSS, images)
└── WEB-INF/
```

### Welcome file workaround

Quarkus/Undertow does not honor `<welcome-file>` in `web.xml` for JSF views. A static `index.html` with `<meta http-equiv="refresh" content="0; url=index.xhtml">` redirects `/` to `index.xhtml`.

### `web.xml` cleanup

| Removed | Reason |
|---------|--------|
| 4 `<mime-mapping>` entries (ttf, woff, woff2, eot) | PrimeFaces declares these via `web-fragment.xml`; duplicates cause NPE in Undertow ([quarkusio/quarkus#57602](https://github.com/quarkusio/quarkus/issues/57602)) |
| 5 `<jms-destination>` entries | JMS replaced by CDI Events |
| `<resource-ref>` for datasource | Managed by `application.properties` |
| `<env-entry>` for GraphTraversalUrl | Replaced by `@ConfigProperty` |
| `glassfish-web.xml`, `jboss-web.xml`, `jboss-deployment-structure.xml`, `payara-resources.xml` | App-server-specific configs not needed in Quarkus |

### `@Named` with dots — EL resolution difference

EAP/Mojarra allows dots in `@Named` values (e.g., `@Named("admin.track")`) and resolves `#{admin.track.trackingId}` as a literal bean name lookup. Quarkus/MyFaces interprets the dot as property navigation — it looks for a bean named `admin` and navigates to property `track`.

| Before | After | Files affected |
|--------|-------|----------------|
| `@Named("admin.track")` | `@Named("adminTrack")` | `Track.java` (booking), `track.xhtml` (admin) |
| `@Named("public.track")` | `@Named("publicTrack")` | `Track.java` (tracking), `track.xhtml` (public) |

All EL expressions in the corresponding XHTML files were updated (e.g., `#{admin.track.cargo}` → `#{adminTrack.cargo}`).

### `<f:metadata>` placement — MyFaces strictness

MyFaces enforces that `<f:metadata>` must be a direct child of `UIViewRoot`. In EAP/Mojarra, `<f:metadata>` inside `<ui:define name="content">` works because Mojarra is lenient. MyFaces rejects it.

**Fix:** Added `<ui:insert name="metadata"/>` to both templates (`admin.xhtml`, `public.xhtml`) before `<h:head>`. Views that use `<f:viewParam>` now place `<f:metadata>` in `<ui:define name="metadata">`:

```xml
<!-- Template (admin.xhtml) -->
<ui:insert name="metadata"/>
<h:head>...</h:head>

<!-- View (show.xhtml, route.xhtml, selectItinerary.xhtml) -->
<ui:define name="metadata">
    <f:metadata>
        <f:viewParam name="trackingId" value="#{cargoDetails.trackingId}" />
        <f:event type="preRenderView" listener="#{cargoDetails.load()}" />
    </f:metadata>
</ui:define>
<ui:define name="content">
    ...
</ui:define>
```

**Files fixed:** `show.xhtml`, `route.xhtml`, `selectItinerary.xhtml`.

---

## Step 8 — Lazy loading: EAGER fetch for embedded collections

### Why?

EAP's `persistence.xml` had `hibernate.enable_lazy_load_no_trans=true`, which allowed lazy-loaded collections to be accessed outside a Hibernate session. **Quarkus does not support this property.** When JSF views access lazy collections during the render phase (outside the transaction), a `LazyInitializationException` occurs.

### What changed

Two `@Embeddable` value objects have `@OneToMany` collections that are always needed with their parent:

| Entity | Collection | Before | After |
|--------|-----------|--------|-------|
| `Itinerary` | `legs` | `@OneToMany(cascade = ALL)` (default LAZY) | `@OneToMany(cascade = ALL, fetch = EAGER)` |
| `Schedule` | `carrierMovements` | `@OneToMany(cascade = ALL, orphanRemoval = true)` (default LAZY) | `@OneToMany(cascade = ALL, orphanRemoval = true, fetch = EAGER)` |

These are semantically correct — an itinerary without legs and a schedule without movements are meaningless as standalone objects.

---

## Step 9 — Test migration: JUnit 4 / Arquillian → JUnit 5 / `@QuarkusTest`

### Why?

The original tests used JUnit 4 (`org.junit.Test`) with Arquillian (`@RunWith(Arquillian.class)`, `@Deployment`, ShrinkWrap). Quarkus uses JUnit 5 with `@QuarkusTest` — Arquillian and ShrinkWrap are not supported.

### Test dependencies in `pom.xml`

| Before | After |
|--------|-------|
| `junit:junit:4.13.1` | Removed (JUnit 5 comes via `quarkus-junit5`) |
| `arquillian-junit-container` | Removed |
| `shrinkwrap-resolver-impl-maven` | Removed |
| `quarkus-junit` | `quarkus-junit5` |
| — | `rest-assured` (kept) |

### Dead code removed

| File | Reason |
|------|--------|
| `HandlingEventServiceTest` | All code commented out, no `@Test` |
| `ExternalRoutingServiceTest` | All code commented out, no `@Test` |
| `CargoLifecycleScenarioTest` | All code commented out, no `@Test` |
| `BookingServiceTestDataGenerator` | `@Singleton @Startup` EJB test helper — `SampleDataGenerator` already seeds data in `@QuarkusTest` |
| `arquillian.xml` | Payara container config — not needed |
| `test-web.xml` | JNDI env-entry + Derby datasource for Arquillian — Dev Services replaces |

### 5 unit tests migrated (JUnit 4 → 5)

Pure domain model tests — no container needed:

| Test | Changes |
|------|---------|
| `CargoTest` (9 tests) | `org.junit.Test` → `org.junit.jupiter.api.Test`, `Assert.*` → `Assertions.*` |
| `ItineraryTest` (3 tests) | Same + `try/catch/fail()` → `assertThrows()` |
| `RouteSpecificationTest` (4 tests) | Same |
| `HandlingEventTest` (6 tests) | Same + `try/catch/fail()` → `assertThrows()` |
| `HandlingHistoryTest` (2 tests) | Same |

### 1 integration test rewritten (Arquillian → `@QuarkusTest`)

`BookingServiceTest` — tests the complete booking lifecycle (register, route, assign, change destination, change deadline):

| Before | After |
|--------|-------|
| `@RunWith(Arquillian.class)` | `@QuarkusTest` |
| `@Deployment` + `ShrinkWrap.create(WebArchive.class)` | Removed — `@QuarkusTest` boots the full app |
| `@PersistenceContext EntityManager` | `@Inject EntityManager` |
| `@InSequence(1..5)` across 5 methods | Single `@TestTransaction` method with sequential steps |
| `BookingServiceTestDataGenerator` EJB | Removed — `SampleDataGenerator` seeds data automatically |

### Test configuration

`src/test/resources/application.properties`:
```properties
cargo-tracker.graph-traversal-url=http://localhost:${quarkus.http.test-port}/rest/graph-traversal/shortest-path
```

In test mode, Quarkus runs on a random port. The graph traversal URL must use `${quarkus.http.test-port}` instead of hardcoded `8080`.

### Result

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

---

## Summary — What was migrated

| Area | EAP 8.1 | Quarkus 3.40.1 | Files changed |
|------|---------|----------------|---------------|
| Project structure | WAR + EAP BOM | Quarkus packaging + extensions | `pom.xml` |
| Datasource | `persistence.xml` + JNDI | `application.properties` + Dev Services | 1 deleted, 1 created |
| JPA repositories | `@PersistenceContext` | `@Inject` | 4 classes |
| EJB services | `@Stateless` / `@Singleton` | `@ApplicationScoped` + `@Transactional` | 7 classes |
| Transactions | Implicit (EJB container) | Explicit `@Transactional` | 11 classes |
| EJB timers | `@Schedule` | `@Scheduled` | 1 class |
| Startup | `@Startup @PostConstruct` | `@Observes StartupEvent` | 1 class |
| REST endpoints | `@Stateless` + `Application` class | `@ApplicationScoped` + `quarkus.rest.path` | 4 classes |
| Messaging | JMS (`JMSContext` + 5 MDBs) | CDI Events (`Event<T>` + `@ObservesAsync`) | 6 classes rewritten, 3 records created |
| Batch processing | Jakarta Batch (6 classes + XML) | Consolidated into `UploadDirectoryScanner` | 5 deleted, 1 rewritten |
| Configuration | JNDI `@Resource` lookup | `@ConfigProperty` | 1 class |
| Lazy loading | `enable_lazy_load_no_trans=true` | `FetchType.EAGER` on embedded collections | 2 entities |
| UI framework | JSF/PrimeFaces served by EAP | `quarkus-primefaces` 3.12.5 + Undertow | `pom.xml`, views moved |
| Web resources | `src/main/webapp/` | `src/main/resources/META-INF/resources/` | All views/CSS/images moved |
| `@Named` beans | Dots in names (`admin.track`) | camelCase (`adminTrack`) | 2 beans + 2 XHTMLs |
| `<f:metadata>` | Inside `<ui:define name="content">` | Inside `<ui:define name="metadata">` | 3 views + 1 template |
| Welcome file | `web.xml` `<welcome-file>` | `index.html` with meta redirect | 1 file created |
| Tests | JUnit 4 + Arquillian + ShrinkWrap | JUnit 5 + `@QuarkusTest` + `@TestTransaction` | 6 tests migrated, 4 deleted |
| Badge | "JBoss EAP 8.1" (red) | "Quarkus 3.40" (blue) | 2 templates |

## Gotchas — things MTA does not detect

These are the issues that caused runtime errors and required manual investigation:

1. **Missing `@Transactional`** — EJB transactions are implicit; Quarkus requires explicit `@Transactional`. Without it, JPA operations fail silently or throw exceptions during JSF render.
2. **`hibernate.enable_lazy_load_no_trans`** — Not supported in Quarkus. Lazy collections accessed outside transactions cause `LazyInitializationException`. Fix: change to `FetchType.EAGER` or ensure access within transaction scope.
3. **`@Named` with dots** — Mojarra treats `admin.track` as a literal bean name. MyFaces interprets it as property navigation (`admin` → `track`). Fix: rename to camelCase.
4. **`<f:metadata>` placement** — Mojarra allows `<f:metadata>` anywhere. MyFaces requires it as a direct child of `UIViewRoot`. Fix: use `<ui:define name="metadata">` in templates.
5. **`src/main/webapp/` ignored** — Quarkus serves web content from `META-INF/resources/`, not `webapp/`. All views and static files must be moved.
6. **`<welcome-file>` for `.xhtml`** — Undertow in Quarkus does not process JSF welcome files. Fix: static `index.html` with meta redirect.
7. **`<mime-mapping>` conflicts** — PrimeFaces `web-fragment.xml` declares font mime-mappings. Duplicates in `web.xml` cause NPE in Undertow.

---

## OpenShift Deployment

The Quarkus version is deployed using the `quarkus-openshift` extension with Docker build strategy.

### Dependencies

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-openshift</artifactId>
</dependency>
```

### Configuration (`application.properties`)

```properties
# Production datasource (OCP PostgreSQL)
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://cargo-tracker-pg:5432/ct-db
%prod.quarkus.datasource.username=ct-user
%prod.quarkus.datasource.password=ct-password

# OpenShift deployment
quarkus.openshift.build-strategy=docker
quarkus.openshift.route.expose=true
quarkus.kubernetes-client.trust-certs=true
```

### Deploy command

```shell
oc login --token=<token> --server=<server>
oc project cargo-tracker-quarkus
mvn clean package -Dquarkus.openshift.deploy=true -DskipTests
```

The extension builds the JAR locally, creates a Docker build on OCP, pushes the image to the internal registry, and creates Deployment + Service + Route automatically.

### Application URL

`http://cargo-tracker-cargo-tracker-quarkus.apps.ocp4.masales.cloud`

---

## Performance Comparison — EAP 8.1 vs Quarkus 3.40.1

Measured on the same OpenShift cluster, same application, same PostgreSQL database configuration.

| Metric | EAP 8.1 | Quarkus 3.40.1 | Improvement |
|--------|---------|----------------|-------------|
| Startup time | 55.6s | 5.9s | **9.4x faster** |
| Memory usage | 583 Mi | 259 Mi | **2.3x less memory** |

**How these were measured:**
- **Startup:** From pod logs — EAP: `WFLYSRV0025: JBoss EAP 8.1 ... started in 55612ms`; Quarkus: `cargo-tracker ... started in 5.886s`
- **Memory:** From container cgroup — `cat /sys/fs/cgroup/memory.current` on each pod

Both deployments:
- Project: OpenShift 4.x on `api.ocp4.masales.cloud:6443`
- Database: PostgreSQL 15 (`registry.redhat.io/rhel9/postgresql-15`)
- Java: OpenJDK 17

---

## Running locally

```shell
# Requires Docker running (for Dev Services PostgreSQL)
mvn quarkus:dev
```

The application starts at `http://localhost:8080` with a PostgreSQL container automatically provisioned.

## Running tests

```shell
mvn test
# 25 tests, 0 failures
```
