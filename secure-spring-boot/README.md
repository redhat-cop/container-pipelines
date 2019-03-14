# Secure Spring Boot Pipeline

The objective of this pipeline to provide a working example of using basic security and quality checks in a Spring Boot app running on OpenShift. There are many, many tools that can be used to this end. Currently this pipeline supports the plugins identified in [the CoP Spring Rest app](https://github.com/redhat-cop/spring-rest).

## Usage

This repo assumes knowledge of the OpenShift Applier and ansible in general. If you are new to applier/ansible, please see [the docs](https://github.com/redhat-cop/openshift-applier) which have really useful tutorials. 

1. `git clone https://github.com/redhat-cop/container-pipelines`
2. `cd container-pipelines/secure-spring-boot`
3. `ansible-galaxy install -r requirements.yml --roles-path=roles` 
4. `oc login`
5. `ansible-playbook ci-cd-tooling.yml -i roles/labs-ci-cd/inventory/`
6. `ansible-playbook spring-boot-app.yml -i .openshift-applier/inventory/`

## WIP 

- https://github.com/rht-labs/labs-ci-cd/issues/261 
- reuse app templates
