# Eclipse Cargo Tracker - Applied Domain-Driven Design Blueprints for Jakarta EE

> **This branch (`jakartaee10-eap8`) has been migrated to run on JBoss EAP 8.1 with Java 17 and Jakarta EE 10.**
> This is the result of migrating the [`jee7-eap7`](https://github.com/marcelomrwin/rh-cargotracker/tree/jee7-eap7) branch using the [MTA (Migration Toolkit for Applications)](https://developers.redhat.com/products/mta).

## Migration log

This branch is the result of migrating the [`jee7-eap7`](https://github.com/marcelomrwin/rh-cargotracker/tree/jee7-eap7) branch using MTA.
See [MIGRATION.md](MIGRATION.md) for the complete step-by-step migration log, including which changes were automated by MTA and which were manual.

## What changed from jee7-eap7

- Java upgraded from 11 to 17
- All `javax.*` imports replaced with `jakarta.*` equivalents (276 changes)
- XML deployment descriptors updated to Jakarta EE namespaces
- Maven dependencies updated: `javax` groupId replaced by `jakarta.platform`; Red Hat EAP 8 BOM introduced
- Primefaces upgraded from `8.0` to `12.0.0` with classifier `jakarta`; theme changed from `omega` to `saga`
- `beans.xml` updated to Jakarta EE 3.0 namespace with `bean-discovery-mode="all"` (required  - CDI 4.0 changed the default)
- `faces-config.xml` updated to Jakarta Faces 4.0
- `@PrivateOwned` (EclipseLink-specific) removed  - no equivalent in Hibernate
- Hibernate implicit naming strategy for sequences/tables updated
- Database changed from H2 in-memory to **PostgreSQL** for OpenShift deployment; H2 in-memory remains usable for local testing (see Prerequisites below)

## Prerequisites

- Java 17 (`JAVA_HOME` must point to a Java 17 JDK)
- Maven 3.6+
- Access to the Red Hat Maven repository (required for the EAP 8 BOM): `https://maven.repository.redhat.com/ga/`
- JBoss EAP 8.1
- For local testing: H2 datasource configured in EAP 8.1 via `jboss-cli.sh` (same approach as `jee7-eap7`, using `standalone-full.xml`)
- For OpenShift deployment: a PostgreSQL instance (see [ocp-deployment.md](ocp-deployment.md))

## Build

```shell
mvn clean package -DskipTests
```

> [!IMPORTANT]
> The `pom.xml` uses the Red Hat EAP 8 BOM (`org.jboss.bom:jboss-eap-ee-with-tools`), which is not available in Maven Central. Add the Red Hat GA repository to your `~/.m2/settings.xml` or to the project `pom.xml` before building.

The WAR file will be generated at `target/cargo-tracker.war`.

## OpenShift Cloud Readiness

Before deploying to OpenShift, MTA was used to assess cloud readiness. See [OCP-READINESS.md](OCP-READINESS.md) for the full analysis and [ocp-mta-report/index.html](ocp-mta-report/index.html) for the HTML report.

## Deploy on OpenShift

This branch includes Kubernetes/OpenShift manifests and an S2I Dockerfile for containerized deployment.

See [ocp-deployment.md](ocp-deployment.md) for the complete step-by-step OpenShift deployment guide.

## Migration path

| Phase | Branch | Runtime | Java | Focus |
|-------|--------|---------|------|-------|
| - | `javaee7` | Payara 4.1 | 8 | Original application |
| 1 | `jee7-eap7` | JBoss EAP 7.4 (VM) | 11 | Migration to Red Hat platform |
| 2 | **`jakartaee10-eap8`** | **JBoss EAP 8.1 (VM)** | **17** | **Jakarta EE 10 migration (current)** |
| 3 | `jakartaee10-eap8` | JBoss EAP 8.1 (OpenShift) | 17 | Containerization, cloud readiness |
| 4 | `quarkus` | Quarkus 3.40.1 (local) | 17 | Cloud-native modernization |
| 5 | `quarkus` | Quarkus 3.40.1 (OpenShift) | 17 | Deploy, performance comparison |

### Navigating between phases

```shell
# Clone the repository
git clone https://github.com/marcelomrwin/rh-cargotracker.git
cd rh-cargotracker

# Phase 1 - EAP 7.4 (VM)
git checkout jee7-eap7

# Phase 2/3 - EAP 8.1 (VM + OpenShift)
git checkout jakartaee10-eap8

# Phase 4/5 - Quarkus (local + OpenShift)
git checkout quarkus
```

---

## Overview

The project demonstrates how you can develop applications with Jakarta EE using widely adopted architectural best practices like Domain-Driven 
Design (DDD). The project is directly based on the well known 
original [Java DDD sample application](http://dddsample.sourceforge.net) 
developed by DDD pioneer Eric Evans' company Domain Language and the Swedish 
software consulting company Citerus. The cargo example actually comes from 
Eric Evans' seminal book on DDD. The original application is written in Spring,
Hibernate and Jetty whereas the application is built on Jakarta EE.

The application is an end-to-end system for keeping track of shipping cargo. It 
has several interfaces described in the following sections.

For further details on the project, please visit: https://eclipse-ee4j.github.io/cargotracker/.

![Cargo Tracker cover](cargo_tracker_cover.png)

## Exploring the Application

After the application runs, it will be available at: 
http://localhost:8080/cargo-tracker/. Under the hood, the application uses a 
number of Jakarta EE 10 features including Faces, CDI, EJB, JPA, JAX-RS, WebSocket, JSON Processing, Bean Validation and JMS.

There are several web interfaces, REST interfaces and a file system scanning
interface. It's probably best to start exploring the interfaces in the rough
order below.

The tracking interface let's you track the status of cargo and is
intended for the general public. Try entering a tracking ID like ABC123 (the 
application is pre-populated with some sample data).

The administrative interface is intended for the shipping company that manages
cargo. The landing page of the interface is a dashboard providing an overall 
view of registered cargo. You can book cargo using the booking interface.
One cargo is booked, you can route it. When you initiate a routing request,
the system will determine routes that might work for the cargo. Once you select
a route, the cargo will be ready to process handling events at the port. You can
also change the destination for cargo if needed or track cargo.

The Incident Logging interface is intended for port personnel registering what 
happened to cargo. The interface is primarily intended for mobile devices, but
you can use it via a desktop browser. The interface is accessible at this URL: http://localhost:8080/cargo-tracker/event-logger/index.xhtml. For convenience, you
could use a mobile emulator instead of an actual mobile device. Generally speaking cargo
goes through these events:

* It's received at the origin port.
* It's loaded and unloaded onto voyages on it's itinerary.
* It's claimed at it's destination port.
* It may go through customs at arbitrary points.

While filling out the event registration form, it's best to have the itinerary 
handy. You can access the itinerary for registered cargo via the admin interface. The cargo handling is done via Messaging for scalability. While using the incident logger, note that only the load 
and unload events require as associated voyage.

You should also explore the file system based bulk event registration interface. 
It reads files under /tmp/uploads. The files are just CSV files. A sample CSV
file is available under [src/test/resources/handling_events.csv](src/test/resources/handling_events.csv). The sample is already set up to match the remaining itinerary events for cargo ABC123. Just make sure to update the times in the first column of the sample CSV file to match the itinerary as well.

Sucessfully processed entries are archived under /tmp/archive. Any failed records are 
archived under /tmp/failed.

Don't worry about making mistakes. The application is intended to be fairly 
error tolerant. If you do come across issues, you should [report them](https://github.com/eclipse-ee4j/cargotracker/issues).

*All data entered is wiped upon application restart, so you can start from 
a blank slate easily if needed.*

You can also use the soapUI scripts included in the source code to explore the 
REST interfaces as well as the numerous unit tests covering the code base 
generally.

## Exploring the Code

As mentioned earlier, the real point of the application is demonstrating how to 
create well architected, effective Jakarta EE applications. To that end, once you 
have gotten some familiarity with the application functionality the next thing 
to do is to dig right into the code.

DDD is a key aspect of the architecture, so it's important to get at least a 
working understanding of DDD. As the name implies, Domain-Driven Design is an 
approach to software design and development that focuses on the core domain and 
domain logic.

For the most part, it's fine if you are new to Jakarta EE. As long as you have a
basic understanding of server-side applications, the code should be good enough to get started. For learning Jakarta EE further,
we have recommended a few links in the resources section of the project site. Of 
course, the ideal user of the project is someone who has a basic working 
understanding both Jakarta EE and DDD. Though it's not our goal to become a kitchen 
sink example for demonstrating the vast amount of APIs and features in Jakarta EE,
we do use a very representative set. You'll find that you'll learn a fair amount
by simply digging into the code to see how things are implemented.

## Known Issues

> [!WARNING]
> * **Red Hat Maven repository required:** The EAP 8 BOM (`org.jboss.bom:jboss-eap-ee-with-tools`) is not published to Maven Central. The build will fail without access to `https://maven.repository.redhat.com/ga/`.

> [!NOTE]
> * **`javax.sql.DataSource` in web.xml:** The MTA analysis flags `javax.sql.DataSource` as a migration issue, but this is a false positive. `javax.sql` is part of the Java SE standard and was not moved to the `jakarta` namespace. It must not be changed.
> * **Data is wiped on restart:** The application uses a PostgreSQL database. All data is preserved between restarts, but the schema is recreated if the database is empty.
