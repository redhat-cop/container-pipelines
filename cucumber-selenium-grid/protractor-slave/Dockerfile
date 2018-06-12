FROM registry.access.redhat.com/openshift3/jenkins-slave-nodejs-rhel7

MAINTAINER Raffaele Spazzoli <rspazzol@redhat.com>

LABEL com.redhat.component="jenkins-slave-protractor" \
      name="jenkins-slave-protractor" \
      architecture="x86_64" \
      io.k8s.display-name="Protractor Jenkins Slave" \
      io.k8s.description="Jenkis Slave to run protractor tests" \
      io.openshift.tags="openshift,jenkins,slave,protractor"
      
USER root

RUN ["/bin/bash", "-c", "npm install -g npm && npm cache clean -f && npm install -g n && n stable && cp /usr/local/n/versions/node/10.4.0/bin/node `which node` && npm install -g npm && npm update -g"]     

RUN ["/bin/bash", "-c", "npm install -g protractor"]

USER 1001