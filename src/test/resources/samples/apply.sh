#!/bin/sh

find $(dirname "$0") -name '*.yaml'  | xargs -n1 kubectl apply -f
# -not -name "*[generated].yaml"
find $(dirname "$0") -name '*generated.yaml' | xargs -n1 kubectl create -f
