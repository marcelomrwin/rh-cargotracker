OpenShift Deployment
===============================================================================

## Create project

Create a new project:

    oc new-project cargo-tracker-eap8 \
      --display-name="Cargo Tracker App" \
      --description="Reference java ee 7 application running on top of EAP 8.1"

> [!NOTE]
> If you use a different name, update the `KUBERNETES_NAMESPACE` variable in `ocp/kustomization.yaml` accordingly.

## Local Java build

Build the WAR file locally. The Red Hat Maven GA repository is required for the EAP 8 BOM:

    mvn clean package -DskipTests -Pjboss-ga-repository

## Remote Binary build

Build the container image in OpenShift using a Docker binary build:

    cp target/cargo-tracker.war ocp/s2i/deployments/
    oc new-build --strategy docker --binary --name=cargo-tracker
    oc start-build cargo-tracker --from-dir ocp/s2i/ --follow

> [!NOTE]
> `oc new-build` only needs to run once to create the BuildConfig. For subsequent builds, use only `oc start-build`.

Update the imagestream so the deployment can pull the image from the OpenShift internal registry:

    oc patch imagestream cargo-tracker --type merge -p '{"spec":{"lookupPolicy":{"local":true}}}'

## Deploy the manifest

JGroups Discovery Mechanism requires access to the Kubernetes REST API.

Grant view role to the default service account:

    oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)

Deploy all resources (Deployment, Service, Route, PostgreSQL, ConfigMaps, Secrets):

    oc apply -k ocp

## Test the application

Open the application in your web browser:

    open "https://$(oc get route cargo-tracker --template='{{ .spec.host }}')/cargo-tracker"

## Documentation References

[Datasource](https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.0/html/using_jboss_eap_on_openshift_container_platform/assembly_reference-information-for-openshift-container-platform_default#ref_openshift-datasources_assembly_reference-information-for-openshift-container-platform)

[Driver and Datasource configuration via Galleon layer](https://github.com/jbossas/eap-datasources-galleon-pack/blob/main/doc/postgresql/README.md)
