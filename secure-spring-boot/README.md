# Secure Spring Boot Pipeline

The objective of this pipeline to provide a working example of using basic security and quality checks in a Spring Boot app running on OpenShift. There are many, many tools that can be used to this end. Currently this pipeline supports the plugins identified in [the CoP Spring Rest app](https://github.com/redhat-cop/spring-rest).

## Usage

This repo assumes knowledge of the OpenShift Applier and ansible in general. If you are new to applier/ansible, please see [the docs](https://github.com/redhat-cop/openshift-applier) which have really useful tutorials. 

1. `git clone https://github.com/redhat-cop/container-pipelines`
2. `cd container-pipelines/secure-spring-boot`
3. If you would like to customize the names of OpenShift projects created, edit `project-names.uml`
4. `ansible-galaxy install -r requirements.yml --roles-path=roles` 
5. `oc login`
6. `ansible-playbook ci-cd-tooling.yml -i roles/labs-ci-cd/inventory/`
7. `ansible-playbook spring-boot-app.yml -i .openshift-applier/inventory/`
8. Navigate to the OpenShift Web console for your CI/CD project. You should see a pipeline build running automatically (once everything spins up).

## TODO Screen Shots of the pipeline to explain what's in the box
See https://github.com/redhat-cop/container-pipelines/issues/72

## Advisories

- Running the pipeline for the first time will take ~10 minutes because all maven dependencies and NIST DB need to be downloaded. Subsequent builds will be faster. Also see https://github.com/redhat-cop/container-pipelines/issues/71
- If you have issues with Nexus certificate like seen [here](https://github.com/redhat-cop/infra-ansible/issues/342), then you can set the ansible var `nexus_validate_certs: false` as a work around.