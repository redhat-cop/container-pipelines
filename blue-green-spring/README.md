# A Sample OpenShift Pipeline for Blue Green deployments

This example demonstrates how to implement a full end-to-end Jenkins Pipeline for a Java application in a Blue/Green deployment in the OpenShift Container Platform. The pipleine will create two instances of the application in the Production namespace.  There will be three routes in the namespace; a blue, green and blue-green route.  The blue-green route will switch to the latest deployment when the pipeline completes.  This allows for tesing of the new deployment prior to switching live traffic.  Also, the previous deployment can be used to compare the previous deployment.

This sample demonstrates the following capabilities:

* Deploying an integrated Jenkins server inside of OpenShift
* Running both custom and oob Jenkins slaves as pods in OpenShift
* "One Click" instantiation of a Jenkins Pipeline using OpenShift's Jenkins Pipeline Strategy feature
* Promotion of an application's container image within an OpenShift Cluster (using `oc tag`)
* Tagging images with the current version of the artifact defined in the pom.xml file
* Promotion of an application's container image to a blue/green production configuration
* Switching production routes between blue and green deployments after confirmation
* Automated rollout using the [openshift-applier](https://github.com/redhat-cop/openshift-applier/tree/master/roles/openshift-applier) Ansible role.

## Automated Deployment

Run the following commands to instantiate this example.
1. Clone [this repo](https://github.com/redhat-cop/container-pipelines)
2. `cd container-pipelines/blue-green-spring`
3. Run ansible-galaxy to install required Ansible roles

    ```
    ansible-galaxy install -r requirements.yml --roles-path=galaxy
    ```

4. Run the following Ansible playbook to install all the necessary projects and OpenShift objects as well as a Jenkins instance that will build, promote and deploy the application.

    ```
    ansible-playbook -i .applier galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml
    ```

## Architecture

### OpenShift Templates

The components of this pipeline are divided into three templates.

The first template, `files/builds/template.yml` is what we are calling the "Build" template. It contains:

* A `jenkinsPipelineStrategy` BuildConfig
* An `s2i` BuildConfig
* An ImageStream for the s2i build config to push to

The build template contains a default source code repo for a java application compatible with this pipelines architecture (https://github.com/malacourse/simple-spring-boot-web).

The second template, `files/deployment/template.yml` is the "Deploy" template. It contains:

* A openjdk8 DeploymentConfig
* A Service definition
* A Route

The third template, `files/deployment/template-bg.yml` is the "Deploy" template for a blue/green project. It contains:

* Two openjdk8 DeploymentConfig's
* Two Service definition's
* A Route

The idea behind the split between the templates is that I can deploy the build template only once (to my build project) and that the pipeline will promote my image through all of the various stages of my application's lifecycle. The deployment template gets deployed once to each of the stages of the application lifecycle (once per OpenShift project).

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

For the purposes of this demo, we are going to create four stages for our application to be promoted through.

- `spring-boot-web-build`
- `spring-boot-web-dev`
- `spring-boot-web-stage`
- `spring-boot-web-prod`

In the spirit of _Infrastructure as Code_ we have a YAML file that defines the `ProjectRequests` for us. This is as an alternative to running `oc new-project`, but will yeild the same result.

### 2. Stand up Jenkins master in build

For this step, the OpenShift default template set provides exactly what we need to get jenkins up and running. Jenkins will be running in the `build` project and promote and deploy to the `spring-boot-web-dev` project.

### 4. Instantiate Pipeline

A _deploy template_ is provided at `files/deployment/template.yml` that defines all of the resources required to run the openjdk8 application. It includes:

* A `Service`
* A `Route`
* An `ImageStream`
* A `DeploymentConfig`
* A `RoleBinding` to allow Jenkins to deploy in each namespace.

This template should be instantiated once in each of the lower level namespaces (dev, stage) that our app will be deployed to. For this purpose, we have created a param file to be fed to `oc process` to customize the template for each environment.

A production blue/green _deploy template_ is provided at `deploy/simple-spring-boot-template-prod.yml` that defines all of the resources required to run the openjdk8 application. It includes:

* Two `Service's`
* Three `Route's` a blue route, green route and main route that switches between the two deployments/services.
* Two `ImageStream's`
* Two `DeploymentConfig's`
* A `RoleBinding` to allow Jenkins to deploy in each namespace.

This template should be instantiated in the production blue/green namespace that our app will be deployed to. For this purpose, we have created a param file to be fed to `oc process` to customize the template for each environment.

A _build template_ is provided at `builds/template.yml` that defines all the resources required to build our java app. It includes:

* A `BuildConfig` that defines a `JenkinsPipelineStrategy` build, which will be used to define out pipeline.
* A `BuildConfig` that defines a `Source` build with `Binary` input. This will build our image.

At this point you should be able to go to the Web Console and follow the pipeline by clicking in your `spring-boot-web-build` project, and going to *Builds* -> *Pipelines*. There is a prompt for input on the pipeline before the production route is switched to the new deployment. You can interact with it by clicking on the _input required_ link, which takes you to Jenkins, where you can click the *Proceed* button. By the time you get through the end of the pipeline you should be able to visit the Route for your app deployed to the `myapp-prod` project to confirm that your image has been promoted through all stages.

## Cleanup

Cleaning up this example is as simple as deleting the projects we created at the beginning.

```
oc delete project spring-boot-web-build spring-boot-web-dev spring-boot-web-prod spring-boot-web-stage
```
