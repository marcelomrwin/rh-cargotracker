# Stage 1: Provision the JBoss EAP 8 server using Galleon
FROM registry.redhat.io/jboss-eap-8/eap8-openjdk17-builder-openshift-rhel8:latest AS builder

# Galleon feature packs and layers to provision
ENV GALLEON_PROVISION_FEATURE_PACKS org.jboss.eap:wildfly-ee-galleon-pack,org.jboss.eap.cloud:eap-cloud-galleon-pack,org.jboss.eap:eap-datasources-galleon-pack
ENV GALLEON_PROVISION_LAYERS cloud-default-config,embedded-activemq,postgresql-driver,postgresql-datasource

# Specify the JBoss EAP channel (version)
ENV GALLEON_PROVISION_CHANNELS org.jboss.eap.channels:eap-8.0

# PostgreSQL driver version (used by the postgresql-driver Galleon layer)
ENV POSTGRESQL_DRIVER_VERSION 42.7.5

# Run the S2I assemble script to provision the EAP server.
# This also executes extensions/postconfigure.sh, which runs extensions/extensions.cli
# to configure the datasource and enable spec-descriptor-property-replacement.
RUN /usr/local/s2i/assemble


# Stage 2: Assemble the runtime image
FROM registry.redhat.io/jboss-eap-8/eap8-openjdk17-runtime-openshift-rhel8:latest AS runtime

# Copy the provisioned server from the builder stage
COPY --from=builder --chown=jboss:root $JBOSS_HOME $JBOSS_HOME

# Copy the application WAR
# The PostgreSQL driver module is installed by Galleon (postgresql-driver layer)
COPY --chown=jboss:root deployments/*.war $JBOSS_HOME/standalone/deployments

# Ensure correct permissions
RUN chmod -R ug+rwX $JBOSS_HOME
