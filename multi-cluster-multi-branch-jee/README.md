# Sample Multi-Cluster with GitFlow Pipeline for JEE Application
This example demonstrates and provides a framework for creating a Multi-Cluster Pipeline using GitFlow source control patterns. This sample demonstrates the following capabilities:

* reusable pipeline groovy function for use with multiple JEE applications
* Multi-cluster promotion ready by specifying remote cluster credentials and remote image repository credentials
* Designed to work with external image repository such as Red Hat Quay, Sonatype Nexus, or JFrog Artifactory.
* Stages include
  * SETUP
  * BUILD
  * DEV
  * TEST
  * QA
  * PROD
* Example stubbed out openshift-applier inventories for fully infrastructure as code using openshift-applier and Ansible. Pipeline is capability of building ALL required OpenShift projects and resources on demand on the fly as part of the pipeline.

## Prerequisites
In order to run this pipeline, you will need:

* Jenkins
* One (1) or more OpenShift clusters
  * Tested on OpenShift 3.11, but likely works on older and newer versions
* OpenShift Service Account credentials for each OpenShift cluster with permissions to create Projects
* OpenShift user group that exists in all OpenShift clusters to give owner permissions to for generated OpenShift resources
* External image repository service account credentials with permissions to pull images
* External image repository service account credentials with permissions to push images
* Optionally: configured SonarQube Environment in Jenkins

## Deployment Instructions

### 1. Get/Create OpenShift Service Account credentials for each OpenShift cluster that can create projects
There are many ways to go about this, this is just one option.

```
CLUSTER_API=
SERVICE_ACCOUNT_NAMESPACE=jenkins
SERVICE_ACCOUNT_NAME=jenkins

oc login ${CLUSTER_API}
oc new-project ${SERVICE_ACCOUNT_NAMESPACE}
oc create sa ${SERVICE_ACCOUNT_NAME} -n ${SERVICE_ACCOUNT_NAMESPACE}
oc adm policy add-cluster-role-to-user self-provisioners system:serviceaccount:${SERVICE_ACCOUNT_NAMESPACE}:jenkins

TOKEN_NAME=$(oc get secret -n ${SERVICE_ACCOUNT_NAMESPACE} | grep ${SERVICE_ACCOUNT_NAME}-token | head -n 1 | awk '{ print $1 }')
echo TOKEN_NAME=${TOKEN_NAME}
TOKEN=$(oc get secret ${TOKEN_NAME} --template='{{.data.token}}' -n ${SERVICE_ACCOUNT_NAMESPACE} | base64 --decode)
echo TOKEN=${TOKEN}
```

**NOTE** you will need the `TOKEN`(s) in step [6.2 update .applier/inventory/host_vars/app-build.yml](#62-update-applierinventoryhost_varsapp-buildyml)

### 2. Get Image Repository service account credential(s)
This will depend on your Image Repository tool of choice. At minimum you will need a single service account that can push images and pull images. Depending on your Image Repository tool you may need seperate accounts for push vs pull.

**NOTE** you will need the credentials in step [6.2 update .applier/inventory/host_vars/app-build.yml](#62-update-applierinventoryhost_varsapp-buildyml)

### 3. Copy and modify the pipelineJEE8.groovy file
The `pipelineJEE8.groovy file provided here is meant to be a reference for you to build on and add your own stages too or modify how the git branching and promotion process works.

At minimum you will need to:

1. search the file for `TODO` and resolve those items.`
2. copy the file into a "public on your network" shared git repository, example `my-orgs-pipeline-library` into a `vars` directory in that repository. This repository will be good for storing your shared jenkins pipelines as well as any other jenkins utils functions not already provided in [redhat-cop/pipeline-library](https://github.com/redhat-cop/pipeline-library).

### 4. Copy the example `Jenkinsfile.example` to your application and update

The provided `Jenkinsfile.example` file is meant to be a reference for how to call this pipeline from one of your applications.

1. copy `Jenkinsfile.example` to your JEE8 project
2. Edit the `library` `remote` URL
3. Optionally set the `library` `credentialsId`
4. set all the required parameters to pass to the `pipelineJEE8` function

### 5. Copy the `.openshift` directory to your application and update
The `.openshift` directory contains reference templates specific to this pipeline that don't otherwise belong in the more generic and shareable [redhat-cop/openshift-templates](https://github.com/redhat-cop/openshift-templates).

#### 5.1 `app-deploy-jboss-eap.yml`
This template defines the basic framework for deploying an image for a JEE application on JBoss EAP. It contains
* `DeploymentConfig`
* `Service` - http
* `Service` - ping
* `Route` - HTTPS with edge termination

This template is meant to be updated and/or replaced with the specifics for how your application deploys, for instance updating for HTTPS passthrough or passing in additional environment variables.

**NOTE** be sure that if you add additional parameters to this template that in step [6.3 update/copy .applier/inventory/host_vars/app-ENV_NAME.yml files](#63-updatecopy-applierinventoryhost_varsapp-env_nameyml-files) you update them to pass the new parameters.

### 6. Copy the `.applier` directory to your application and update

1. copy the `.applier` directory into your applications root directory as a peer to your `Jenkinsfile`

#### 6.1 update `.applier/inventory/hosts`
The example `.applier/inventory/hosts` file assumes a process of
1. build (required)
2. dev
3. test
4. qa
5. prod

If your promotion process includes or does not include any of these environments then adjust the host file accordingly.

#### 6.2 update `.applier/inventory/host_vars/app-build.yml`
This host vars file is mostly completely but it has a few `TODO`s to be filled in with vaulted variables containing your cluster credentials from step [1. Get/Create OpenShift Service Account credentials for each OpenShift cluster that can create projects](#1-getcreate-openshift-service-account-credentials-for-each-openshift-cluster-that-can-create-projects) and your image repository credentials from step [2. Get Image Repository service account credential(s)](#2-get-image-repository-service-account-credentials).

1. replace `TODO`s with required vaulted variables
   * **NOTE** all vaulated variables should be vaulted using the same Ansible Vault password
2. ADVANCED: add in any other additional steps to build OpenShift resources that your build process may required out of the standard process defined here

#### 6.3 update/copy `.applier/inventory/host_vars/app-ENV_NAME.yml` files
There are sample Ansible host vars files for `dev`, `test`, `qa`, and `prod` environments. In these examples the files are all but identical. The reason there are multiple rather than just one `app-deploy` is because in real world application each one tends to start to differ from each other with unique vaulted variables for things like DB passwords etc. In theory you could instead put those sorts of things in Jenkins credentials and then pass them through to a more generic `app-deploy` inventory, but then those credentials fall out of the everthing is code paradigm implemented here, hence the need for one host file per logical environment rather then just one generic one. You could possibly do some trickery with `group_vars` and `app-deploy` and the only having the differences in `host_vars`. It all gets to complicated for an example like this. It is suggested starting with this example, one file per environment, and playing with it from there.


1. update each `.applier/inventory/host_vars/app-ENV_NAME.yml` for the specifics of deploying your applications.
   * little to no updates should be needed to deploy a 'hello-world' application

### 7. Configure Jenkins
Jenkins must be setup to run this pipeline.

#### 7.1 Install required Jenkins plugins

* TBD

#### 7.2 Add Jenkins Credential for ansible vault password
You vaulted a bunch of passwords/etc in your Ansible inventories, in theory they were all vaulted with the same Ansible Vault password. In order for Jenkins to be able to run the Ansible openshift-applier it will need that Ansible Vault password. Therefore you need to put the Ansible Vault password in a Jenkins credential.

1. Create a Jenkins Credential:
   * scope: so long as your new pipeline can see it that is all that matters
   * name: matching that of the value of the `ansibleVaultJenkinsCredentialName` parameters you updated your Jenkinsfile (ex: `my-jenkins-credential-with-my-ansible-vault-password`)
   * value: the Ansible Vault password used to create all of the vaulted values in your `.applier/inventory` directory

#### 7.3 Add multi branch build for your application
1. create a Jenkins multi branch build that points at the git repository for your application
2. Optionally: filter the branches based on your git branching and promotion strategy defined in your copy of the `pipelineJEE8.groovy` file

### 8. Run the pipeline
1. Run the pipeline

### 9. Troubleshoot the pipeline
This inevitably wont work on the first try. Troubleshoot the errors as you see them, and if you are feeling generous, do PRs here to share those errors and their resolutions here.
