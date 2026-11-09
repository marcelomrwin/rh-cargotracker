# Eclipse Cargo Tracker - Applied Domain-Driven Design Blueprints for Jakarta EE

> **This branch (`jee7-eap7`) has been adapted to run on JBoss EAP 7.4 with Java 11.**
> The original application targets Payara/Glassfish with Java 8  - see the [`javaee7`](https://github.com/marcelomrwin/rh-cargotracker/tree/javaee7) branch for that version.

## MTA Analysis

This branch includes an MTA (Migration Toolkit for Applications) analysis report identifying the changes required to migrate this application from EAP 7 to EAP 8.

- [View the MTA analysis report](mta-report/index.html)
- [MTA analysis procedure and results summary](MTA-ANALYSIS.md)

## Running on JBoss EAP 7.4

### Prerequisites

- Java 11 (`JAVA_HOME` must point to a Java 11 JDK  - EAP 7.4 does not support Java 17+)
- Maven 3.6+
- JBoss EAP 7.4

### Build

```shell
mvn clean package -DskipTests
```

### Server setup (run once)

Start EAP with the **full profile** (required for JMS/messaging support):

```shell
$EAP_HOME/bin/standalone.sh -c standalone-full.xml
```

Configure the H2 datasource (persisted automatically in `standalone-full.xml`):

```shell
$EAP_HOME/bin/jboss-cli.sh --connect --command=\
'/subsystem=datasources/data-source=CargoTrackerDS:add(
  jndi-name="java:/jdbc/CargoTrackerDatabase",
  driver-name="h2",
  connection-url="jdbc:h2:mem:cargotracker;DB_CLOSE_DELAY=-1",
  user-name="sa",
  password="sa",
  use-java-context=true,
  enabled=true)'
```

> The application does not define its own datasource. It expects `java:/jdbc/CargoTrackerDatabase` to be configured at the server level.

### Deploy

```shell
cp target/cargo-tracker.war $EAP_HOME/standalone/deployments/
```

Access the application at: http://localhost:8080/cargo-tracker

### What changed from the original

- Java upgraded from 8 to 11
- Internal database changed from Derby to H2
- JMS resource adapter reference removed (EAP uses a single default adapter)
- MOXy JSON provider removed in favor of JSON-B
- Persistence layer adjusted for Hibernate (`orphanRemoval` relaxed, lazy loading outside transactions enabled)

### Migration path

| Phase | Branch | Runtime | Java | Focus |
|-------|--------|---------|------|-------|
| — | `javaee7` | Payara 4.1 | 8 | Original application |
| 1 | **`jee7-eap7`** | **JBoss EAP 7.4 (VM)** | **11** | **Migration to Red Hat platform (current)** |
| 2 | `jakartaee10-eap8` | JBoss EAP 8.1 (VM) | 17 | Jakarta EE 10 migration |
| 3 | `jakartaee10-eap8` | JBoss EAP 8.1 (OpenShift) | 17 | Containerization, cloud readiness |
| 4 | `quarkus` | Quarkus 3.40.1 (local) | 17 | Cloud-native modernization |
| 5 | `quarkus` | Quarkus 3.40.1 (OpenShift) | 17 | Deploy, performance comparison |

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
number of Jakarta EE (Java EE 7) features including JSF, CDI, EJB, JPA, JAX-RS, WebSocket, JSON Processing, Bean Validation and JMS.

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

## Exploring the Tests

Cargo Tracker's testing is done using JUnit and Arquillian. The Arquillian configuration
uses a [remote container](http://arquillian.org/arquillian-core/#_containers). Therefore, to perform a test you will need to make sure
to have a container running.

## Known Issues

* **Deployment fails on first start:** The H2 datasource must be configured in EAP before the WAR is deployed. If the application fails to deploy with `jboss.naming.context.java.jdbc.CargoTrackerDatabase not found`, configure the datasource via `jboss-cli.sh` as described in the setup section above, then trigger a redeploy by removing the `.failed` marker and creating a `.dodeploy` marker in the deployments folder.
* **Data is wiped on restart:** The application uses an in-memory H2 database. All data entered is lost when the server restarts  - this is expected behavior and allows starting from a blank slate easily.
