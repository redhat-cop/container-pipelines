# A Sample OpenShift Pipeline for a Spring Boot Application Using Helm

This Jenkins pipeline provides an example of how to build a [basic Spring Boot application](https://github.com/redhat-cop/spring-rest) using Helm. The pipeline runs using the CoP's [jenkins-agent-helm](https://github.com/redhat-cop/containers-quickstarts/tree/c016fed925a0ca281a03c5e6f4c1a54e7878caa6/jenkins-agents/jenkins-agent-helm) agent and contains the following steps:

1. `Checkout`: Checks out the spring-rest application
1. `Get Version From POM`: Gets the version defined in the pom.xml file. This version is used to set your image's tag.
1. `S2I Build`: Performs an [s2i build](https://docs.openshift.com/container-platform/4.5/builds/understanding-image-builds.html#build-strategy-s2i_understanding-image-builds). This stage runs your unit tests, builds your jar file, and builds your container image. You could split this stage like the [basic-spring-boot](../basic-spring-boot) example if desired, but the jenkins-agent-helm agent doesn't have maven on it, so in this case, it's easier to perform an s2i build instead.
1. `Deploy`: Deploys your application to OpenShift.

This pipeline uses two different Helm charts called [spring-boot-build](./spring-boot-build) and [spring-boot](./spring-boot). The `spring-boot-build` chart is used in the `S2I Build` stage to create and update your BuildConfig and ImageStream. The `spring-boot` chart is used in the `Deploy` stage to create and update your application's Kubernetes resources. While you could combine both of these charts into one, splitting into separate charts for building and deploying provides a greater separation of concerns.

Before you configure this pipeline, you must first build a Jenkins agent containing Helm. Let's look at how you can build the [jenkins-agent-helm](https://github.com/redhat-cop/containers-quickstarts/tree/c016fed925a0ca281a03c5e6f4c1a54e7878caa6/jenkins-agents/jenkins-agent-helm) agent.

## Building the jenkins-agent-helm Agent
While the pipeline itself only uses the spring-boot and spring-boot-build Helm charts, a third Helm chart is provided under the `.helm/` folder called `jenkins-agent-helm` that you can use to easily build this Jenkins agent. First, install the jenkins-agent-helm Helm chart, which creates a BuildConfig and ImageStream:

```bash
helm install jenkins-agent-helm .helm/jenkins-agent-helm
```

Then, use the `oc start-build` command to start the build in OpenShift:

```bash
oc start-build jenkins-agent-helm --follow
```

This ImageStream created by Helm already has the `role: jenkins-slave` label, so Jenkins will see this and automatically use this image to configure a new Jenkins agent.

Next, let's configure the Jenkins pipeline.

## Configuring This Jenkins Pipeline

Because the `JenkinsPipeline` build strategy is deprecated in OpenShift 4.x, this doc will describe how you can configure this pipeline manually within the Jenkins console.

First, deploy a Jenkins instance to OpenShift. You can deploy Jenkins by running this command:

```bash
oc new-app jenkins-persistent
```

Once Jenkins is up, find and access your route URL:

```bash
oc get route jenkins -o jsonpath='{.spec.host}'
```

Then, follow these steps:

1. Click `New Item` on the lefthand side of the screen
1. Enter a name for this pipeline. The suggested name is `basic-helm-spring-boot`.
1. Select `Pipeline` and click `Ok`
1. Scroll down to the botton of the screen to the `Pipeline` section. Change the "Pipeline script" dropdown to `Pipeline script from SCM`.
1. For "SCM", select `Git` and enter the Repository URL `https://github.com/redhat-cop/container-pipelines.git`
1. For "Script Path", enter `basic-helm-spring-boot/Jenkinsfile`
1. Click `Save`

Once you configure your pipeline, you can click `Build Now` on the lefthand side of your screen to start the pipeline.