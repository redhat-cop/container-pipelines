library identifier: "pipeline-library@master",
retriever: modernSCM(
  [
    $class: "GitSCMSource",
    remote: "https://github.com/dwasinge/pipeline-library.git"
  ]
)

openshift.withCluster() {

  env.NAMESPACE = openshift.project()
  env.POM_FILE = env.BUILD_CONTEXT_DIR ? "${env.BUILD_CONTEXT_DIR}/pom.xml" : "pom.xml"
  env.APP_NAME = "${env.JOB_NAME}".replaceAll(/-?${env.PROJECT_NAME}-?/, '').replaceAll(/-?pipeline-?/, '').replaceAll('/','')
  env.BUILD = "${env.NAMESPACE}"
  env.DEV = env.BUILD.replace('ci-cd', 'dev')
  env.TEST = env.BUILD.replace('ci-cd', 'test')

  echo "Starting Pipeline for ${APP_NAME}..."

}

pipeline {
  // Use Jenkins Maven slave
  // Jenkins will dynamically provision this as OpenShift Pod
  // All the stages and steps of this Pipeline will be executed on this Pod
  // After Pipeline completes the Pod is killed so every run will have clean
  // workspace
  agent {
    label 'jenkins-slave-mvn'
  }

  // Pipeline Stages start here
  // Requeres at least one stage
  stages {

    // Checkout source code
    // This is required as Pipeline code is originally checked out
    // so this will pull the application source code to this slave
    stage('Git Checkout Application') {
      steps {
        // Turn off Git's SSL cert check, uncomment if needed
        // sh 'git config --global http.sslVerify false'
        git url: "${APPLICATION_SOURCE_REPO}"
      }
    }

    //verify nexus is up

    stage('Wait for Nexus') {
      steps {
        verifyDeployment(targetApp: "nexus", projectName: env.BUILD)
      }
    }

    // Run Maven build, skipping tests
    stage('Build'){
      steps {
        sh "mvn -B clean install -DskipTests=true -f ${POM_FILE}"
      }
    }

    // Run Maven unit tests
    stage('Unit Test'){
      steps {
        sh "mvn -B test -f ${POM_FILE}"
      }
    }

    // stage ('Code Analysis') {
    //     steps {
    //       sonarqubeStaticAnalysis(
    //         buildServerWebHookName: "jenkins",
    //         buildServerWebHookUrl: "${JENKINS_URL}sonarqube-webhook/",
    //         dependencyCheckReportDir: "target",
    //         dependencyCheckReportFiles: "dependency-check-report.html",
    //         dependencyCheckReportName: "OWASP Dependency Check Report",
    //         dependencyCheckKeepAll: true,
    //         dependencyCheckAlwaysLinkToLastBuild: true,
    //         dependencyCheckAllowMissing: true,
    //         unitTestReportDir: "target/site/jacoco/",
    //         unitTestReportFiles: "index.html",
    //         unitTestReportName: "Jacoco Unit Test Report",
    //         unitTestKeepAll: true,
    //         unitTestAlwaysLinkToLastBuild: false,
    //         unitTestAllowMissing: true)
    //     }
    // }

    // Build Container Image using the artifacts produced in previous stages
    stage('Build Container Image'){
      steps {
        // Copy the resulting artifacts into common directory
        sh """
          ls target/*
          rm -rf oc-build && mkdir -p oc-build/deployments
          for t in \$(echo "jar;war;ear" | tr ";" "\\n"); do
            cp -rfv ./target/*.\$t oc-build/deployments/ 2> /dev/null || echo "No \$t files"
          done
        """

        // Build container image using local Openshift cluster
        // Giving all the artifacts to OpenShift Binary Build
        // This places your artifacts into right location inside your S2I image
        // if the S2I image supports it.
        binaryBuild(projectName: env.BUILD, buildConfigName: env.APP_NAME, artifactsDirectoryName: "oc-build")
      }
    }

    stage('Promote from Build to Dev') {
      steps {
        tagImage(sourceImageName: env.APP_NAME, sourceImagePath: env.BUILD, toImagePath: env.DEV)
      }
    }

    stage ('Verify Deployment to Dev') {
      steps {
        verifyDeployment(projectName: env.DEV, targetApp: env.APP_NAME)
      }
    }

/*
    stage('Scan Web Application') {
      agent {
        label 'zap'
      } 
      steps {

        // run zap scanner
        sh "/zap/zap-baseline.py -d -m 5 -x zaprpt.xml -t ${env.APP_DEV_HOST}"

        // publish report to jenkins
        publishHTML([
          allowMissing: false, 
          alwaysLinkToLastBuild: false, 
          keepAll: true, 
          reportDir: '/zap/wrk', 
          reportFiles: 'baseline.html', 
          reportName: 'ZAP Baseline Scan', 
          reportTitles: 'ZAP Baseline Scan'
        ])

        //no mvn, so stash it and unstash later in pipeline on a maven node instead of ZAP node... 
        //sh "mvn sonar:sonar -Dsonar.zaproxy.reportPath=/zap/wrk/zaprpt.xml"
        stash name: "zaproxyreport", includes: "/zap/wrk/zaprpt.xml"
      }
    }

    stage('Publish ZAP Report') {
      steps {
        unstash "zaproxyreport" 
        sh "mvn sonar:sonar -Dsonar.zaproxy.reportPath=/zap/wrk/zaprpt.xml"
      }
    }
*/

    stage('Promotion gate') {
      steps {
        script {
          input message: 'Promote application to Test?'
        }
      }
    }

    stage('Promote from Dev to Test') {
      steps {
        tagImage(sourceImageName: env.APP_NAME, sourceImagePath: env.DEV, toImagePath: env.TEST)
      }
    }

    stage ('Verify Deployment to Test') {
      steps {
        verifyDeployment(projectName: env.TEST, targetApp: env.APP_NAME)
      }
    }

  }
}
