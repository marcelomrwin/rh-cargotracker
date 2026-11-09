# Eclipse Cargo Tracker - Quarkus 3.40.1

> **This branch (`quarkus`) is the Quarkus migration of the Cargo Tracker application.**
> Starting point: [`jakartaee10-eap8`](https://github.com/marcelomrwin/rh-cargotracker/tree/jakartaee10-eap8) (EAP 8.1 / Jakarta EE 10 running on OpenShift).
> Target: **Quarkus 3.40.1.1** - cloud-native, no application server required.

## Migration documentation

See [QUARKUS.md](QUARKUS.md) for the complete step-by-step migration guide covering all 9 steps, gotchas, and lessons learned.

## Prerequisites

- Java 17 or 21 (`JAVA_HOME` must point to a compatible JDK)
- Maven 3.9+
- Docker or Podman running (for Dev Services PostgreSQL)

> [!TIP]
> No application server, no external database, no Red Hat Maven repository required. Docker/Podman must be running for Dev Services to provision PostgreSQL automatically.

## Build

```shell
mvn clean package
```

## Run locally

```shell
mvn quarkus:dev
```

The application starts at `http://localhost:8080` with a PostgreSQL container automatically provisioned via Dev Services.

## Run tests

```shell
mvn test
# 25 tests (5 unit test classes + 1 integration test with @QuarkusTest)
```

> [!NOTE]
> Tests also use Dev Services. No manual database setup needed.

## Migration path

| Phase | Branch | Runtime | Java | Focus |
|-------|--------|---------|------|-------|
| - | `javaee7` | Payara 4.1 | 8 | Original application |
| 1 | `jee7-eap7` | JBoss EAP 7.4 (VM) | 11 | Migration to Red Hat platform |
| 2 | `jakartaee10-eap8` | JBoss EAP 8.1 (VM) | 17 | Jakarta EE 10 migration (MTA) |
| 3 | `jakartaee10-eap8` | JBoss EAP 8.1 (OpenShift) | 17 | Containerization, cloud readiness |
| 4 | **`quarkus`** | **Quarkus 3.40.1 (local)** | **17** | **Cloud-native modernization** |
| 5 | **`quarkus`** | **Quarkus 3.40.1 (OpenShift)** | **17** | **Deploy, performance comparison** |

## What changed from EAP 8.1

| Area | EAP 8.1 | Quarkus 3.40.1 |
|------|---------|----------------|
| Packaging | WAR deployed to app server | Quarkus JAR (no app server) |
| Dependencies | EAP BOM (`provided`) | Quarkus extensions (`compile`) |
| CDI | `@Stateless` / `@Singleton` (EJB) | `@ApplicationScoped` + `@Transactional` |
| Transactions | Implicit (EJB container) | Explicit `@Transactional` |
| Persistence | `persistence.xml` + JNDI datasource | `application.properties` + Dev Services |
| Messaging | JMS (5 queues, MDBs) | CDI Events (`Event<T>` + `@ObservesAsync`) |
| Batch | Jakarta Batch via EAP embedded JBeret | Jakarta Batch via `quarkus-jberet` 2.11.0 + `@Scheduled` |
| Configuration | JNDI `@Resource` lookup | `@ConfigProperty` |
| UI | JSF/PrimeFaces served by EAP | `quarkus-primefaces` 3.12.5 (MyFaces + PrimeFaces 12) |
| Web resources | `src/main/webapp/` | `src/main/resources/META-INF/resources/` |
| Tests | JUnit 4 + Arquillian + ShrinkWrap | JUnit 5 + `@QuarkusTest` + `@TestTransaction` |
| Startup (OCP) | 55.6s (measured from pod log `WFLYSRV0025`) | 5.9s (measured from pod log `started in`) |
| Memory (OCP) | 583 Mi (measured via `cgroup/memory.current`) | 259 Mi (measured via `cgroup/memory.current`) |

---

## Overview

The project demonstrates how you can develop applications with Jakarta EE using widely adopted architectural best practices like Domain-Driven
Design (DDD). The project is directly based on the well known
original [Java DDD sample application](http://dddsample.sourceforge.net)
developed by DDD pioneer Eric Evans' company Domain Language and the Swedish
software consulting company Citerus. The cargo example actually comes from
Eric Evans' seminal book on DDD.

The application is an end-to-end system for keeping track of shipping cargo. It
has several interfaces described in the following sections.

For further details on the project, please visit: https://eclipse-ee4j.github.io/cargotracker/.

![Cargo Tracker cover](cargo_tracker_cover.png)

## Exploring the Application

After the application runs, it will be available at:
http://localhost:8080/. Under the hood, the application uses
CDI, JPA (Hibernate ORM), JAX-RS (RESTEasy), WebSocket, Bean Validation, PrimeFaces (JSF/MyFaces), and Quarkus Scheduler.

There are several web interfaces, REST interfaces and a file system scanning
interface. It's probably best to start exploring the interfaces in the rough
order below.

The tracking interface lets you track the status of cargo and is
intended for the general public. Try entering a tracking ID like ABC123 (the
application is pre-populated with some sample data).

The administrative interface is intended for the shipping company that manages
cargo. The landing page of the interface is a dashboard providing an overall
view of registered cargo. You can book cargo using the booking interface.
Once cargo is booked, you can route it. When you initiate a routing request,
the system will determine routes that might work for the cargo. Once you select
a route, the cargo will be ready to process handling events at the port. You can
also change the destination for cargo if needed or track cargo.

The Incident Logging interface is intended for port personnel registering what
happened to cargo. The interface is primarily intended for mobile devices, but
you can use it via a desktop browser. The interface is accessible at: http://localhost:8080/event-logger/index.xhtml.

Generally speaking cargo goes through these events:

* It's received at the origin port.
* It's loaded and unloaded onto voyages on its itinerary.
* It's claimed at its destination port.
* It may go through customs at arbitrary points.

While filling out the event registration form, it's best to have the itinerary
handy. You can access the itinerary for registered cargo via the admin interface.

You should also explore the file system based bulk event registration interface.
It reads files under /tmp/uploads. The files are just CSV files. A sample CSV
file is available under [src/test/resources/handling_events.csv](src/test/resources/handling_events.csv). The sample is already set up to match the remaining itinerary events for cargo ABC123.

Successfully processed entries are archived under /tmp/archive. Any failed records are
archived under /tmp/failed.

> [!IMPORTANT]
> All data entered is wiped upon application restart (schema is recreated via `drop-and-create`). Sample data is re-seeded on each startup via `SampleDataGenerator`.

## Known Issues

> [!NOTE]
> * **Data is wiped on restart:** The application uses `drop-and-create` schema generation.
> * **Booking flow uses `@SessionScoped`:** The original EAP version used `@FlowScoped` with a JSF Faces Flow. Quarkus/MyFaces does not support flow definition XML files in packaged JARs, so the booking wizard uses `@SessionScoped` with direct URL navigation instead.
