#!/usr/bin/env bats

@test "basic-dotnet-core/.openshift" {
    run conftest test basic-dotnet-core/.openshift --output tap

    [ "$status" -eq 0 ]
}

@test "basic-nginx/.openshift" {
    run conftest test basic-nginx/.openshift --output tap

    [ "$status" -eq 0 ]
}

@test "basic-spring-boot-tekton/.openshift" {
    run conftest test basic-spring-boot-tekton/.openshift --output tap

    [ "$status" -eq 0 ]
}

@test "basic-spring-boot/.openshift basic-tomcat/.openshift" {
    run conftest test basic-spring-boot/.openshift basic-tomcat/.openshift --output tap

    [ "$status" -eq 0 ]
}

@test "blue-green-spring/.openshift" {
    run conftest test blue-green-spring/.openshift --output tap

    [ "$status" -eq 0 ]
}

@test "cucumber-selenium-grid/applier/projects" {
    run conftest test cucumber-selenium-grid/applier/projects --output tap

    [ "$status" -eq 0 ]
}

@test "cucumber-selenium-grid/applier/templates" {
    run conftest test cucumber-selenium-grid/applier/templates --output tap

    [ "$status" -eq 0 ]
}

@test "jenkins-s2i multi-cluster-multi-branch-jee/.openshift" {
    run conftest test jenkins-s2i multi-cluster-multi-branch-jee/.openshift --output tap

    [ "$status" -eq 0 ]
}

@test "multi-cluster-spring-boot/image-mirror-example/.applier/projects" {
    run conftest test multi-cluster-spring-boot/image-mirror-example/.applier/projects --output tap

    [ "$status" -eq 0 ]
}

@test "multi-cluster-spring-boot/image-mirror-example/.applier/templates" {
    run conftest test multi-cluster-spring-boot/image-mirror-example/.applier/templates --output tap

    [ "$status" -eq 0 ]
}

@test "multi-cluster-spring-boot/skopeo-example/.applier/projects" {
    run conftest test multi-cluster-spring-boot/skopeo-example/.applier/projects --output tap

    [ "$status" -eq 0 ]
}

@test "multi-cluster-spring-boot/skopeo-example/.applier/templates" {
    run conftest test multi-cluster-spring-boot/skopeo-example/.applier/templates --output tap

    [ "$status" -eq 0 ]
}

@test "secure-spring-boot/.openshift-applier/templates" {
    run conftest test secure-spring-boot/.openshift-applier/templates --output tap

    [ "$status" -eq 0 ]
}