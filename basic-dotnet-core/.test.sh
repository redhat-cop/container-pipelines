#!/usr/bin/env bash

applier() {
  echo "applier - $(pwd)"
  ansible-galaxy install -r requirements.yml -p galaxy
  ansible-playbook -i ./.applier/ galaxy/openshift-applier/playbooks/openshift-cluster-seed.yml -e oc_token="$(oc whoami --show-token)"
}

test() {
  echo "TODO: test - $(pwd)"
  oc start-build basic-dotnet-core-pipeline -n basic-dotnet-core-build
}

cleanup() {
  echo "cleanup - $(pwd)"
  oc delete project/basic-dotnet-core-build project/basic-dotnet-core-dev project/basic-dotnet-core-stage project/basic-dotnet-core-prod --ignore-not-found
}

# Process arguments
case $1 in
  applier)
    applier
    ;;
  test)
    test
    ;;
  cleanup)
    cleanup
    ;;
  *)
    echo "Not an option"
    exit 1
esac