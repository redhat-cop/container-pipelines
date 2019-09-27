library identifier: "pipeline-library@v1.4",
retriever: modernSCM(
  [
    $class: "GitSCMSource",
    remote: "https://github.com/redhat-cop/pipeline-library.git"
  ]
)

def currentState = 'green'
def newState = 'blue'
def pom_file = ''
def version = ''

openshift.withCluster() {
  env.TARGET = env.BUILD_CONTEXT_DIR ? "${env.BUILD_CONTEXT_DIR}/target" : "target"
  env.APP_NAME = "${JOB_NAME}".replaceAll(/-build.*/, '')
  env.BUILD = openshift.project()
  env.DEV = "${APP_NAME}-dev"
  env.STAGE = "${APP_NAME}-stage"
  env.PROD = "${APP_NAME}-prod"
  echo "Starting Pipeline for ${APP_NAME}..."
}

pipeline {
  agent {
    label 'maven'
  }

  stages {
    stage('Git Checkout') {
      steps {
        git url: "${APPLICATION_SOURCE_REPO}", branch: "${APPLICATION_SOURCE_REF}"
      }
    }

    stage('pom information') {
      steps {
        script {
          pom_file = env.BUILD_CONTEXT_DIR ? "${env.BUILD_CONTEXT_DIR}/pom.xml" : "pom.xml"
          version = getVersionFromPom(pom_file)
          def artifactId = getArtifactIdFromPom(pom_file)
          def groupId = getGroupIdFromPom(pom_file)
          println("Artifact ID: ${artifactId}, Group ID: ${groupId}")
          println("New version tag: ${version}")
        }
      }
    }
    
    stage('Build') {
      steps {
        sh "mvn -B clean install -DskipTests=true -f ${pom_file}"
      }
    }

    stage('Unit Test') {
      steps {
        sh "mvn -B test -f ${pom_file}"
      }
    }

    stage('Build Container Image'){
      steps {
        sh """
          ls ${TARGET}/*
          rm -rf oc-build && mkdir -p oc-build/deployments
          for t in \$(echo "jar;war;ear" | tr ";" "\\n"); do
            cp -rfv ./${TARGET}/*.\$t oc-build/deployments/ 2> /dev/null || echo "No \$t files"
          done
        """
        binaryBuild(projectName: BUILD, buildConfigName: APP_NAME, buildFromPath: "oc-build")
      }
    }

    stage('Promote from Build to Dev') {
      steps {
        tagImage(sourceImageName: APP_NAME, sourceImagePath: BUILD, toImagePath: DEV)
      }
    }

    stage('Verify Deployment to Dev') {
      steps {
        verifyDeployment(projectName: DEV, targetApp: APP_NAME)
      }
    }

    stage('Integration Test') {
      steps {
        echo "TODO: Add application integration testing, verify db connectivity, rest calls ..."
      }
    }

    stage('Promote from Dev to Stage') {
      steps {
        tagImage(sourceImageName: APP_NAME, sourceImagePath: DEV, toImagePath: STAGE, toImageTag: version)
        script {
          println("New version tag:" + version)
          openshift.withCluster() {
            openshift.withProject(STAGE) {
              def dc = openshift.selector("dc/${APP_NAME}").object()
              def trigger_patch =  [
                ["type":"ImageChange",
                 "imageChangeParams":[
                   "automatic": true,
                   "containerNames": [APP_NAME],
                   "from":[
                     "kind":"ImageStreamTag",
                     "namespace":STAGE,
                     "name":"${APP_NAME}:${version}"
                   ]
                 ]
                ],
                ["type":"ConfigChange"]
              ]
              dc.spec.triggers = trigger_patch
              openshift.apply(dc)
            }
          }
        }
      }
    }

    stage('Verify Deployment to Stage') {
      steps {
        verifyDeployment(projectName: STAGE, targetApp: APP_NAME)
      }
    }

    stage('Promote from Stage to Prod') {
      steps {
        script {
          println("New version tag:" + version)
          openshift.withCluster() {
            openshift.withProject(PROD) {
              def activeService = openshift.selector("route/${APP_NAME}").object().spec.to.name
              if (activeService == "${APP_NAME}-blue") {
                newState = 'green'
                currentState = 'blue'
              }
              def dc = openshift.selector("dc/${APP_NAME}-${newState}").object()
              def trigger_patch =  [
                ["type":"ImageChange",
                 "imageChangeParams":[
                   "automatic": true,
                   "containerNames": ["${APP_NAME}-${newState}"],
                   "from":[
                     "kind":"ImageStreamTag",
                     "namespace":PROD,
                     "name":"${APP_NAME}-${newState}:${version}"
                   ]
                 ]
                ],
                ["type":"ConfigChange"]
              ]
              dc.spec.triggers = trigger_patch
              openshift.apply(dc)
            }
          }
        }
        tagImage(sourceImageName: APP_NAME, sourceImagePath: DEV, toImagePath: PROD, toImageName: "${APP_NAME}-${newState}", toImageTag: version)
      }
    }
    
    stage('Verify Deployment to Prod') {
      steps {
        verifyDeployment(projectName: PROD, targetApp: "${APP_NAME}-${newState}")
      }
    }
    
    stage('Switch route to new version') {
      steps {
        script {
          input "Switch ${PROD} from ${currentState} to ${newState} deployment?"
          openshift.withCluster() {
            openshift.withProject(PROD) {
              def route = openshift.selector("route/${APP_NAME}").object()
              route.spec.to.name = "${APP_NAME}-${newState}"
              openshift.apply(route)
            }
          }
        }
      }
    }
  }
}

// Convenience Functions to read variables from the pom.xml
// Do not change anything below this line.
def getVersionFromPom(pom) {
  def matcher = readFile(pom) =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}
def getGroupIdFromPom(pom) {
  def matcher = readFile(pom) =~ '<groupId>(.+)</groupId>'
  matcher ? matcher[0][1] : null
}
def getArtifactIdFromPom(pom) {
  def matcher = readFile(pom) =~ '<artifactId>(.+)</artifactId>'
  matcher ? matcher[0][1] : null
}
