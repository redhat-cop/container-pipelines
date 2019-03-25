# Basic Vue Pipeline

The objective of this pipeline to provide a working example of a basic vue js pipeline that can run on openshift.

## Usage

This repo assumes knowledge of the OpenShift Applier and ansible in general. If you are new to applier/ansible, please see [the docs](https://github.com/redhat-cop/openshift-applier) which have really useful tutorials. 

1. `git clone https://github.com/redhat-cop/container-pipelines`
2. `cd container-pipelines/basic-vue`
3. If you would like to customize the names of OpenShift projects created, edit `project-names.uml`
4. `ansible-galaxy install -r requirements.yml --roles-path=roles` 
5. `oc login`
6. `ansible-playbook ci-cd-tooling.yml -i roles/labs-ci-cd/inventory/`
7. `ansible-playbook vue-app.yml -i .openshift-applier/inventory/`
8. Navigate to the OpenShift Web console for your CI/CD project. You should see a pipeline build running automatically (once everything spins up).