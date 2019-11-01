# A Sample Tekton Pipeline for a Python responder Application

## Prerequisites

* One OpenShift Container Platform Clusters
  * OpenShift 4.1+ is required
* Access to GitHub
* [Tekton Operator]() In order to use Tekton pipelines you need to install the Tekton operator. To do that go to ***Catalog***-> ***Operator Hub*** page of you admin console. Look for ***OpenShift Pipelines Operator*** and install it. The default settings are fine for this demo. You can also automate the installation of the Tekton operator, but this is outside the scope of this demo.
* [Tekton CLI](https://github.com/tektoncd/cli)
* *Highly recommended*: [tekton dashboard](https://github.com/tektoncd/dashboard)

## Automated Deployment

This quickstart can be deployed quickly using Ansible. Here are the steps.

1. Clone [this repo](https://github.com/redhat-cop/container-pipelines)
2. `cd container-pipelines/basic-python-pipenv-tekton`
3. Run `ansible-galaxy install -r requirements.yml --roles-path=galaxy`
4. Log into an OpenShift cluster, then run the following command.

```shell
ansible-playbook -i ./.applier/ galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml
```

## Running the pipelines

Once you have deployed the needed infrastructure either with applier or manually, you can run the pipeline by issuing the following command:

```shell
oc project python-example-ns
tkn -n python-example-ns pipeline start python-example-pipeline -r python-example-git=python-example-git -s tekton
```
