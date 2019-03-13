# Secure Spring Boot Pipeline

## How to deploy
1. Apply the bootstrap and tools targets from [rht-labs/labs-ci-cd](https://github.com/rht-labs/labs-ci-cd) to deploy sonarqube, jenkins, etc. onto your cluster.
    - Review the instruction on the [README](https://github.com/rht-labs/labs-ci-cd/blob/master/README.md)
    - Be sure to define the ci-cd, dev, and test namespaces in this [file](https://github.com/rht-labs/labs-ci-cd/blob/master/inventory/group_vars/all.yml)



2. Apply the openshift cluster content from this folder
    - Log in to the openshift cluster where the [rht-labs/labs-ci-cd](https://github.com/rht-labs/labs-ci-cd) tooling was deployed
    - Clone this repo
    - Change directories to the [secure-spring-boot](https://github.com/haithamshahin333/container-pipelines/tree/master/secure-spring-boot) folder
    - Enter the [.openshift-applier](https://github.com/haithamshahin333/container-pipelines/tree/master/secure-spring-boot/.openshift-applier) directory
    - Update the namespaces in [apply.yml](https://github.com/haithamshahin333/container-pipelines/blob/master/secure-spring-boot/.openshift-applier/apply.yml) to be the same as those in [all.yml](https://github.com/rht-labs/labs-ci-cd/blob/master/inventory/group_vars/all.yml) from labs-ci-cd
    - Execute `ansible-galaxy install -r requirements.yml --roles-path=roles` to install the required openshift applier dependency
    - Execute `ansible-playbook apply.yml -i inventory/` to deploy the pipeline to your cluster
    
