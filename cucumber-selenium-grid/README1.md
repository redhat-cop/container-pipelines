setup
```
oc process -f applier/projects/projects.yml | oc apply -f -
oc process openshift//jenkins-ephemeral | oc apply -f- -n todomvc-build
oc env dc/jenkins JENKINS_JAVA_OVERRIDES=-Dhudson.model.DirectoryBrowserSupport.CSP='' INSTALL_PLUGINS=ansicolor:0.5.2 -n todomvc-build
manually install the ansiccolor plugin
manually run this: System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "") or pass this: -Dhudson.model.DirectoryBrowserSupport.CSP= 
oc new-build --strategy docker --name jenkins-slave-nodejs8 --context-dir cucumber-selenium-grid/nodejs-slave https://github.com/raffaelespazzoli/container-pipelines#selenium -n todomvc-build 
oc process -f applier/templates/deployment.yml --param-file=applier/params/deployment-dev | oc apply -f-
oc process -f applier/templates/deployment.yml --param-file=applier/params/deployment-stage | oc apply -f-
oc process -f applier/templates/deployment.yml --param-file=applier/params/deployment-prod | oc apply -f-
oc process -f applier/templates/build.yml --param-file applier/params/build-dev | oc apply -f-
oc adm policy add-scc-to-user anyuid -z zalenium -n todomvc-stage
#oc adm policy add-cluster-role-to-user zalenium-role -z zalenium -n todomvc-stage
oc process -f applier/templates/selenium-grid.yaml NAMESPACE=todomvc-stage | oc apply -f -
``` 
delete
```
oc delete project todomvc-build
oc delete project todomvc-dev
oc delete project todomvc-stage
oc delete project todomvc-prod
```

nfs provisioner
```
oc new-project nfs-provisioner
oc create sa nfs-provisioner
oc apply -f https://raw.githubusercontent.com/raffaelespazzoli/openshift-enablement-exam/master/misc/nfs-dp/nfs-provisioner-scc.yaml
oc adm policy add-scc-to-user nfs-provisioner -z nfs-provisioner
oc apply -f https://raw.githubusercontent.com/kubernetes-incubator/external-storage/master/nfs/deploy/kubernetes/auth/openshift-clusterrole.yaml
oc adm policy add-cluster-role-to-user nfs-provisioner-runner -z nfs-provisioner
oc apply -f https://raw.githubusercontent.com/raffaelespazzoli/openshift-enablement-exam/master/misc/nfs-dp/nfs-provisioner-dc.yaml
oc apply -f https://raw.githubusercontent.com/raffaelespazzoli/openshift-enablement-exam/master/misc/nfs-dp/nfs-provisioner-class.yaml
```

ansible nodes -b -vv -i /tmp/git/openshift-enablement-exam/misc/casl/inventory -m shell -a "mkdir -p /tmp/nfs-provisioner && chmod -R 777 /tmp/nfs-provisioner && semanage fcontext -a -t container_file_t /tmp/nfs-provisioner && restorecon -R /tmp/nfs-provisioner" --private-key=~/.ssh/rspazzol-etl3.pem -e openstack_ssh_public_key=rspazzol

