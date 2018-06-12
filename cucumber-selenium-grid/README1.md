oc new-project selenium
oc new-app registry.access.redhat.com/rhoar-nodejs/nodejs-8~https://github.com/raffaelespazzoli/todomvc --context-dir examples/angularjs --name todomvc

oc new-build --strategy=docker --name=jenkins-slave-nodejs8
oc start-build -F jenkins-slave-protractor --from-dir=.