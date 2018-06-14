setup
```
oc process -f applier/projects/projects.yml | oc apply -f -
oc process openshift//jenkins-ephemeral | oc apply -f- -n todomvc-build
oc new-build --strategy docker --name jenkins-slave-nodejs8 --context-dir cucumber-selenium-grid/nodejs-slave https://github.com/raffaelespazzoli/container-pipelines#selenium -n todomvc-build 
oc process -f applier/templates/deployment.yml --param-file=applier/params/deployment-dev | oc apply -f-
oc process -f applier/templates/deployment.yml --param-file=applier/params/deployment-stage | oc apply -f-
oc process -f applier/templates/deployment.yml --param-file=applier/params/deployment-prod | oc apply -f-
oc process -f applier/templates/build.yml --param-file applier/params/build-dev | oc apply -f-
oc adm policy add-scc-to-user anyuid -z zalenium -n todomvc-stage
oc adm policy add-cluster-role-to-user zalenium-role -z zalenium -n todomvc-stage
oc process -f applier/templates/selenium-grid.yaml NAMESPACE=todomvc-stage | oc apply -f -
``` 
delete
```
oc delete project todomvc-build
oc delete project todomvc-dev
oc delete project todomvc-stage
oc delete project todomvc-prod
```