# A Sample OpenShift Pipeline for a Spring Boot Application

This example demonstrates how to implement a full end-to-end Jenkins Pipeline for a Java application in OpenShift Container Platform. This sample demonstrates the following capabilities:

* Deploying an integrated Jenkins server inside of OpenShift
* Running both custom and oob Jenkins slaves as pods in OpenShift
* "One Click" instantiation of a Jenkins Pipeline using OpenShift's Jenkins Pipeline Strategy feature
* Promotion of an application's container image within an OpenShift Cluster (using `oc tag`)
* Promotion of an application's container image to a separate OpenShift Cluster (using `skopeo`)
* Automated rollout using the [openshift-appler](https://github.com/redhat-cop/openshift-applier) project.

## Prerequisites

In order to run this pipeline, you will need:

* Two (2) OpenShift clusters, version 3.5 or greater
  * In this document, we will refer to the first cluster as *_Dev_* and the second as *_Prod_*.
* Ansible installed on your machine

## Automated Quickstart for image mirror

This quickstart can be deployed quickly using Ansible. Here are the steps.

1. Clone [this repo](https://github.com/redhat-cop/container-pipelines)
2. `cd container-pipelines/multi-cluster-spring-boot`
3. Run `ansible-galaxy install -r requirements.yml --roles-path=galaxy`
4. Log into your _Prod_ OpenShift cluster, and run the following command.
    ```
    $ oc login <prod cluster>
    ...
    $ ansible-playbook -i image-mirror-example/.applier/inventory-prod/ galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml
    ```
5. One of the things that was created by ansible is a `ServiceAccount` that will be used for promoting your app from _Dev_ to _Prod_. We'll need to extract its credentials so that our pipeline can use that account.
    ```
    $ TOKEN=$(oc serviceaccounts get-token docker-registry-prod -n multicluster-spring-boot-prod)
    ```
    The Ansible automation for your _Dev_ cluster will expect a parameters file to be created at `./applier/params/prod-credentials`. It should look something like this:
    ```
    $ echo "TOKEN=${TOKEN}
    SECRET_NAME=prod-credentials" > image-mirror-example/.applier/params/prod-credentials
    ```
6. We need to create the the *prod-api-credentials* param file so our pipeline will be able to verify a successful deployment to production.
    ```
    $ echo "TOKEN=${TOKEN}
    API_URL=<API_URL>
    REGISTRY_URL=<REGISTRY URL>
    SECRET_NAME=prod-cluster-credentials" > image-mirror-example/.applier/params/prod-cluster-credentials
    ```
6. Now, Log into your _Dev_ cluster, and instantiate the pre-pipeline configuration.
    ```
    $ oc login <dev cluster>
    ...
    $ ansible-playbook -i image-mirror-example/.applier/inventory-pre-dev/ galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml
    ```
7. Now the service account for the dev cluster docker registry has been created. We'll need to extract it's credentials so that our pipeline can authenticate to the dev cluster docker registry.
    ```
    $ TOKEN=$(oc serviceaccounts get-token docker-registry-dev -n multicluster-spring-boot-stage)
    $ echo "TOKEN=${TOKEN}
      SECRET_NAME=nonprod-credentials" > image-mirror-example/.applier/params/nonprod-credentials
    ```
8. Now, we will instantiate the pipeline and all configuration in the non-production cluster.
   ```
   $ ansible-playbook -i image-mirror-example/.applier/inventory-dev/ galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml
   ```

At this point you should have 3 projects deployed (`multicluster-spring-boot-dev`, `multicluster-spring-boot-stage`, and `multicluster-spring-boot-prod`) with our [Spring Rest](https://github.com/redhat-cop/spring-rest) demo application deployed to all 3.

## Automated Quickstart for skopeo

This quickstart can be deployed quickly using Ansible. Here are the steps.

1. Clone [this repo](https://github.com/redhat-cop/container-pipelines)
2. `cd container-pipelines/multi-cluster-spring-boot`
3. Run `ansible-galaxy install -r requirements.yml --roles-path=galaxy`
4. Log into your _Prod_ OpenShift cluster, and run the following command.
    ```
    $ oc login <prod cluster>
    ...
    $ ansible-playbook -i skopeo-example/.applier/inventory-prod/ galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml
    ```
5. One of the things that was created by ansible is a `ServiceAccount` that will be used for promoting your app from _Dev_ to _Prod_. We'll need to extract its credentials so that our pipeline can use that account.
    ```
    TOKEN=$(oc serviceaccounts get-token promoter -n multicluster-spring-boot-prod)
    ```
    The Ansible automation for your _Dev_ cluster will expect a parameters file to be created at `./applier/params/prod-credentials`. It should look something like this:
    ```
    $ echo "TOKEN=${TOKEN}
    API_URL=https://master.example.com
    REGISTRY_URL=docker-registry-default.apps.example.com
    " > skopeo-example/.applier/params/prod-credentials
    ```
6. Now, Log into your _Dev_ cluster, and instantiate the pipeline.
    ```
    $ oc login <dev cluster>
    ...
    $ ansible-playbook -i skopeo-example/.applier/inventory-dev/ galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml
    ```

At this point you should have 3 projects deployed (`multicluster-spring-boot-dev`, `multicluster-spring-boot-stage`, and `multicluster-spring-boot-prod`) with our [Spring Rest](https://github.com/redhat-cop/spring-rest) demo application deployed to all 3.

## Architecture

The following breaks down the architecture of the pipeline deployed, as well as walks through the manual deployment steps

### OpenShift Templates

The components of this pipeline are divided into two templates.

The first template, `applier/templates/build.yml` is what we are calling the "Build" template. It contains:

* A `jenkinsPipelineStrategy` BuildConfig
* An `s2i` BuildConfig
* An ImageStream for the s2i build config to push to

The build template contains a default source code repo for a java application compatible with this pipelines architecture (https://github.com/redhat-cop/spring-rest).

The second template, `applier/templates/deployment.yml` is the "Deploy" template. It contains:

* A tomcat8 DeploymentConfig
* A Service definition
* A Route

The idea behind the split between the templates is that I can deploy the build template only once (to my dev project) and that the pipeline will promote my image through all of the various stages of my application's lifecycle. The deployment template gets deployed once to each of the stages of the application lifecycle (once per OpenShift project).

### Pipeline Script

This project includes a sample `Jenkinsfile` pipeline script that could be included with a Java project in order to implement a basic CI/CD pipeline for that project, under the following assumptions:

* The project is built with Maven
* The `Jenkinsfile` script is placed in the same directory as the `pom.xml` file in the git source.
* The OpenShift projects that represent the Application's lifecycle stages are of the naming format: `<app-name>-dev`, `<app-name>-stage`, `<app-name>-prod`.

For convenience, this pipeline script is already included in the following git repository, based on our [Spring Boot Demo App](https://github.com/redhat-cop/spring-rest) app.

https://github.com/redhat-cop/spring-rest

## Manual Deployment Instructions

### 1. Create Lifecycle Stages

For the purposes of this demo, we are going to create three stages for our application to be promoted through.

- `multicluster-spring-boot-dev`
- `multicluster-spring-boot-stage`
- `multicluster-spring-boot-prod`

In the spirit of _Infrastructure as Code_ we have a YAML file that defines the `ProjectRequests` for us. This is as an alternative to running `oc new-project`, but will yeild the same result.

```
$ oc create -f applier/projects/projects.yml
projectrequest "multicluster-spring-boot-dev" created
projectrequest "multicluster-spring-boot-stage" created
projectrequest "multicluster-spring-boot-prod" created
```

### 2. Stand up Jenkins master in dev

For this step, the OpenShift default template set provides exactly what we need to get jenkins up and running.

```
$ oc process openshift//jenkins-ephemeral | oc apply -f- -n multicluster-spring-boot-dev
route "jenkins" created
deploymentconfig "jenkins" created
serviceaccount "jenkins" created
rolebinding "jenkins_edit" created
service "jenkins-jnlp" created
service "jenkins" created
```

### 4. Instantiate Pipeline

A _deploy template_ is provided at `applier/templates/deployment.yml` that defines all of the resources required to run our Tomcat application. It includes:

* A `Service`
* A `Route`
* An `ImageStream`
* A `DeploymentConfig`
* A `RoleBinding` to allow Jenkins to deploy in each namespace.

This template should be instantiated once in each of the namespaces that our app will be deployed to. For this purpose, we have created a param file to be fed to `oc process` to customize the template for each environment.

Deploy the deployment template to all three projects.
```
$ oc process -f applier/templates/deployment.yml --param-file=applier/params/deployment-dev | oc apply -f-
service "spring-rest" created
route "spring-rest" created
imagestream "spring-rest" created
deploymentconfig "spring-rest" created
rolebinding "jenkins_edit" configured
$ oc process -f applier/templates/deployment.yml --param-file=applier/params/deployments-stage | oc apply -f-
service "spring-rest" created
route "spring-rest" created
imagestream "spring-rest" created
deploymentconfig "spring-rest" created
rolebinding "jenkins_edit" created
$ oc process -f applier/templates/deployment.yml --param-file=applier/params/deployment-prod | oc apply -f-
service "spring-rest" created
route "spring-rest" created
imagestream "spring-rest" created
deploymentconfig "spring-rest" created
rolebinding "jenkins_edit" created
```

A _build template_ is provided at `applier/templates/build.yml` that defines all the resources required to build our java app. It includes:

* A `BuildConfig` that defines a `JenkinsPipelineStrategy` build, which will be used to define out pipeline.
* A `BuildConfig` that defines a `Source` build with `Binary` input. This will build our image.

Deploy the pipeline template in dev only.
```
$ oc process -f applier/templates/build.yml --param-file applier/params/build-dev | oc apply -f-
buildconfig "spring-rest-pipeline" created
buildconfig "spring-rest" created
```

At this point you should be able to go to the Web Console and follow the pipeline by clicking in your `multicluster-spring-boot-dev` project, and going to *Builds* -> *Pipelines*. At several points you will be prompted for input on the pipeline. You can interact with it by clicking on the _input required_ link, which takes you to Jenkins, where you can click the *Proceed* button. By the time you get through the end of the pipeline you should be able to visit the Route for your app deployed to the `myapp-prod` project to confirm that your image has been promoted through all stages.

## Cleanup

Cleaning up this example is as simple as deleting the projects we created at the beginning.

```
oc delete project multicluster-spring-boot-dev multicluster-spring-boot-prod multicluster-spring-boot-stage
```
