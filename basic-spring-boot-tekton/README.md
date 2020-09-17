# A Sample Tekton Pipeline for a Spring Boot Application

This example demonstrates how to implement a full end-to-end [Tekton](https://tekton.dev/) Pipeline for a Java application in OpenShift Container Platform. This sample demonstrates the following capabilities:

* Deploying a Tekton pipeline via applier
* Building an promoting an application with a tekton pipeline

## Prerequisites


* One OpenShift Container Platform Clusters
  * OpenShift 4.1+ is required
  * [Red Hat OpenJDK 1.8](https://access.redhat.com/containers/?tab=overview#/registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift) image is required
* Access to GitHub
* [Tekton Operator](https://github.com/openshift/tektoncd-pipeline-operator) In order to use Tekton pipelines you need to install the Tekton operator (at least version v1.0.1). To do that go to ***Catalog***-> ***Operator Hub*** page of you admin console. Look for ***OpenShift Pipelines Operator*** and install it. The default settings are fine for this demo. You can also automate the installation of the Tekton operator, but this is outside the scope of this demo.
* [Tekton CLI](https://github.com/tektoncd/cli)
* *Highly recommended*: [tekton dashboard](https://github.com/tektoncd/dashboard)

## Automated Deployment

This quickstart can be deployed quickly using Ansible. Here are the steps.

1. Clone [this repo](https://github.com/redhat-cop/container-pipelines)
2. `cd container-pipelines/basic-spring-boot-tekton`
3. Run `ansible-galaxy install -r requirements.yml --roles-path=galaxy`
4. Log into an OpenShift cluster, then run the following command.

```shell
$ ansible-playbook -i ./.applier/ galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml
```

At this point you should have 4 projects created (`basic-spring-boot-build`, `basic-spring-boot-dev`, `basic-spring-boot-stage`, and `basic-spring-boot-prod`) with a pipeline in the `-build` project, and our [Spring Rest](https://github.com/redhat-cop/spring-rest) demo app deployed to the dev/stage/prod projects.

## Architecture

The following breaks down the architecture of the pipeline deployed, as well as walks through the manual deployment steps

### OpenShift Templates

The components of this pipeline are divided into two templates.

The first template, `.openshift/templates/build.yml` is what we are calling the "Build" template. It contains:

* A Tekton pipeline and associated objects
* An `s2i` BuildConfig
* An ImageStream for the s2i build config to push to

The build template contains a default source code repo for a [java application](https://github.com/redhat-cop/spring-rest) compatible with this pipelines architecture .

The second template, `.openshift/templates/deployment.yml` is the "Deploy" template. It contains:

* A tomcat8 DeploymentConfig
* A Service definition
* A Route

The idea behind the split between the templates is that I can deploy the build template only once (to my build project) and that the pipeline will promote my image through all of the various stages of my application's lifecycle. The deployment template gets deployed once to each of the stages of the application lifecycle (once per OpenShift project).

### Pipeline Script

This project includes a sample `Tekton` pipeline script that could be included with a Java project in order to implement a basic CI/CD pipeline for that project, under the following assumptions:

* The project is built with Maven
* The OpenShift projects that represent the Application's lifecycle stages are of the naming format: `<app-name>-dev`, `<app-name>-stage`, `<app-name>-prod`.

This pipeline defaults to use our [Spring Boot Demo App](https://github.com/redhat-cop/spring-rest).

## Manual Deployment Instructions

### 1. Create Lifecycle Stages

For the purposes of this demo, we are going to create three stages for our application to be promoted through.

* `basic-spring-boot-build`
* `basic-spring-boot-dev`
* `basic-spring-boot-stage`
* `basic-spring-boot-prod`

In the spirit of _Infrastructure as Code_ we have a YAML file that defines the `ProjectRequests` for us. This is as an alternative to running `oc new-project`, but will yeild the same result.

```shell
$ oc create -f .openshift/projects/projects.yml
projectrequest "basic-spring-boot-build" created
projectrequest "basic-spring-boot-dev" created
projectrequest "basic-spring-boot-stage" created
projectrequest "basic-spring-boot-prod" created
```

### 2. Instantiate Pipeline

A _deploy template_ is provided at `.openshift/templates/deployment.yml` that defines all of the resources required to run our Tomcat application. It includes:

* A `Service`
* A `Route`
* An `ImageStream`
* A `DeploymentConfig`
* A `RoleBinding` to allow Jenkins to deploy in each namespace.

This template should be instantiated once in each of the namespaces that our app will be deployed to. For this purpose, we have created a param file to be fed to `oc process` to customize the template for each environment.

Deploy the deployment template to all three projects.

```shell
$ oc process -f .openshift/templates/deployment.yml -p APPLICATION_NAME=basic-spring-boot \
 -p NAMESPACE=basic-spring-boot-dev -p SA_NAMESPACE=basic-spring-boot-build -p READINESS_PATH="/health" \
 -p READINESS_RESPONSE="status.:.UP" | oc apply -f -
service "spring-rest" created
route "spring-rest" created
imagestream "spring-rest" created
deploymentconfig "spring-rest" created
rolebinding "tekton_edit" configured
$ oc process -f .openshift/templates/deployment.yml -p APPLICATION_NAME=basic-spring-boot \
 -p NAMESPACE=basic-spring-boot-stage -p SA_NAMESPACE=basic-spring-boot-build -p READINESS_PATH="/health" \
 -p READINESS_RESPONSE="status.:.UP" | oc apply -f -
service "spring-rest" created
route "spring-rest" created
imagestream "spring-rest" created
deploymentconfig "spring-rest" created
rolebinding "tekton_edit" created
$ oc process -f .openshift/templates/deployment.yml -p APPLICATION_NAME=basic-spring-boot \
 -p NAMESPACE=basic-spring-boot-prod -p SA_NAMESPACE=basic-spring-boot-build -p READINESS_PATH="/health" \
-p READINESS_RESPONSE="status.:.UP" | oc apply -f -
service "spring-rest" created
route "spring-rest" created
imagestream "spring-rest" created
deploymentconfig "spring-rest" created
rolebinding "tekton_edit" created
```

A _build template_ is provided at `.openshift/templates/build.yml` that defines all the resources required to build our java app. It includes:

* A `Tekton` pipeline and associated objects.
* A `BuildConfig` that defines a `Source` build with `Binary` input. This will build our image.

Deploy the pipeline template in build only.

```shell
$ oc process -f .openshift/templates/build.yml -p APPLICATION_NAME=basic-spring-boot \
 -p NAMESPACE=basic-spring-boot-build -p SOURCE_REPOSITORY_URL="https://github.com/redhat-cop/container-pipelines.git" \
 -p APPLICATION_SOURCE_REPO="https://github.com/redhat-cop/spring-rest.git" | oc apply -f -
serviceaccount/tekton created
rolebinding.rbac.authorization.k8s.io/tekton_edit created
imagestream.image.openshift.io/basic-spring-boot created
pipelineresource.tekton.dev/basic-spring-boot-image created
pipelineresource.tekton.dev/basic-spring-boot-git created
task.tekton.dev/maven-build-binary-build created
task.tekton.dev/deploy created
pipeline.tekton.dev/basic-spring-boot-pipeline created
buildconfig.build.openshift.io/basic-spring-boot created
```

## Running the pipelines

Once you have deployed the needed infrastructure either with applier or manually, you can run the pipeline by issuing the following command:

```shell
$ tkn pipeline start basic-spring-boot-pipeline \
  -r basic-spring-boot-git=basic-spring-boot-git -s pipeline \
  -n basic-spring-boot-build
```

## Cleanup

Cleaning up this example is as simple as deleting the projects we created at the beginning.

```shell
$ oc delete project basic-spring-boot-build basic-spring-boot-dev basic-spring-boot-prod basic-spring-boot-stage
```
