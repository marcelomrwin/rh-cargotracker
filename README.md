# Eclipse Cargo Tracker — JBoss EAP 7.4 (jee7-eap7)

## Overview

This branch contains the Cargo Tracker application adapted to run on **JBoss EAP 7.4** over a **Java 11** runtime.

The application is an end-to-end system for keeping track of shipping cargo, demonstrating Domain-Driven Design (DDD) with Java EE 7 APIs: JSF, CDI, EJB, JPA, JAX-RS, WebSocket, JSON Processing, Bean Validation and JMS.

The original application runs on Payara/Glassfish with Java 8. See the [`javaee7`](https://github.com/marcelomrwin/rh-cargotracker/tree/javaee7) branch for that version.

## What changed from the original

- Java upgraded from 8 to 11
- Internal database changed from Derby to H2 (declared in `web.xml`)
- H2 runtime dependency declared via `src/main/webapp/WEB-INF/jboss-deployment-structure.xml`
- JMS resource adapter reference removed (EAP uses a single default adapter)
- MOXy JSON provider removed in favor of JSON-B (Jakarta EE 8 standard)
- Persistence layer adjusted for Hibernate: `orphanRemoval` relaxed and lazy loading outside transactions enabled in `persistence.xml`

## Prerequisites

- Java 11
- Maven 3.6+
- JBoss EAP 7.4 (to run locally)

## Build

```shell
git clone https://github.com/marcelomrwin/rh-cargotracker.git
cd rh-cargotracker
git checkout jee7-eap7

mvn clean package
```

The WAR file will be generated at `target/cargo-tracker.war`.

## Run locally on JBoss EAP 7.4

1. Download and install [JBoss EAP 7.4](https://developers.redhat.com/products/eap/download).

2. Ensure `JAVA_HOME` points to a **Java 11** JDK. EAP 7.4 requires Java 11 — Java 17+ causes a startup failure (`UnsupportedOperationException: Setting a system-wide Policy object is not supported`).

3. Start EAP with the **full profile** (required for JMS/messaging support):
   ```shell
   $EAP_HOME/bin/standalone.sh -c standalone-full.xml
   ```

4. Configure the H2 datasource (run once — persisted automatically in `standalone-full.xml`):
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

5. Copy the WAR to the EAP deployments folder:
   ```shell
   cp target/cargo-tracker.war $EAP_HOME/standalone/deployments/
   ```

6. Access the application at: http://localhost:8080/cargo-tracker

## Run in a container

A pre-built WAR is available under `docker/cargo-tracker.war` for convenience.

### Simple build (single stage)

```shell
cd docker
podman build -f Containerfile -t cargo-tracker-eap7 .
podman run -p 8080:8080 cargo-tracker-eap7
```

### Chained build (builder + runtime image)

```shell
cd docker
podman build -f Containerfile-chained -t cargo-tracker-eap7 .
podman run -p 8080:8080 cargo-tracker-eap7
```

Both container builds use the official Red Hat images:
- Builder: `registry.redhat.io/jboss-eap-7/eap74-openjdk11-openshift-rhel8`
- Runtime: `registry.redhat.io/jboss-eap-7/eap74-openjdk11-runtime-openshift-rhel8`

> **Note:** Access to `registry.redhat.io` requires a valid Red Hat account. Log in with:
> ```shell
> podman login registry.redhat.io
> ```

## Exploring the Application

After the application starts, it will be available at: http://localhost:8080/cargo-tracker

The application is pre-populated with sample data. Try entering tracking ID `ABC123` in the tracking interface.

## Migration path

| Branch | Runtime | Java |
|--------|---------|------|
| `javaee7` | Payara 4.1 | 8 |
| `jee7-eap7` | JBoss EAP 7.4 | 11 |
| `jee10-eap8` | JBoss EAP 8.0 | 17 |

To migrate this application from EAP 7.4 to EAP 8.0, see the [`jee10-eap8`](https://github.com/marcelomrwin/rh-cargotracker/tree/jee10-eap8) branch.
The [MTA (Migration Toolkit for Applications)](https://developers.redhat.com/products/mta) can be used to analyze this branch and identify the required changes.
