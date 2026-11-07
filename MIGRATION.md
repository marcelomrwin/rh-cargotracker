# Migration Guide — JBoss EAP 7 to JBoss EAP 8.1

Step-by-step log of the migration process: which changes were automated by MTA and which were applied manually.

For MTA installation and usage instructions, refer to the official documentation:
- **"Installing the migration toolkit for applications"** — Red Hat Customer Portal
- **"Using the migration toolkit for applications command-line interface"** — Red Hat Customer Portal

---

## Step 1 — Analyze with MTA

Before making any changes, the application was analyzed using `mta-cli analyze` to identify all migration issues.

```shell
mta-cli analyze \
  --input <path_to_cargo-tracker.war> \
  --output <path_to_output> \
  --source eap7 \
  --target eap8 \
  --target eap81 \
  --run-local=false \
  --overwrite
```

The analysis identified **319 incidents** across the following categories:

| Incidents | Category |
|---------:|---------|
| 276 | Java imports: `javax.*` → `jakarta.*` |
| 14 | XML descriptors: Java EE namespaces → Jakarta EE |
| 6 | Hibernate: implicit naming for sequences/tables changed |
| 5 | `web.xml`: references to `javax`-prefixed class names |
| 9 | Maven: `pom.xml` dependencies referencing `javax` |
| 1 | Maven: `javax.activation` groupId |

The full MTA report is available in the [`mta-report/`](mta-report/index.html) directory of the [`jee7-eap7`](https://github.com/marcelomrwin/rh-cargotracker/tree/jee7-eap7) branch.

---

## Step 2 — Automated changes (mta-cli openrewrite)

The MTA CLI provides the `openrewrite` command to automatically transform source code.
Documented in **Chapter 8 — "Performing a transformation with the MTA command-line interface"**
of *"Using the migration toolkit for applications command-line interface"*.

> **Note:** The `openrewrite` command requires the container runtime (Podman/Docker) to be configured.

### Jakarta imports

```shell
mta-cli openrewrite \
  --input=<path_to_source_code> \
  --target=org.jboss.windup.JavaxToJakarta
```

This recipe replaced all `javax.*` imports with `jakarta.*` equivalents in Java source files, and updated `javax` artifact coordinates in `pom.xml`.

Example of change applied:

```java
// Before
import javax.ejb.Stateless;
import javax.inject.Inject;

// After
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
```

Dependencies added to `pom.xml` by the tool:

```
jakarta.annotation-api, jakarta.inject-api, jakarta.jms-api,
jakarta.json-api, jakarta.persistence-api, jakarta.transaction-api,
jakarta.websocket-api
```

### Jakarta XML

```shell
mta-cli openrewrite \
  --input=<path_to_source_code> \
  --target=org.jboss.windup.javax-jakarta.PersistenceXML
```

Updated `persistence.xml`: namespace version and property names changed to Jakarta EE equivalents.

### EAP 8 XML

The tool also updated `web.xml` and `test-web.xml` to use EAP 8 compatible namespaces.

---

## Step 3 — Manual changes

The following changes were not automated by MTA and were applied manually.

### Maven BOM

The Red Hat EAP 8 BOM was introduced to manage Jakarta dependency versions:

```xml
<properties>
    <version.server.bom>8.0.5.GA-redhat-00005</version.server.bom>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.jboss.bom</groupId>
            <artifactId>jboss-eap-ee-with-tools</artifactId>
            <version>${version.server.bom}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Additional Jakarta dependencies added manually:

```
jakarta.ejb-api, jakarta.enterprise.cdi-api, jakarta.websocket-client-api,
jakarta.faces-api, jakarta.batch-api, jakarta.xml.bind-api,
jakarta.ws.rs-api, jakarta.validation-api
```

### Primefaces

Primefaces was upgraded from `8.0` to `12.0.0` with the `jakarta` classifier, as version 8 is not compatible with Jakarta Faces 4.0:

```xml
<dependency>
    <groupId>org.primefaces</groupId>
    <artifactId>primefaces</artifactId>
    <version>12.0.0</version>
    <classifier>jakarta</classifier>
</dependency>
```

The UI theme was also changed from `omega` (retired in Primefaces 12) to `saga`.

### Messaging (MDB annotations)

`@ActivationConfigProperty` values in Message Driven Beans contained `javax` references that the tool did not update:

```java
// Before
@ActivationConfigProperty(propertyName = "destinationType",
    propertyValue = "javax.jms.Queue")

// After
@ActivationConfigProperty(propertyName = "destinationType",
    propertyValue = "jakarta.jms.Queue")
```

### CDI descriptor (beans.xml)

Updated to Jakarta EE 3.0 namespace. The `bean-discovery-mode="all"` attribute is now **required** — CDI 4.0 changed the default from `all` to `annotated`, which is a silent breaking change:

```xml
<beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
           https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd"
       version="3.0"
       bean-discovery-mode="all">
</beans>
```

### Jakarta Faces descriptor (faces-config.xml)

Updated to version 4.0:

```xml
<faces-config version="4.0"
    xmlns="https://jakarta.ee/xml/ns/jakartaee"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
        https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_0.xsd">
```

### JEE Batch job descriptor

The batch job XML namespace was updated to Jakarta version:

```xml
<job id="EventFilesProcessorJob"
     xmlns="https://jakarta.ee/xml/ns/jakartaee"
     version="2.0">
```

### @PrivateOwned removal

The `@PrivateOwned` annotation (EclipseLink-specific) was removed. It has no equivalent in Hibernate (the JPA provider used by EAP 8) and has no functional side effects in this application.

---

## Post-migration validation

After applying all changes, a second MTA analysis was run against the migrated branch:

```shell
mta-cli analyze \
  --input <path_to_jakartaee10-eap8_source> \
  --output <path_to_output> \
  --source eap7 \
  --target eap8 \
  --target eap81 \
  --run-local=false \
  --overwrite
```

Result: **1 incident** — `javax.sql.DataSource` (confirmed false positive, not to be changed).
All 319 original incidents were resolved.
