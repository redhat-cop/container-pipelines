# JBoss Middleware Inventory

The goal of this repository is to [openshift-applier](https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier) inventory for middleware, largely focused on Red Hat JBoss middleware.

## How it Works

There is ansible inventory which identifies all components to be deployed to an OpenShift cluster. The ansible layer is very thin. It simply provides a way to orchestrate the application of [OpenShift templates](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html) across one or more [OpenShift projects](https://docs.openshift.com/container-platform/3.6/architecture/core_concepts/projects_and_users.html#projects). All configuration for the applications should be defined by an OpenShift template and the corresponding parameters file. 

Currently, the following components are included in this inventory:

* Red Hat BRMS 6.x decision server

## Prerequisites

* The following OCP projects. This inventory defaults to, and is tested against, the projects in [labs-ci-cd](https://github.com/rht-labs/labs-ci-cd), but you can supply your own via `-e` vars on the command line:
    * a CI/CD project for building images and running pipelines
    * a dev project to automatically deploy the image
    * a demo project that require human approval to deploy to
* [Ansible](http://docs.ansible.com/ansible/latest/intro_installation.html)
* [OpenShift CLI Tools](https://docs.openshift.com/container-platform/3.6/cli_reference/get_started_cli.html)
* Access to the OpenShift cluster 

## Usage 

1. Log on to an OpenShift server `oc login -u <user> https://<server>:<port>/`
    1. Your user needs permissions to deploy ProjectRequest objects
2. Clone this repository
3. Install the required [casl-ansible](https://github.com/redhat-cop/casl-ansible) dependency
    1. `[middleware]$ ansible-galaxy install -r requirements.yml --roles-path=roles`
4. Run the ansible playbook provided by the casl-ansible
    1. `[middleware]$ ansible-playbook roles/casl-ansible/playbooks/openshift-cluster-seed.yml -i inventory/`

After running the playbook, the pipeline should execute in Jenkins


## Running a Subset of the Inventory 

See [the docs](https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier#filtering-content-based-on-tags) in casl-ansible

## Layout
- `inventory`: a standard [ansible inventory](http://docs.ansible.com/ansible/latest/intro_inventory.html). 
  - the `group_vars` are written according to [the convention defined by the openshift-applier role](https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier#sourcing-openshift-object-definitions).
  -  the `hosts` file reflects the fact that the playbook will use the OpenShift CLI on your localhost to interact with the cluster
- `openshift-templates`: a set [OpenShift templates](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html) to be sourced from the inventory. OpenShift provides a lot of templates out of the box, and [the Labs team curates a repository](https://github.com/rht-labs/labs-ci-cd/tree/master/templates) as well. These should be favored before writing custom/new templates to be kept here.
- `params`: a set of [parameter files](https://docs.openshift.com/container-platform/3.6/dev_guide/templates.html#templates-parameters) to be processed along with their respective OpenShift template. the convention here is to group files by their application.

## Common Issues

- S2I Build fails to push image to registry with `error: build error: Failed to push image: unauthorized: authentication required`
  - See [this issue](https://github.com/openshift/origin/issues/4518)
  
## Contributing

1) Fork the repo and open PR's
2) Add all new components to the inventory with appropriate namespaces and tags
3) Extended the `Jenkinsfile` with steps to verify that your components built/deployed correctly
4) For now, it is your responsibility to run the CI job. Please contact an admin for the details to set the CI job up.

## License
[ASL 2.0](LICENSE)
