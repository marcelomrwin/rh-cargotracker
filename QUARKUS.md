# Migrating Cargo Tracker to Quarkus 3.40.1

## Overview

This document describes the migration of the Cargo Tracker application from JBoss EAP 8.1 / Jakarta EE 10 to Quarkus 3.40.1 — a Kubernetes-native Java framework designed for cloud environments.

**Target version:** `io.quarkus.platform:quarkus-bom:3.40.1.1`
*(Version verified in [quarkus-quickstarts](https://github.com/quarkusio/quarkus-quickstarts))*

**Reference material:**
- Red Hat training: DO378 — Red Hat Cloud-native Microservices Development with Quarkus 3.8
- Quarkus guides: https://quarkus.io/guides/
- Quickstarts: https://github.com/quarkusio/quarkus-quickstarts

---

## Why Quarkus?

Quarkus eliminates the application server. The framework is embedded in the application, providing:
- Startup time measured in milliseconds (vs. 60+ seconds for EAP)
- Low memory footprint — suitable for container environments
- Build-time CDI resolution — errors caught at compile time, not runtime
- Native binary compilation via GraalVM (optional)
- Developer experience with live reload (`quarkus dev`)

---

## MTA Analysis

Before writing any code, MTA was used to identify migration issues.

```shell
mta-cli analyze \
  --input <path_to_cargo-tracker.war> \
  --output <path_to_output> \
  --target quarkus \
  --run-local=false \
  --overwrite
```

> **Note:** Do not specify `--source eap8`. The Quarkus ruleset targets any Java EE application regardless of source platform.

The analysis identified **13 incidents** — mostly structural changes to `pom.xml`. The full report is in [`quarkus-mta-report/`](quarkus-mta-report/index.html).

---

## Full Migration Scope

The MTA report is a starting point. A complete audit of the source code reveals the full scope:

### 1. Maven project structure

**MTA incidents: 7** — All `pom.xml` changes.

The base `pom.xml` structure for Quarkus (verified in `getting-started` quickstart):

```xml
<packaging>quarkus</packaging>

<properties>
    <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
    <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
    <quarkus.platform.version>3.40.1.1</quarkus.platform.version>
    <maven.compiler.release>17</maven.compiler.release>
    <maven.compiler.parameters>true</maven.compiler.parameters>
    <compiler-plugin.version>3.15.0</compiler-plugin.version>
    <surefire-plugin.version>3.5.4</surefire-plugin.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>${quarkus.platform.group-id}</groupId>
            <artifactId>${quarkus.platform.artifact-id}</artifactId>
            <version>${quarkus.platform.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<build>
    <plugins>
        <plugin>
            <groupId>${quarkus.platform.group-id}</groupId>
            <artifactId>quarkus-maven-plugin</artifactId>
            <version>${quarkus.platform.version}</version>
            <extensions>true</extensions>
        </plugin>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>${compiler-plugin.version}</version>
        </plugin>
        <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>${surefire-plugin.version}</version>
        </plugin>
    </plugins>
</build>

<!-- Native build profile -->
<profiles>
    <profile>
        <id>native</id>
        <activation>
            <property><name>native</name></property>
        </activation>
        <properties>
            <quarkus.native.enabled>true</quarkus.native.enabled>
            <quarkus.package.jar.enabled>false</quarkus.package.jar.enabled>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <version>${surefire-plugin.version}</version>
                    <!-- ... -->
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

**Effort: Low | Story points: 5**

---

### 2. EJB — replace with CDI

**MTA incidents: 0** (not detected — requires manual analysis)

7 `@Stateless` EJBs and 1 `@Singleton @Startup` must be converted.
Reference: DO378 Chapter 2 — Developing Cloud-native Microservices with Quarkus.

| Class | Current | Quarkus replacement | Notes |
|-------|---------|-------------------|-------|
| `DefaultBookingService` | `@Stateless` | `@ApplicationScoped` | Transactions managed by `quarkus-narayana-jta` |
| `DefaultCargoInspectionService` | `@Stateless` | `@ApplicationScoped` | Same |
| `DefaultHandlingEventService` | `@Stateless` | `@ApplicationScoped` | Same |
| `ExternalRoutingService` | `@Stateless` | `@ApplicationScoped` + `@RegisterRestClient` | HTTP client change |
| `SampleDataGenerator` | `@Singleton @Startup @PostConstruct` | `@ApplicationScoped` + `void onStart(@Observes StartupEvent ev)` | DO378 §2.1 |
| `UploadDirectoryScanner` | `@Stateless @Schedule` | `@ApplicationScoped @Scheduled(every="2m")` | Guide: https://quarkus.io/guides/scheduler |

Required extension:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-arc</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-scheduler</artifactId>
</dependency>
```

> **CDI strictness:** Quarkus validates CDI at build time. Circular dependencies and ambiguous injection points that work silently in EAP will cause compile-time errors in Quarkus.

**Effort: Low | Story points: 7**

---

### 3. JAX-RS endpoints

**MTA incidents: 0** (structural, not detected)

3 REST endpoints. Replace `quarkus-rest` for JAX-RS support.
Reference: DO378 §2.3 — Developing REST based Microservices.

| Class | Path | Quarkus change |
|-------|------|---------------|
| `CargoMonitoringService` | `GET /rest/cargo` | Drop `@Stateless`, add `@ApplicationScoped` |
| `HandlingReportService` | `POST /rest/handling/reports` | Drop `@Stateless`, replace JMS with reactive messaging |
| `GraphTraversalService` | `GET /rest/graph-traversal/shortest-path` | Drop `@Stateless`, add `@ApplicationScoped` |
| `RestConfiguration` | JAX-RS `Application` | Replace with `quarkus.rest.path=/rest` in `application.properties` |

Required extensions (from `hibernate-orm-quickstart` and `rest-json-quickstart`):
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```

**Effort: Low | Story points: 3**

---

### 4. JPA / Persistence

**MTA incidents: 2** (dependency + persistence.xml)

4 JPA repositories using `@PersistenceContext EntityManager`.
Reference: DO378 §2.5 — Data Persistence with Panache; Guide: https://quarkus.io/guides/hibernate-orm

| Change | Before | After |
|--------|--------|-------|
| Dependency | EAP BOM manages JPA | `quarkus-hibernate-orm` + `quarkus-agroal` + `quarkus-jdbc-postgresql` |
| Injection | `@PersistenceContext EntityManager em` | `@Inject EntityManager em` |
| Configuration | `persistence.xml` with JTA datasource | `application.properties` |

Required extensions (from `hibernate-orm-quickstart`):
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-agroal</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
```

`application.properties` (replaces `persistence.xml`):
```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/cargo-tracker
quarkus.datasource.username=ct-user
quarkus.datasource.password=ct-password
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.log.sql=false
```

> **Critical:** `hibernate.enable_lazy_load_no_trans=true` used in EAP is **not supported in Quarkus**. All lazy loading outside transaction boundaries must be refactored — either by eagerly loading associations or by ensuring access within transaction scope.

**Effort: Medium | Story points: 8**

---

### 5. JMS/Messaging

**MTA incidents: 0** (not detected — HIGH effort)

The most complex migration area. 1 JMS producer + 5 MDB consumers using ActiveMQ.

**Option A — Keep JMS with Artemis extension** (from `jms-quickstart`):
```xml
<!-- Additional BOM in dependencyManagement -->
<dependency>
    <groupId>io.quarkiverse.artemis</groupId>
    <artifactId>quarkus-artemis-bom</artifactId>
    <version>3.14.3</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<!-- Dependency -->
<dependency>
    <groupId>io.quarkiverse.artemis</groupId>
    <artifactId>quarkus-artemis-jms</artifactId>
</dependency>
```
Keeps `JMSContext`, `@Resource`, `@MessageDriven` patterns. Least disruptive.

**Option B — Reactive Messaging with AMQP** (from `amqp-quickstart`, recommended for cloud-native):
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-messaging-amqp</artifactId>
</dependency>
```

Producer replaces `JMSContext` with `@Inject @Channel("cargo-handled") Emitter<String>`.
Consumer replaces `@MessageDriven` with `@Incoming("cargo-handled")`.

| Current class | Current | Option A | Option B |
|---------------|---------|----------|----------|
| `JmsApplicationEvents` | `@ApplicationScoped` + `JMSContext` | Keep + use Artemis JMS | `Emitter<String>` per channel |
| `CargoHandledConsumer` | `@MessageDriven` | `@MessageDriven` (Artemis) | `@Incoming("cargo-handled")` |
| `DeliveredCargoConsumer` | `@MessageDriven` | `@MessageDriven` (Artemis) | `@Incoming("delivered-cargo")` |
| `HandlingEventRegistrationAttemptConsumer` | `@MessageDriven` | `@MessageDriven` (Artemis) | `@Incoming("handling-event-registration")` |
| `MisdirectedCargoConsumer` | `@MessageDriven` | `@MessageDriven` (Artemis) | `@Incoming("misdirected-cargo")` |
| `RejectedRegistrationAttemptsConsumer` | `@MessageDriven` | `@MessageDriven` (Artemis) | `@Incoming("rejected-attempts")` |

**Effort: High | Story points: 13**

---

### 6. WebSocket

**MTA incidents: 0** (not detected)

1 WebSocket endpoint (`RealtimeCargoTrackingService`) using `@ServerEndpoint`.
Reference: https://quarkus.io/guides/websockets

Required extension (from `websockets-quickstart`):
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-websockets</artifactId>
</dependency>
```

The `@ServerEndpoint("/tracking")` annotation is **compatible** with Quarkus via this extension. Change `@Singleton` to `@ApplicationScoped`.

**Effort: Medium | Story points: 3**

---

### 7. Jakarta Batch

**MTA incidents: 0** (not detected — HIGH effort)

The file processing subsystem uses Jakarta Batch 2.0 (not supported in Quarkus).

Components:
- `UploadDirectoryScanner` — triggers job via `@Schedule`
- `EventItemReader` — reads CSV files
- `EventItemWriter` — writes handling events
- `FileProcessorJobListener`, `LineParseExceptionListener` — listeners
- `batch-jobs/EventFilesProcessorJob.xml` — job descriptor

**Replacement strategy:** Use `quarkus-quartz` for scheduling + custom file processing:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-quartz</artifactId>
</dependency>
```

**Effort: High | Story points: 13**

---

### 8. JSF / Primefaces UI

**MTA incidents: 1** (only dependency change detected)

**This is the highest effort area.** Quarkus has no native JSF support.

Scope: **10 JSF backing beans + 23 XHTML views + faces-config.xml navigation rules + 1 JSF flow**.

**Options:**

| Option | Description | Effort |
|--------|-------------|--------|
| `quarkus-myfaces` | Community extension — runs JSF in Quarkus with minimal code changes | Medium (extension may lag Quarkus versions) |
| Qute templates | Quarkus native — requires rewriting all views as Qute + REST endpoints | Very High |
| Keep separate | Maintain UI as a separate WAR, only migrate backend to Quarkus | Low for migration, High for architecture |

For the **workshop demo**, the recommended approach is `quarkus-myfaces` to minimize rewrite effort while still running on Quarkus. Full Qute rewrite is recommended for production.

**Effort: Very High | Story points: 34+**

---

## Summary — Effort Estimate

| Area | Items | Effort | Story Points |
|------|-------|--------|-------------|
| Maven `pom.xml` (BOM, plugins, packaging) | 1 file | Low | 5 |
| EJB (`@Stateless`) → CDI (`@ApplicationScoped`) | 7 classes | Low | 7 |
| `@Singleton @Startup` → `StartupEvent` | 1 class | Low | 2 |
| `@Schedule` → `@Scheduled` | 1 class | Low | 2 |
| JAX-RS endpoints | 3 classes | Low | 3 |
| JPA repositories (`@PersistenceContext` → `@Inject`) | 4 classes | Low | 2 |
| `persistence.xml` → `application.properties` | 1 file | Low | 2 |
| Lazy loading refactor | Unknown scope | Medium-High | 8-13 |
| WebSocket | 1 class | Medium | 3 |
| JMS/Messaging (1 producer + 5 MDBs) | 6 classes | High | 13 |
| Jakarta Batch → Quartz + custom | 6 classes + 1 XML | High | 13 |
| JSF/Primefaces UI (10 beans + 23 views) | 33+ files | Very High | 34+ |
| External REST client | 1 class | Medium | 3 |
| **TOTAL** | | | **~90-100 SP** |

> **Context:** At a typical team velocity of 20-30 SP/sprint (2-week sprint, 2 developers), this migration represents **3-5 sprints** of work. The JSF/Primefaces layer alone may require 1-2 dedicated sprints depending on the chosen UI strategy.

> **MTA coverage:** The 13 incidents MTA detected cover approximately 10 SP out of ~90-100 total. MTA identifies the structural starting point but does not detect messaging, batch, or UI migration work.

---

## Migration Approach

Based on the DO378 training material, the recommended incremental approach:

1. **Foundation** — Set up Quarkus project structure, migrate `pom.xml`, configure datasource
2. **Domain layer** — Migrate JPA entities and repositories (no breaking changes)
3. **Application services** — Convert EJBs to CDI beans
4. **REST layer** — Migrate JAX-RS endpoints
5. **Messaging** — Replace JMS with Artemis JMS extension or reactive messaging
6. **WebSocket** — Migrate real-time tracking
7. **Scheduling/Batch** — Replace Jakarta Batch with Quartz
8. **UI** — Migrate or replace JSF/Primefaces layer

This order minimises risk by establishing the core domain and infrastructure first.
