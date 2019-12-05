#!/usr/bin/env groovy

/* The branching and promotion stratigy defined in this pipeline
 * is based on the GitFlow paradigm merged with OpenShift promotion concepts.
 * 
 * Branches to Stages to OpenShift Project Mapping
 *   * master - primary release branch
 *     * SETUP - N/A
 *     * BUILD - ${applicationName}-ci-cd
 *     * DEV   - ${applicationName}-dev
 *     * TEST  - ${applicationName}-test
 *     * QA    - ${applicationName}-qa
 *     * PROD  - ${applicationName}-prod
 *   * release/* - pre merging to master release candidate testing branch
 *     * SETUP - N/A
 *     * BUILD - ${applicationName}-ci-cd
 *     * DEV   - ${applicationName}-dev
 *     * TEST  - ${applicationName}-test
 *     * QA    - ${applicationName}-qa
 *   * develop - branch for developer intigration before merging into a release/* or master branch
 *     * SETUP - N/A
 *     * BUILD - ${applicationName}-ci-cd
 *     * DEV   - ${applicationName}-dev
 *   * hotfix/* - branch for a hotfix that needs to be tested through QA without interupting primary development
 *     * SETUP - N/A
 *     * BUILD - ${applicationName}-ci-cd-hotfix-*
 *     * DEV   - ${applicationName}-dev-hotfix-*
 *     * TEST  - ${applicationName}-test-hotfix-*
 *     * QA    - ${applicationName}-qa-hotfix-*
 *   * feature/* - branch for a standard feature being tested by a developer
 *     * SETUP - N/A
 *     * BUILD - ${applicationName}-ci-cd-feature-*
 *     * DEV   - ${applicationName}-dev-feature-*
 *   * pipeline/* - branch for testing changes to the CI/CD (pipeline) process
 *     * SETUP - N/A
 *     * BUILD - ${applicationName}-ci-cd-pipeline-*
 *     * DEV   - ${applicationName}-dev-pipeline-*
 *     * TEST  - ${applicationName}-test-pipeline-*
 *     * QA    - ${applicationName}-qa-pipeline-*
 *     * PROD  - ${applicationName}-prod-pipeline-*
 */

/* Namespace ownership and creation stratigy:
 *
 * CICD_NAMSPACE  - created by jenkins service account
 * DEV_NAMESPACE  - created by service account associated with token in provided in secret named by DEV_CLUSTER_CREDENTIAL_SECRET_NAME
 * TEST_NAMESPACE - created by service account associated with token in provided in secret named by TEST_CLUSTER_CREDENTIAL_SECRET_NAME
 * QA_NAMESPACE   - created by service account associated with token in provided in secret named by QA_CLUSTER_CREDENTIAL_SECRET_NAME
 * PROD_NAMESPACE - created by service account associated with token in provided in secret named by PROD_CLUSTER_CREDENTIAL_SECRET_NAME
 *
 * Permissions:
 *   * all of the service accounts, the one used by jenkins, and the ones specfied in the secrets,
 *     need to have self-provisioniner permissions in their respective clusters
 */

/* 
 * @param applicationName      The name of the application. This will be used as part of the dynamically created
 *                             OpenShift Project names as well as the image group when pushing and pulling
 *                             images form image repository.
 * @param serviceName          The servcie name within the applciation. Used for creating the OpenShift BuildConfig, Service, etc
 *                             as well as the image name pushed to the image group within the image repository.
 * @param ownerGroupName       The OpenShift RBAC group that should own the OpenShift resources created by this pipeline.
 * @param imagePushRegistry    The container image repository to push images to.
 * @param imagePullRegistry    The container image repository to pull images from. Some external image repositories, such as Nexus,
 *                             won't let you push to a group repository, hence the need for the seperation of push and pull
 *                             repositories, if your repository supports pushing to a default repository within an group repository,
 *                             such as Artifactory, then imagePushRegistry and imagePullRegistry can have the same value.
 * @param ansibleVaultJenkinsCredentialName    Name of the Jenkins credential that contians the Ansible Vault password to
 *                                             access vaulted variables when running Ansible.
 * @param imagePushSecret      Name of the OpenShift Secret that contains the credentials for pushing to the imagePushRegistry.
 *                             Default: image-push-repo-credenetial
 * @param imagePullSecret      Name of the OpenShift Secret that contains the credentials for pulling from the imagePullRegistry.
 *                             Default: image-pull-repo-credential
 * @param builderImage         S2I Image that supports binary builds to use to build the application service image.
 *                             Default: jboss-eap-7/eap72-openjdk11-openshift-rhel8:1.0
 * @param mavenMirrorUrl       Optional Maven Mirror URL to use when pulling Maven dependencies.
 * @param mvnAdditionalArgs    Additional arguments to pass to Maven build.
 *                             Default: -Dcom.redhat.xpaas.repo.jbossorg
 * @param sonarQubeEnv         Sonar Qube Environment to use when running Sonar Qube Tests
 */
def call(
    applicationName,
    serviceName,
    ownerGroupName,
    imagePushRegistry,
    imagePullRegistry,
    ansibleVaultJenkinsCredentialName = ''
    imagePushSecret   = 'image-push-repo-credential',
    imagePullSecret   = 'image-pull-repo-credential',
    builderImage      = 'jboss-eap-7/eap72-openjdk11-openshift-rhel8:1.0',
    mavenMirrorUrl    = '',
    mvnAdditionalArgs = '-Dcom.redhat.xpaas.repo.jbossorg',
    sonarQubeEnv      = ''
) {
    // if on a feature/*, hotfix/*, or pipeline/* branch then
    // use dedicated namespaces so as not to interupt primary
    // develop,release/*, and master branch work flows.
    String DEDICATED_NAMESPACES_BRANCHES_REGEX = "feature|hotfix|pipeline"

    // branches to build periodiacly
    String TRIGGER_CRON_PERIOD = env.BRANCH_NAME == "develop" ? "@midnight" : ""

    // Jenkins workers images
    String JENKINS_WORKER_IMAGE_MAVEN           = 'openshift3/jenkins-agent-maven-35-rhel7:latest'
    String JENKINS_WORKER_IMAGE_ANSIBLE         = 'quay.io/redhat-cop/jenkins-worker-ansible:v1.11'
    String JENKINS_WORKER_IMAGE_IMAGE_MANAGMENT = 'quay.io/redhat-cop/jenkins-slave-image-mgmt:v1.11'

    pipeline {
        triggers {
            cron(TRIGGER_CRON_PERIOD)
        }
        environment {
            DEFAULT_CICD_NAMESPACE = "${applicationName}-ci-cd"
            DEFAULT_DEV_NAMESPACE  = "${applicationName}-dev"
            DEFAULT_TEST_NAMESPACE = "${applicationName}-test"
            DEFAULT_QA_NAMESPACE   = "${applicationName}-qa"
            DEFAULT_PROD_NAMESPACE = "${applicationName}-prod"

            DEV_IMAGE_TAG  = 'dev'
            TEST_IMAGE_TAG = 'test'
            QA_IMAGE_TAG   = 'qa'
            PROD_IMAGE_TAG = 'prod'

            DEV_CLUSTER_CREDENTIAL_SECRET_NAME    = 'cluster-credential-dev'
            TEST_CLUSTER_CREDENTIAL_SECRET_NAME   = 'cluster-credential-test'
            QA_CLUSTER_CREDENTIAL_SECRET_NAME     = 'cluster-credential-qa'
            PROD_CLUSTER_CREDENTIAL_SECRET_NAME   = 'cluster-credential-prod'
        }
        // The options directive is for configuration that applies to the whole job.
        options {
            buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '1'))
            timeout(time: 15, unit: 'MINUTES')
            ansiColor('xterm')
            timestamps()
        }
        agent {
            kubernetes {
                label "jenkins-${applicationName}-${serviceName}-${env.BUILD_ID}"
                cloud 'openshift'
                yaml """
apiVersion: v1
kind: Pod
spec:
  serviceAccount: jenkins
  containers:
  - name: 'jnlp'
    image: "${JENKINS_WORKER_IMAGE_MAVEN}"
    tty: true
    volumeMounts:
    - name: maven-settings
      mountPath: /home/jenkins/.m2/settings.xml
      subPath: settings.xml
      readOnly: true
    - name: m2
      mountPath: /home/jenkins/.m2
  - name: 'jenkins-worker-ansible'
    image: "${JENKINS_WORKER_IMAGE_ANSIBLE}"
    tty: true
    command: ['sh', '-c', 'generate_container_user && cat']
  - name: 'jenkins-worker-image-mgmt'
    image: "${JENKINS_WORKER_IMAGE_IMAGE_MANAGMENT}"
    tty: true
    command: ['sh', '-c', 'generate_container_user && cat']
    volumeMounts:
    - mountPath: /var/run/secrets/kubernetes.io/dockerconfigjson
      name: dockerconfigjson
      readOnly: true
  volumes:
  - name: dockerconfigjson 
    secret:
      secretName: "${imagePushSecret}"
  - name: maven-settings
    configMap:
      name: maven
  - name: m2
    emptyDir: {}
"""
            }
        }
        stages {
            stage('SETUP') {
                when {
                    anyOf {
                        branch 'master';
                        branch 'release/*';
                        branch 'develop';
                        branch 'hotfix/*';
                        branch 'feature/*';
                        branch 'pipeline/*';
                    }
                }
                steps {
                    // determine OpenShift/Kubernetes Project/Namespace names
                    script {
                        // for certain branches use dedicated namespaces so as not to interupt primary
                        // develop,release/*, and master branch work flows.
                        if ( env.BRANCH_NAME =~ /^(${DEDICATED_NAMESPACES_BRANCHES_REGEX})\/.*/) {
                            namespace_postfix  = env.BRANCH_NAME.replaceAll("[^a-zA-Z1-9]","-")
                            env.CICD_NAMESPACE = "${env.DEFAULT_CICD_NAMESPACE}-${namespace_postfix}"
                            env.DEV_NAMESPACE  = "${env.DEFAULT_DEV_NAMESPACE}-${namespace_postfix}"
                            env.TEST_NAMESPACE = "${env.DEFAULT_TEST_NAMESPACE}-${namespace_postfix}"
                            env.QA_NAMESPACE   = "${env.DEFAULT_QA_NAMESPACE}-${namespace_postfix}"
                            env.PROD_NAMESPACE = "${env.DEFAULT_PROD_NAMESPACE}-${namespace_postfix}"
                        }

                        // ensure required environment variables are set
                        if (!env.CICD_NAMESPACE) env.CICD_NAMESPACE = env.DEFAULT_CICD_NAMESPACE
                        if (!env.DEV_NAMESPACE)  env.DEV_NAMESPACE  = env.DEFAULT_DEV_NAMESPACE
                        if (!env.TEST_NAMESPACE) env.TEST_NAMESPACE = env.DEFAULT_TEST_NAMESPACE
                        if (!env.QA_NAMESPACE)   env.QA_NAMESPACE   = env.DEFAULT_QA_NAMESPACE
                        if (!env.PROD_NAMESPACE) env.PROD_NAMESPACE = env.DEFAULT_PROD_NAMESPACE
                    }

                    // log some helpful information
                    script {
                        sh 'printenv'

                        echo "applicationName:   ${applicationName}"
                        echo "serviceName:       ${serviceName}"
                        echo "ownerGroupName:    ${ownerGroupName}"
                        echo "imagePushRegistry: ${imagePushRegistry}"
                        echo "imagePullRegistry: ${imagePullRegistry}"
                        echo "imagePushSecret:   ${imagePushSecret}"
                        echo "imagePullSecret:   ${imagePullSecret}"
                        echo "ansibleVaultJenkinsCredentialName: ${ansibleVaultJenkinsCredentialName}"
                        echo "builderImage:      ${builderImage}"
                        echo "mavenMirrorUrl:    ${mavenMirrorUrl}"
                        echo "mvnAdditionalArgs: ${mvnAdditionalArgs}"

                        echo "BRANCH_NAME:       ${env.BRANCH_NAME}"

                        echo "CICD_NAMESPACE:    ${env.CICD_NAMESPACE}"
                        echo "DEV_NAMESPACE:     ${env.DEV_NAMESPACE}"
                        echo "TEST_NAMESPACE:    ${env.TEST_NAMESPACE}"
                        echo "QA_NAMESPACE:      ${env.QA_NAMESPACE}"
                        echo "PROD_NAMESPACE:    ${env.PROD_NAMESPACE}"

                        echo "DEV_IMAGE_TAG:     ${env.DEV_IMAGE_TAG}"
                        echo "TEST_IMAGE_TAG:    ${env.TEST_IMAGE_TAG}"
                        echo "QA_IMAGE_TAG:      ${env.QA_IMAGE_TAG}"
                        echo "PROD_IMAGE_TAG:    ${env.PROD_IMAGE_TAG}"
                    }

                    script {
                        // get the version
                        def pom             = readMavenPom file: 'pom.xml'
                        env.MVN_VERSION     = pom.version
                        env.MVN_ARTIFACT_ID = pom.artifactId
                        env.APP_VERSION     = pom.version
                        // if branch is master don't include branch name in version
                        // else include branch name in version
                        //
                        // NOTE: by strict semantic versioning rules the build information should
                        //       be appended to version using a + but + is not valid in docker tags
                        if (env.BRANCH_NAME == 'master') {
                            env.BUILD_VERSION = "v${env.APP_VERSION}.${env.BUILD_ID}"
                        } else {
                            safeBranchName = env.BRANCH_NAME.replaceAll('/','_')
                            env.BUILD_VERSION = "v${env.APP_VERSION}-${safeBranchName}.${env.BUILD_ID}"
                        }
                        echo "App Version:   ${env.APP_VERSION}"
                        echo "Build Version: ${env.BUILD_VERSION}"
                    }
                }
            }
            stage('BUILD') {
                when {
                    anyOf {
                        branch 'master';
                        branch 'release/*';
                        branch 'develop';
                        branch 'hotfix/*';
                        branch 'feature/*';
                        branch 'pipeline/*';
                    }
                }
                stages {
                    stage ("BUILD: Create OpenShift artifacts") {
                        steps {
                            container('jenkins-worker-ansible') {
                                withCredentials([file(credentialsId: "${ansibleVaultJenkinsCredentialName}", variable: 'ANSIBLE_VAULT_PASSWORD_FILE')]) {
                                    // NOTE: openshift_templates_raw MUST be accessible to OpenShift without authentication
                                    // NOTE: this will use the serivce account that Jenkins is running as to create these resoruces to "solve"
                                    //       the chicken and egg problem of creating the CICD namespace and the cluster secrets in it for the other environments.
                                    applier(
                                        applierPlaybook: "apply.yml",
                                        playbookAdditionalArgs: """ \
                                        --vault-password-file=${ANSIBLE_VAULT_PASSWORD_FILE} \
                                        -e app_env=app-build \
                                        -e app_name=${serviceName} \
                                        -e ci_cd_namespace=${env.CICD_NAMESPACE} \
                                        -e app_build_push_secret=${imagePushSecret} \
                                        -e app_build_pull_secret=${imagePullSecret} \
                                        -e app_build_destination_image_name=${serviceName} \
                                        -e app_owner_group_name=${ownerGroupName} \
                                        -e app_build_destination_repo_name=${imagePushRegistry} \
                                        -e app_build_destination_image_tag=latest \
                                        -e app_build_destination_repo_namespace=${applicationName} \
                                        -e app_build_maven_mirror_url=${mavenMirrorUrl} \
                                        -e app_build_builder_image_name=${builderImage} \
                                        -e app_build_maven_args_append=${mvnAdditionalArgs}
                                        """,
                                        inventoryPath:    "inventory/hosts",
                                        requirementsPath: "requirements.yml",
                                        ansibleRootDir:   ".openshift-applier"
                                    )
                                }
                            }
                        }
      		    }
                    stage('BUILD: build') {
                        steps {
                            // build the applciation
                            sh "mvn clean package -U -B ${mvnAdditionalArgs}"

                            // copy other binary artifacts expected by S2I build to target directory
                            sh 'cp -R .s2i target/'
                            sh 'cp -R configuration target/'
                            sh 'cp -R extensions target/'
                        }
                    }
                    stage('BUILD: SonarQube') {
                        steps {                            
                            echo '### Running sonar scanner ###'
                            script {
                                withSonarQubeEnv(sonarQubeEnv) {
                                    sh 'mvn sonar:sonar -U -B -Dsurefire.useFile=false'
                                }
                            }
                        }
                    }
                    stage('BUILD: Build Image') {
                        steps {
                            buildAndTag(
                                imageName        : serviceName,
                                imageNamespace   : applicationName,
                                imageVersion     : env.BUILD_VERSION,
                                registryFQDN     : imagePushRegistry,
                                buildProjectName : env.CICD_NAMESPACE,
                                fromFilePath     : "target/"
                            )
                        }
                    }
                }
                post {
                    success {
                       //TODO: send success notification to ChatOps / Email / Other notifciation system
                    }
                    failure {
                       //TODO: send failure notification to ChatOps / Email / Other notifciation system
                    }
                }
            }
            stage('DEV') {
                when {
                    anyOf {
                        branch 'master';
                        branch 'release/*';
                        branch 'develop';
                        branch 'hotfix/*';
                        branch 'feature/*';
                        branch 'pipeline/*';
                    }
                }
                stages {
                    stage('DEV: Approval') {
                        steps {
                            echo 'TODO: verify build results'
                            echo 'Automatic approval given build results.'
                        }
                    }
                    stage('DEV: Promote') {
                        options {
                            lock("env-dev-${applicationName}-${serviceName}")
                        }
                        stages {
                            stage ("DEV: Get OpenShift cluster credentials") {
                                steps {
				    container('jenkins-worker-image-mgmt') {
					script {
					    def (devAPI, devToken) = clusterCredentials(
						projectName: env.CICD_NAMESPACE,
						secretName : env.DEV_CLUSTER_CREDENTIAL_SECRET_NAME
					    )
					    env.DEV_API   = devAPI
					    env.DEV_TOKEN = devToken
					}
				    }
                                }
                            }
                            stage ("DEV: Create OpenShift artifacts") {
                                steps {
                                    container('jenkins-worker-ansible') {
                                        withCredentials([file(credentialsId: "${ansibleVaultJenkinsCredentialName}", variable: 'ANSIBLE_VAULT_PASSWORD_FILE')]) {
                                            // NOTE: openshift_templates_raw MUST be accessible to OpenShift without authentication
                                            applier(
                                                applierPlaybook: "apply.yml",
                                                playbookAdditionalArgs: """ \
                                                    --vault-password-file=${ANSIBLE_VAULT_PASSWORD_FILE} \
                                                    -e app_env=app-dev \
                                                    -e namespace=${env.DEV_NAMESPACE} \
                                                    -e ci_cd_namespace=${env.CICD_NAMESPACE} \
                                                    -e app_owner_group_name=${ownerGroupName} \
                                                    -e app_name=${serviceName} \
                                                    -e app_image_tag=${env.DEV_IMAGE_TAG} \
                                                    -e app_image_namespace=${applicationName} \
                                                    -e repo_name=${imagePullRegistry} \
                                                    -e app_custom_env='{}' \
                                                    -e image_pull_secret=${imagePullSecret}
                                                """,
                                                inventoryPath:    "inventory/hosts",
                                                requirementsPath: "requirements.yml",
                                                ansibleRootDir:   ".openshift-applier",
                                                clusterAPI:       env.DEV_API,
                                                clusterToken:     env.DEV_TOKEN
                                            )
                                        }
                                    }
                                }   
      		            }
                            stage('DEV: Deploy') {
                                steps {
                                    tagAndDeploy(
                                        imageName                    : serviceName,
                                        imageNamespace               : applicationName,
                                        imageVersion                 : env.BUILD_VERSION,
                                        registryFQDN                 : imagePushRegistry,
                                        clusterAPI                   : env.DEV_API,
                                        clusterToken                 : env.DEV_TOKEN,
                                        deployDestinationProjectName : env.DEV_NAMESPACE,
                                        deployDestinationVersionTag  : DEV_IMAGE_TAG
                                    )
                                }
                            }
                        }
                    }
                }
                post {
                    success {
                       //TODO: send success notification to ChatOps / Email / Other notifciation system
                    }
                    failure {
                       //TODO: send failure notification to ChatOps / Email / Other notifciation system
                    }
                }
            }
            stage('TEST') {
                when {
                    anyOf {
                        branch 'master';
                        branch 'release/*';
                        branch 'hotfix/*';
                        branch 'pipeline/*';
                    }
                }
                stages {
                    stage('TEST: Approval') {
                        steps {
                            //TODO: send wiating for approval notification to ChatOps / Email / Other notification system
                            input 'Promote to TEST environment?'
                        }
                    }
                    stage('TEST: Promote') {
                        options {
                            lock("env-test-${applicationName}-${serviceName}")
                        }
                        stages {
                            stage ("TEST: Get OpenShift cluster credentials") {
                                steps {
                                    container('jenkins-worker-image-mgmt') {
                                        script {
                                            def (testAPI, testToken) = clusterCredentials(
                                                projectName: env.CICD_NAMESPACE,
                                                secretName : env.TEST_CLUSTER_CREDENTIAL_SECRET_NAME
                                            )
                                            env.TEST_API   = testAPI
                                            env.TEST_TOKEN = testToken
                                        }
                                    }
                                }
                            }
                            stage ("TEST: Create OpenShift artifacts") { 
                                steps {
                                    container('jenkins-worker-ansible') {
                                        withCredentials([file(credentialsId: "${ansibleVaultJenkinsCredentialName}", variable: 'ANSIBLE_VAULT_PASSWORD_FILE')]) {
                                            // NOTE: openshift_templates_raw MUST be accessible to OpenShift without authentication
                                            applier(
                                                applierPlaybook: "apply.yml",
                                                playbookAdditionalArgs: """ \
                                                    --vault-password-file=${ANSIBLE_VAULT_PASSWORD_FILE} \
                                                    -e app_env=app-test \
                                                    -e namespace=${env.TEST_NAMESPACE} \
                                                    -e ci_cd_namespace=${env.CICD_NAMESPACE} \
                                                    -e app_owner_group_name=${ownerGroupName} \
                                                    -e app_name=${serviceName} \
                                                    -e app_image_tag=${env.TEST_IMAGE_TAG} \
                                                    -e app_image_namespace=${applicationName} \
                                                    -e repo_name=${imagePullRegistry} \
                                                    -e app_custom_env='{}' \
                                                    -e image_pull_secret=${imagePullSecret}
                                                """,
                                                inventoryPath:    "inventory/hosts",
                                                requirementsPath: "requirements.yml",
                                                ansibleRootDir:   ".openshift-applier",
                                                clusterAPI:       env.TEST_API,
                                                clusterToken:     env.TEST_TOKEN
                                            )
                                        }
                                    }
                                }   
      		            }
                            stage('TEST: Deploy') {
                                steps {
                                    tagAndDeploy(
                                        imageName                    : serviceName,
                                        imageNamespace               : applicationName,
                                        imageVersion                 : env.BUILD_VERSION,
                                        registryFQDN                 : imagePushRegistry,
                                        clusterAPI                   : env.TEST_API,
                                        clusterToken                 : env.TEST_TOKEN,
                                        deployDestinationProjectName : env.TEST_NAMESPACE,
                                        deployDestinationVersionTag  : env.TEST_IMAGE_TAG
                                    )
                                }
                            }
                            stage('TEST: Run Tests') {
                                steps {
                                    echo "TODO"
                                }
                            }
                        }
                    }
                }
                post {
                    success {
                       //TODO: send success notification to ChatOps / Email / Other notifciation system
                    }
                    failure {
                       //TODO: send failure notification to ChatOps / Email / Other notifciation system
                    }
                }
            }
            stage('QA') {
                when {
                    anyOf {
                        branch 'master';
                        branch 'release/*';
                        branch 'hotfix/*';
                        branch 'pipeline/*';
                    }
                }
                stages {
                    stage('QA: Approval') {
                        steps {
                            //TODO: send wiating for approval notification to ChatOps / Email / Other notification system
                            input 'Promote to QA environment?'
                        }
                    }
                    stage('QA: Promote') {
                        options {
                            lock("env-qa-${applicationName}-${serviceName}")
                        }
                        stages {
                            stage ("QA: Get OpenShift cluster credentials") {
                                steps {
                                    container('jenkins-worker-image-mgmt') {
                                        script {
                                            def (qaAPI, qaToken) = clusterCredentials(
                                                projectName: env.CICD_NAMESPACE,
                                                secretName : env.QA_CLUSTER_CREDENTIAL_SECRET_NAME
                                            )
                                            env.QA_API   = qaAPI
                                            env.QA_TOKEN = qaToken
                                        }
                                    }
                                }
                            }
                            stage ("QA: Create OpenShift artifacts") { 
                                steps {
                                    container('jenkins-worker-ansible') {
                                        withCredentials([file(credentialsId: "${ansibleVaultJenkinsCredentialName}", variable: 'ANSIBLE_VAULT_PASSWORD_FILE')]) {
                                            // NOTE: openshift_templates_raw MUST be accessible to OpenShift without authentication
                                            applier(
                                                applierPlaybook: "apply.yml",
                                                playbookAdditionalArgs: """ \
                                                    --vault-password-file=${ANSIBLE_VAULT_PASSWORD_FILE} \
                                                    -e app_env=app-qa \
                                                    -e namespace=${env.QA_NAMESPACE} \
                                                    -e ci_cd_namespace=${env.CICD_NAMESPACE} \
                                                    -e app_owner_group_name=${ownerGroupName} \
                                                    -e app_name=${serviceName} \
                                                    -e app_image_tag=${env.QA_IMAGE_TAG} \
                                                    -e app_image_namespace=${applicationName} \
                                                    -e repo_name=${imagePullRegistry} \
                                                    -e app_custom_env='{}' \
                                                    -e image_pull_secret=${imagePullSecret}
                                                """,
                                                inventoryPath:    "inventory/hosts",
                                                requirementsPath: "requirements.yml",
                                                ansibleRootDir:   ".openshift-applier",
                                                clusterAPI:       env.QA_API,
                                                clusterToken:     env.QA_TOKEN
                                            )
                                        }
                                    }
                                }   
      		            }
                            stage('QA: Deploy') {
                                steps {
                                    tagAndDeploy(
                                        imageName                    : serviceName,
                                        imageNamespace               : applicationName,
                                        imageVersion                 : env.BUILD_VERSION,
                                        registryFQDN                 : imagePushRegistry,
                                        clusterAPI                   : env.QA_API,
                                        clusterToken                 : env.QA_TOKEN,
                                        deployDestinationProjectName : env.QA_NAMESPACE,
                                        deployDestinationVersionTag  : env.QA_IMAGE_TAG
                                    )
                                }
                            }
                            stage('QA: Run Tests') {
                                steps {
                                    echo "TODO"
                                }
                            }
                        }
                    }
                }
                post {
                    success {
                       //TODO: send success notification to ChatOps / Email / Other notifciation system
                    }
                    failure {
                       //TODO: send failure notification to ChatOps / Email / Other notifciation system
                    }
                }
            }
            stage('PROD') {
                when {
                    anyOf {
                        branch 'master';
                        branch 'pipeline/*';
                    }
                }
                stages {
                    stage('PROD: Approval') {
                        steps {
                            //TODO: send wiating for approval notification to ChatOps / Email / Other notification system
                            input 'Promote to PROD environment?'
                        }
                    }
                    stage('PROD: Promote') {
                        options {
                            lock("env-prod-${applicationName}-${serviceName}")
                        }
                        stages {
                            stage ("PROD: Get OpenShift cluster credentials") {
                                steps {
                                    container('jenkins-worker-image-mgmt') {
                                        script {
                                            def (prodAPI, prodToken) = clusterCredentials(
                                                projectName: env.CICD_NAMESPACE,
                                                secretName : env.PROD_CLUSTER_CREDENTIAL_SECRET_NAME
                                            )
                                            env.PROD_API   = prodAPI
                                            env.PROD_TOKEN = prodToken
                                        }
                                    }
                                }
                            }
                            stage ("PROD: Create OpenShift artifacts") { 
                                steps {
                                    container('jenkins-worker-ansible') {
                                        withCredentials([file(credentialsId: "${ansibleVaultJenkinsCredentialName}", variable: 'ANSIBLE_VAULT_PASSWORD_FILE')]) {
                                            // NOTE: openshift_templates_raw MUST be accessible to OpenShift without authentication
                                            applier(
                                                applierPlaybook: "apply.yml",
                                                playbookAdditionalArgs: """ \
                                                    --vault-password-file=${ANSIBLE_VAULT_PASSWORD_FILE} \
                                                    -e app_env=app-prod \
                                                    -e namespace=${env.PROD_NAMESPACE} \
                                                    -e ci_cd_namespace=${env.CICD_NAMESPACE} \
                                                    -e app_owner_group_name=${ownerGroupName} \
                                                    -e app_name=${serviceName} \
                                                    -e app_image_tag=${env.PROD_IMAGE_TAG} \
                                                    -e app_image_namespace=${applicationName} \
                                                    -e repo_name=${imagePullRegistry} \
                                                    -e app_custom_env='{}' \
                                                    -e image_pull_secret=${imagePullSecret}
                                                """,
                                                inventoryPath:    "inventory/hosts",
                                                requirementsPath: "requirements.yml",
                                                ansibleRootDir:   ".openshift-applier",
                                                clusterAPI:       env.PROD_API,
                                                clusterToken:     env.PROD_TOKEN
                                            )
                                        }
                                    }
                                }   
      	                    }
                            stage('PROD: Deploy') {
                                steps {
                                    tagAndDeploy(
                                        imageName                    : serviceName,
                                        imageNamespace               : applicationName,
                                        imageVersion                 : env.BUILD_VERSION,
                                        registryFQDN                 : imagePushRegistry,
                                        clusterAPI                   : env.PROD_API,
                                        clusterToken                 : env.PROD_TOKEN,
                                        deployDestinationProjectName : env.PROD_NAMESPACE,
                                        deployDestinationVersionTag  : env.PROD_IMAGE_TAG
                                    )
                                }
                            }
                        }
                    }
                }
                post {
                    success {
                       //TODO: send success notification to ChatOps / Email / Other notifciation system
                    }
                    failure {
                       //TODO: send failure notification to ChatOps / Email / Other notifciation system
                    }
                }
            }
        }
        post {
            post {
                success {
                    //TODO: send success notification to ChatOps / Email / Other notifciation system
                }
                failure {
                    //TODO: send failure notification to ChatOps / Email / Other notifciation system
                }
            }
            failure {
              mattermostSend "${serviceName}: Pipeline FAILED: ${env.BUILD_VERSION}"  
            }
            always {
                archiveArtifacts "**"
            }
        }
    }
}
