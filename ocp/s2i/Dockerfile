# Stage 1: Provision the JBoss EAP 8.1 server using Galleon
# Source: "Using JBoss EAP on OpenShift Container Platform" - Table 9.1 (EAP 8.1 image names)
# Source: "Provisioning JBoss EAP" - Chapter 3.2 (OpenShift provisioning with eap-maven-plugin)
FROM registry.redhat.io/jboss-eap-8/eap81-openjdk17-builder-openshift-rhel9:latest AS builder

# Galleon feature packs required for OpenShift provisioning
# - wildfly-ee-galleon-pack: core EAP/Jakarta EE capabilities
# - eap-cloud-galleon-pack: OpenShift-specific configuration (cloud-server layer, health checks, etc.)
# - eap-datasources-galleon-pack: PostgreSQL driver layer
ENV GALLEON_PROVISION_FEATURE_PACKS org.jboss.eap:wildfly-ee-galleon-pack,org.jboss.eap.cloud:eap-cloud-galleon-pack,org.jboss.eap:eap-datasources-galleon-pack

# Galleon layers to include in the provisioned server
ENV GALLEON_PROVISION_LAYERS cloud-default-config,embedded-activemq,postgresql-driver,postgresql-datasource

# JBoss EAP 8.1 channel
ENV GALLEON_PROVISION_CHANNELS org.jboss.eap.channels:eap-8.1

# PostgreSQL driver version (used by the postgresql-driver Galleon layer)
ENV POSTGRESQL_DRIVER_VERSION 42.7.5

# Run the S2I assemble script to provision the EAP 8.1 server.
# This also executes extensions/postconfigure.sh, which runs extensions/extensions.cli
# to enable spec-descriptor-property-replacement for runtime env var resolution in web.xml.
RUN /usr/local/s2i/assemble


# Stage 2: Assemble the runtime image
FROM registry.redhat.io/jboss-eap-8/eap81-openjdk17-runtime-openshift-rhel9:latest AS runtime

# Copy the provisioned server from the builder stage
COPY --from=builder --chown=jboss:root $JBOSS_HOME $JBOSS_HOME

# Copy the application WAR
# The PostgreSQL driver module is installed by Galleon (postgresql-driver layer)
COPY --chown=jboss:root deployments/*.war $JBOSS_HOME/standalone/deployments

# Ensure correct permissions
RUN chmod -R ug+rwX $JBOSS_HOME
