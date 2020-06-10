library identifier: "pipeline-library@1.6",
retriever: modernSCM(
  [
    $class: "GitSCMSource",
    remote: "https://github.com/redhat-cop/pipeline-library.git"
  ]
)


openshift.withCluster() {
  env.APP_NAME = "${JOB_NAME}".replaceAll(/-build.*/, '')
  env.ARTIFACT_DIRECTORY = "basic-nginx/build"
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
        git url: "${SOURCE_REPOSITORY_URL}", branch: "${SOURCE_REPOSITORY_REF}"
      }
    }

    stage('Build') {
      steps {
        script {
          sh """
              cd basic-nginx
              mkdir build
              cp index.html build/index.html
              cp nginx.conf build/nginx.conf
            """
        }
      }
    }

    stage('Image Build') {
      steps {
        binaryBuild(projectName: "${BUILD}", buildConfigName: "${APP_NAME}", buildFromPath: "${ARTIFACT_DIRECTORY}")
      }
    }

    stage('Promote from Build to Dev') {
      steps {
        tagImage(sourceImageName: env.APP_NAME, sourceImagePath: env.BUILD, toImagePath: env.DEV)
      }
    }

    stage('Verify Deployment to Dev') {
      steps {
        verifyDeployment(projectName: env.DEV, targetApp: env.APP_NAME)
      }
    }

    stage('Promotion gate (Stage)') {
      steps {
        script {
	  if(!("${SKIP_MANUAL_PROMOTION}").toBoolean()) {
            input message: 'Promote Application to Stage?'
          }
        }
      }
    }

    stage('Promote from Dev to Stage') {
      steps {
        tagImage(sourceImageName: env.APP_NAME, sourceImagePath: env.DEV, toImagePath: env.STAGE)
      }
    }

    stage('Verify Deployment to Stage') {
      steps {
        verifyDeployment(projectName: env.STAGE, targetApp: env.APP_NAME)
      }
    }

    stage('Promotion gate (Prod)') {
      steps {
        script {
          if(!("${SKIP_MANUAL_PROMOTION}").toBoolean()) {
            input message: 'Promote Application to Prod?'
          }
        }
      }
    }

    stage('Promote from Stage to Prod') {
      steps {
        tagImage(sourceImageName: env.APP_NAME, sourceImagePath: env.STAGE, toImagePath: env.PROD)
      }
    }

    stage('Verify Deployment to Prod') {
      steps {
        verifyDeployment(projectName: env.PROD, targetApp: env.APP_NAME)
      }
    }
  }
}
