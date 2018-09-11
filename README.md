# Container Pipelines

Let's get the ball rolling on some Container-driven CI &amp; CD

## Catalog

The following is a list of the pipeline samples available in this repository:

- [Basic Tomcat](./basic-tomcat) - Builds a Java Application like Ticket Monster and deploys it to Tomcat
- [Basic Spring Boot](./basic-spring-boot) - Builds a Spring Boot application and deploys using an Embedded Servlet jar file
- [Blue Green Spring Boot](./blue-green-spring) - Build a Spring Boot application and deploys it using a blue-green deployment
- [Cross Cluster Promotion Pipeline](./multi-cluster-spring-boot) - A [declarative syntax](https://jenkins.io/doc/book/pipeline/syntax/#declarative-pipeline) pipeline that demonstrates promoting a microservice between clusters (i.e. a Non-Production to a Production cluster)

## Makeup of a "Pipeline"

We understand that everyone's definition of a pipeline is a little (maybe a lot) different. Let's talk about what WE mean.

In this context, a _pipeline_ is defined as all of the technical collateral required to take application source code and get it deployed through it's relevant lifecycle environments on an OpenShift cluster (or multiple clusters).

A few guiding principles for a pipeline quickstart in this repo:
- **Everything as code**. A pipeline should require as few commands as possible to deploy (We recommend an [openshift-applier](https://github.com/redhat-cop/openshift-applier) compatible inventory)
- **Use OpenShift Features**. The intention of these quickstarts is to showcase how OpenShift can be used to make pipeline development and management simpler. Use of features like slave pods, webhooks, source to image and the `JenkinsPipelineStrategy` is highly desired where possible.
- **Sharing is Caring**. If there are things that can be common to multiple pipelines (templates, builder images, etc.), let's refactor to make them shared.

Typically the things required to build a pipeline sample include:
- Project definitions (each representing a lifecycle environment)
- A Jenkins Master
- A jenkinsfile
- A _build template_ that includes all things necessary to get the source code built into a container image. This means:
  - A JenkinsPipelineStrategy buildConfig, which is used to inject the pipeline into Jenkins automatically
  - A Source strategy binary buildConfig, which is used to build the container image
- A deployment template that includes all the necessary objects to run the application in an environment. At a minimum:
  - A DeploymentConfig definition
  - A Service definition
- it might also include:
  - Routes
  - Secrets
  - ConfigMaps
  - StatefulSets
  - etc.

See our [basic spring boot](./basic-spring-boot) example for a very simple reference architecture.

## Automated Deployments

These pipeline quickstarts include an Ansible inventory through which they can be automatically deployed and managed using the [OpenShift Applier](https://github.com/redhat-cop/openshift-applier) role.
