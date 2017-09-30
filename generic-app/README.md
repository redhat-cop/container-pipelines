# Generic Application Pipeline

This Jenkins pipeline is designed to run under the following conditions:

- the Jenkins pipeline job is created via the OpenShift sync plugin, which in turn requires a `BuildConfig` with strategy `pipeline` 
- the application will be built & packaged in a Jenkins slave pod, not via S2I source
- the container image will be built via S2I binary, initiated from the slave pod that built the application


## Support Resources

The following resources are commonly used in support of this pipeline:

- an OpenShift template containing the S2I `BuildConfig` used to construct the container image, and the `BuildConfig` that models the Jenkins pipeline
  - [this version](https://github.com/rht-labs/openshift-templates/blob/master/s2i-app-build-with-secret-template.json) uses a `sourcesecret`
  - [this version](https://github.com/rht-labs/openshift-templates/blob/master/s2i-app-build-template.json) does not use a `sourcesecret`
- [an OpenShift template](https://github.com/rht-labs/labs-ci-cd/blob/master/templates/build-pod/template.json) for a custom Jenkins slave pod
- Docker builds that define [Maven](https://github.com/rht-labs/labs-ci-cd/tree/master/docker/mvn-build-pod) and [NPM](https://github.com/rht-labs/labs-ci-cd/tree/master/docker/npm-build-pod) Jenkins slave pods
- A generic application `DeploymentConfig` for apps exposes http endpoints
- The ansible automation in the [openshift-applier]((https://github.com/redhat-cop/casl-ansible/tree/master/roles/openshift-applier) role in `casl-ansible`.

## Getting Started

1. Copy the `Jenkinsfile` into the root directory of your application source code
2. Review the environment variables specified in the first `node`. This part of the script will run on a Jenkins master,
and set all the needed variables for the rest of the script
3. Commit and push your changes
4. Ensure you a pipeline `BuildConfig` in the OCP project where your Jenkins should live
5. Run the Jenkins job 