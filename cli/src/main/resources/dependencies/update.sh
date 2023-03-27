#!/bin/env sh

# This script updates the isntallation files to required versions

KNATIVE_EVENTING_VERSION=v0.21.4
TEKTON_VERSION=v0.41.1
TEKTON_TRIGGERS_VERSION=v0.22.2

curl -L https://github.com/knative/eventing/releases/download/${KNATIVE_EVENTING_VERSION}/eventing-core.yaml -o $(dirname "$0")/knative-eventing-core.yaml
curl -L https://github.com/knative/eventing/releases/download/${KNATIVE_EVENTING_VERSION}/in-memory-channel.yaml -o $(dirname "$0")/knative-eventing-in-memory-channel.yaml
curl -L https://github.com/knative/eventing/releases/download/${KNATIVE_EVENTING_VERSION}/mt-channel-broker.yaml -o $(dirname "$0")/knative-eventing-mt-channel-broker.yaml

curl -L https://storage.googleapis.com/tekton-releases/pipeline/previous/${TEKTON_VERSION}/release.yaml -o $(dirname "$0")/tekton.yaml

curl -L https://storage.googleapis.com/tekton-releases/triggers/previous/${TEKTON_TRIGGERS_VERSION}/release.yaml -o $(dirname "$0")/tekton-triggers.yaml
curl -L https://storage.googleapis.com/tekton-releases/triggers/previous/${TEKTON_TRIGGERS_VERSION}/interceptors.yaml -o $(dirname "$0")/tekton-triggers-interceptors.yaml
