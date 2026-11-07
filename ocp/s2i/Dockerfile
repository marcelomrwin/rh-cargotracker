# Stage 1: Provision the JBoss EAP 8 server using Galleon
FROM registry.redhat.io/jboss-eap-8/eap8-openjdk17-builder-openshift-rhel8:latest AS builder

# Galleon feature packs and layers to provision
ENV GALLEON_PROVISION_FEATURE_PACKS org.jboss.eap:wildfly-ee-galleon-pack,org.jboss.eap.cloud:eap-cloud-galleon-pack,org.jboss.eap:eap-datasources-galleon-pack
ENV GALLEON_PROVISION_LAYERS cloud-default-config,embedded-activemq,postgresql-driver,postgresql-datasource

# Specify the JBoss EAP channel (version)
ENV GALLEON_PROVISION_CHANNELS org.jboss.eap.channels:eap-8.1

# PostgreSQL driver version (used by the postgresql-driver Galleon layer)
ENV POSTGRESQL_DRIVER_VERSION 42.7.5

# Run the S2I assemble script to provision the EAP server
RUN /usr/local/s2i/assemble


# Stage 2: Assemble the runtime image
FROM registry.redhat.io/jboss-eap-8/eap8-openjdk17-runtime-openshift-rhel8:latest AS runtime

# Copy the provisioned server from the builder stage
COPY --from=builder --chown=jboss:root $JBOSS_HOME $JBOSS_HOME

# Copy the application WAR and PostgreSQL driver module
COPY --chown=jboss:root modules/ $JBOSS_HOME/modules/
COPY --chown=jboss:root deployments/*.war $JBOSS_HOME/standalone/deployments

# Configure the server using jboss-cli in embedded mode (no running server needed).
# - Enable environment variable substitution in deployment descriptors (required for
#   ${env.CARGO_ENDPOINT} in web.xml to be resolved at runtime)
# - Add the PostgreSQL datasource bound to the JNDI name the application expects
RUN $JBOSS_HOME/bin/jboss-cli.sh --commands="\
  embed-server,\
  /subsystem=ee:write-attribute(name=spec-descriptor-property-replacement,value=true),\
  /subsystem=datasources/data-source=CargoTrackerDS:add(\
    jndi-name=java:/jdbc/CargoTrackerDatabase,\
    driver-name=postgresql,\
    connection-url=jdbc:postgresql://\${env.CARGO_POSTGRES_HOST}:\${env.CARGO_POSTGRES_PORT}/\${env.CARGO_POSTGRES_NAME},\
    driver-class=org.postgresql.Driver,\
    user-name=\${env.CARGO_POSTGRES_USERNAME},\
    password=\${env.CARGO_POSTGRES_PASSWORD},\
    min-pool-size=2,\
    max-pool-size=20,\
    enabled=true,\
    use-java-context=true,\
    jta=false)"

# Ensure correct permissions
RUN chmod -R ug+rwX $JBOSS_HOME
