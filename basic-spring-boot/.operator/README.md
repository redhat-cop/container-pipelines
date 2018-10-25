# Sample Spring Boot Operator

Operators are a powerful mechanism to manage the configuration of Kubernetes
resources in a "Kubernetes Native" way. This directory has a sample of using
the Helm Operator Kit to create a `cop.redhat.com/v1.SpringBootApp` operator that deploys 
`basic-spring-boot` as well as the required services, routes, etc.

The templates for the managed resources are in `basic-spring-boot/templates`.
`basic-spring-boot/values.yaml` defines the parameters that can be passed to 
the operator and their default values.

## Build This Operator

A build of this operator has already been published to 
`quay.io/cpitman/spring-boot-operator-demo`. This image can be used, or you can
build your own:

To build this operator, do a docker build using `Dockerfile` with this 
directory as the context root. Then push this image to a docker registry
and update `deploy/operator.yaml` to point to the updated registry. 

## Install This Operator

To install the operator, a cluster admin will need to create `deploy/crd.yaml`.
This defines the `SpringBootApp` custom resource.

Then, within a namespace/project, a user with at least `edit` access should 
create `deploy/rbac.yaml` and `deploy/operator.yaml`. This is the namespace 
that the operator will watch for custom resources in.

## Using the Operator

Once the operator is installed, you can create custom resources in the same
namespace/project. You can use `deploy/cr.yaml`, which has the following 
content:

```
apiVersion: cop.redhat.com/v1alpha1
kind: SpringBootApp
metadata:
  name: testapp
spec:
  applicationName: spring-rest
  readinessResponse: status.:.UP
  readinessPath: /health
```

After creating the resource, a DeploymentConfig, ImageStream, Service, and 
Route will be created. 

You can now update the `SpringBootApp` resource, and those changes will 
propagate to all managed resources. If you edit the managed resources, the
operator will enforce the required configuration by changing it back. Finally,
When the resource is deleted, then all managed resources are automatically 
cleaned up.

