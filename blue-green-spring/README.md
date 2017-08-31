# A Sample OpenShift Pipeline for Blue Green deployments

This example demonstrates how to implement a full end-to-end Jenkins Pipeline for a Java application in a Blue/Green deployment in the OpenShift Container Platform. This sample demonstrates the following capabilities:

* Deploying an integrated Jenkins server inside of OpenShift
* Running both custom and oob Jenkins slaves as pods in OpenShift
* "One Click" instantiation of a Jenkins Pipeline using OpenShift's Jenkins Pipeline Strategy feature
* Promotion of an application's container image within an OpenShift Cluster (using `oc tag`)
* Tagging images with the current version of the artifact defined in the pom.xml file
* Promotion of an application's container image to a blue/green production configuration
* Switching production routes between blue and green deployments after confirmation

## Quickstart

Run the following commands to instantiate this example.

```
cd ./simple-spring-boot
oc create -f projects/projects.yml
oc process openshift//jenkins-ephemeral | oc apply -f- -n simple-spring-boot-dev
oc process -f deploy/simple-spring-boot-template.yml --param-file=deploy/dev/params | oc apply -f-
oc process -f deploy/simple-spring-boot-template.yml --param-file=deploy/stage/params | oc apply -f-
oc process -f deploy/simple-spring-boot-template-prod.yml --param-file=deploy/prod/params | oc apply -f-
oc process -f build/basic-java-template.yml --param-file build/dev/params | oc apply -f-
```

## Architecture

### OpenShift Templates

The components of this pipeline are divided into three templates.

The first template, `build/simple-spring-boot-template.yml` is what we are calling the "Build" template. It contains:

* A `jenkinsPipelineStrategy` BuildConfig
* An `s2i` BuildConfig
* An ImageStream for the s2i build config to push to

The build template contains a default source code repo for a java application compatible with this pipelines architecture (https://github.com/malacourse/simple-spring-boot-web).

The second template, `deploy/simple-spring-boot-template.yml` is the "Deploy" template. It contains:

* A openjdk8 DeploymentConfig
* A Service definition
* A Route

The third template, `deploy/simple-spring-boot-template-prod.yml` is the "Deploy" template for a blue/green project. It contains:

* Two openjdk8 DeploymentConfig's
* Two Service definition's
* A Route

The idea behind the split between the templates is that I can deploy the build template only once (to my dev project) and that the pipeline will promote my image through all of the various stages of my application's lifecycle. The deployment template gets deployed once to each of the stages of the application lifecycle (once per OpenShift project).

### Pipeline Script

This project includes a sample `pipeline.groovy` Jenkins Pipeline script that could be included with a Java project in order to implement a basic CI/CD pipeline for that project, under the following assumptions:

* The project is built with Maven
* The `pipeline.groovy` script is placed in the same directory as the `pom.xml` file in the git source.
* The OpenShift projects that represent the Application's lifecycle stages are of the naming format: `<app-name>-dev`, `<app-name>-stage`, `<app-name>-prod`.

For convenience, this pipeline script is already included in the following git repository, based on a [Simple Spring Boot Web app](https://github.com/malacourse/simple-spring-boot-web) app.  The app displays a message that will change color based on which deployment is live in the production project.

https://github.com/malacourse/simple-spring-boot-web

## Bill of Materials

* One OpenShift Container Platform Clusters
  * OpenShift 3.5+ is required.
* Access to GitHub

## Implementation Instructions

### 1. Create Lifecycle Stages

For the purposes of this demo, we are going to create three stages for our application to be promoted through.

- `simple-spring-boot-dev`
- `simple-spring-boot-stage`
- `simple-spring-boot-prod`

In the spirit of _Infrastructure as Code_ we have a YAML file that defines the `ProjectRequests` for us. This is as an alternative to running `oc new-project`, but will yeild the same result.

```
$ oc create -f projects/projects.yml
projectrequest "simple-spring-boot-dev" created
projectrequest "simple-spring-boot-stage" created
projectrequest "simple-spring-boot-prod" created
```

### 2. Stand up Jenkins master in dev

For this step, the OpenShift default template set provides exactly what we need to get jenkins up and running.

```
$ oc process openshift//jenkins-ephemeral | oc apply -f- -n simple-spring-boot-dev
route "jenkins" created
deploymentconfig "jenkins" created
serviceaccount "jenkins" created
rolebinding "jenkins_edit" created
service "jenkins-jnlp" created
service "jenkins" created
```

### 4. Instantiate Pipeline

A _deploy template_ is provided at `deploy/simple-spring-boot-template.yml` that defines all of the resources required to run the openjdk8 application. It includes:

* A `Service`
* A `Route`
* An `ImageStream`
* A `DeploymentConfig`
* A `RoleBinding` to allow Jenkins to deploy in each namespace.

This template should be instantiated once in each of the lower level namespaces (dev, stage,qa) that our app will be deployed to. For this purpose, we have created a param file to be fed to `oc process` to customize the template for each environment.

A production blue/green_deploy template_ is provided at `deploy/simple-spring-boot-template-prod.yml` that defines all of the resources required to run the openjdk8 application. It includes:

* Two `Service's` 
* A `Route`
* Two `ImageStream's`
* Two `DeploymentConfig's`
* A `RoleBinding` to allow Jenkins to deploy in each namespace.

This template should be instantiated in the production blue/green namespace that our app will be deployed to. For this purpose, we have created a param file to be fed to `oc process` to customize the template for each environment.

Deploy the deployment template to all three projects.
```
$ oc process -f deploy/simple-spring-boot-template.yml --param-file=deploy/dev/params | oc apply -f-
service "simple-spring-boot" created
route "simple-spring-boot" created
imagestream "simple-spring-boot" created
deploymentconfig "simple-spring-boot" created
rolebinding "jenkins_edit" configured
$ oc process -f deploy/simple-spring-boot-template.yml --param-file=deploy/stage/params | oc apply -f-
service "simple-spring-boot" created
route "simple-spring-boot" created
imagestream "simple-spring-boot" created
deploymentconfig "simple-spring-boot" created
rolebinding "jenkins_edit" created
$ oc process -f deploy/simple-spring-boot-template-prod.yml --param-file=deploy/prod/params | oc apply -f-
service "simple-spring-boot" created
route "simple-spring-boot" created
imagestream "simple-spring-boot" created
deploymentconfig "simple-spring-boot" created
rolebinding "jenkins_edit" created
```

A _build template_ is provided at `build/basic-java-template.yml` that defines all the resources required to build our java app. It includes:

* A `BuildConfig` that defines a `JenkinsPipelineStrategy` build, which will be used to define out pipeline.
* A `BuildConfig` that defines a `Source` build with `Binary` input. This will build our image.

Deploy the pipeline template in dev only.
```
$ oc process -f build/basic-java-template.yml --param-file build/dev/params | oc apply -f-
buildconfig "simple-spring-boot-pipeline" created
buildconfig "simple-spring-boot" created
```

At this point you should be able to go to the Web Console and follow the pipeline by clicking in your `myapp-dev` project, and going to *Builds* -> *Pipelines*. There is a prompt for input on the pipeline before the production route is switched to the new deployment. You can interact with it by clicking on the _input required_ link, which takes you to Jenkins, where you can click the *Proceed* button. By the time you get through the end of the pipeline you should be able to visit the Route for your app deployed to the `myapp-prod` project to confirm that your image has been promoted through all stages.

## Cleanup

Cleaning up this example is as simple as deleting the projects we created at the beginning.

```
oc delete project simple-spring-boot-dev simple-spring-boot-prod simple-spring-boot-stage
```
