# Build and Publishing of Operators

This is a reference implementation of building and publishing Go based Operators with the [operator-sdk](https://sdk.operatorframework.io/) using [GitHub Actions](https://github.com/features/actions).

## Overview

### Actions

Two actions are associated with this implementation and are triggered based on the event received:

| Action | Description |
| ------ | ----------- |
| Pull Requests | Validates the content of the pull request<br/><ul><li>Compiles the Go based operator</li><li>Builds and verifies the operator bundle</li></ul>
| Pushes | Performs the following tasks for all push events<br/><ul><li>Compiles the Go based operator</li><li>Push the operator to the image repository (tagged version if tag release)<li>Builds, verifies and pushes the operator bundle to the image repository (tagged version if tag release</li></ul>The following steps occur as part of a tagged release<br/><ul><li><a href="https://docs.github.com/en/free-pro-team@latest/github/administering-a-repository/releasing-projects-on-github" target="_blank">GitHub release</a></li><li>Builds and verifies the operator bundle</li><li>Submitting a pull request against the <a href="https://github.com/operator-framework/community-operators" target="_blank">community-operators</a> using a branch against the <a href="https://github.com/redhat-cop/community-operators" target="_blank">forked copy</a> of the repository to release to  <a href="https://docs.openshift.com/container-platform/4.6/operators/understanding/olm-understanding-operatorhub.html" target="_blank">OperatorHub</a></li></ul> |

## Prerequisites

The following steps must be completed prior to using the assets contained within this implementation:

### Image Repositories

Two image repositories must be available for the pipeline to produce images against:

* Operator
* [Operator Bundle](https://docs.openshift.com/container-platform/4.6/operators/operator_sdk/osdk-working-bundle-images.html) (Using the form `<operator-name>-bundle`)

Repositories within the [CoP Quay Organization](https://quay.io/organization/redhat-cop/) can be requested by submitting an issue or pull request against the [org](https://github.com/redhat-cop/org) repository.

### Secrets

Sensitive information, such as usernames and passwords are needed at various points of the action to interact with external systems and can be stored and provided as [Secrets](https://docs.github.com/en/free-pro-team@latest/actions/reference/encrypted-secrets). 

The following secrets must be created and stored in the repository:

| Name | Description |
| ---- | ----------- |
| `QUAY_USERNAME` | Username of the destination image registry |
| `QUAY_PASSWORD` | Password of the destination image registry |
| `COMMUNITY_OPERATOR_PAT` | [Personal Access Token](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) (See below) to communicate against the GitHub API when submitting the pull request against the CoP [community-operators](https://github.com/redhat-cop/community-operators) repository. |

### Committer Email Address

A commit against a branch of the CoP [community-operators](https://github.com/redhat-cop/community-operators) repository occurs as part of the release to OperatorHub in the [pull](.github/workflows/push.yaml). As part of this implementation, the email address of the committer contains a placeholder (`changeme-redhat-cop-user@redhat.com`). Update this value with the desired email address to represent the individual responsible for the commit.

#### Personal Access Token

A [Personal Access Token](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) must be created with push access to the CoP[community-operators](https://github.com/redhat-cop/community-operators) repository. The user creating the personal access token must have push access against this repository. Permissions to the repository are managed in the [org](https://github.com/redhat-cop/org) repository

## Implementation

Once each of the prerequisite steps have been completed, copy the contents of the [.github](.github) directory to your repository. The workflows will automatically be triggered when any of the events occurs.