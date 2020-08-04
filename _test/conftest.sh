#!/usr/bin/env bats

load bats-support-clone
load test_helper/bats-support/load
load test_helper/redhatcop-bats-library/load

setup_file() {
  rm -rf /tmp/rhcop
  conftest_pull
}

@test "basic-dotnet-core/.openshift" {
  tmp=$(split_files "basic-dotnet-core/.openshift")

  namespaces=$(get_rego_namespaces "ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "basic-nginx/.openshift" {
  tmp=$(split_files "basic-nginx/.openshift")

  namespaces=$(get_rego_namespaces "(?!ocp\.deprecated.ocp4_3\.buildconfig_jenkinspipeline_strategy)ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "basic-spring-boot/.openshift" {
  tmp=$(split_files "basic-spring-boot/.openshift")

  namespaces=$(get_rego_namespaces "ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "basic-spring-boot-tekton/.openshift" {
  tmp=$(split_files "basic-spring-boot-tekton/.openshift")

  namespaces=$(get_rego_namespaces "ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "basic-tomcat/.openshift" {
  tmp=$(split_files "basic-tomcat/.openshift")

  namespaces=$(get_rego_namespaces "(?!ocp\.deprecated.ocp4_3\.buildconfig_jenkinspipeline_strategy)ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "blue-green-spring/.openshift" {
  tmp=$(split_files "blue-green-spring/.openshift")

  namespaces=$(get_rego_namespaces "(?!ocp\.deprecated.ocp4_3\.buildconfig_jenkinspipeline_strategy)ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "cucumber-selenium-grid/applier" {
  tmp=$(split_files "cucumber-selenium-grid/applier")

  namespaces=$(get_rego_namespaces "(?!ocp\.deprecated.ocp4_3\.buildconfig_jenkinspipeline_strategy)ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "jenkins-s2i" {
  tmp=$(split_files "jenkins-s2i")

  namespaces=$(get_rego_namespaces "ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "multi-cluster-multi-branch-jee/.openshift" {
  tmp=$(split_files "multi-cluster-multi-branch-jee/.openshift")

  namespaces=$(get_rego_namespaces "ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "multi-cluster-spring-boot/image-mirror-example/.applier" {
  tmp=$(split_files "multi-cluster-spring-boot/image-mirror-example/.applier")

  namespaces=$(get_rego_namespaces "(?!ocp\.deprecated.ocp4_3\.buildconfig_jenkinspipeline_strategy)ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "multi-cluster-spring-boot/skopeo-example/.applier" {
  tmp=$(split_files "multi-cluster-spring-boot/skopeo-example/.applier")

  namespaces=$(get_rego_namespaces "(?!ocp\.deprecated.ocp4_3\.buildconfig_jenkinspipeline_strategy)ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}

@test "secure-spring-boot/.openshift-applier" {
  tmp=$(split_files "secure-spring-boot/.openshift-applier")

  namespaces=$(get_rego_namespaces "ocp\.deprecated\.*")
  cmd="conftest test ${tmp} --output tap ${namespaces}"
  run ${cmd}

  print_info "${status}" "${output}" "${cmd}" "${tmp}"
  [ "$status" -eq 0 ]
}