#!/bin/sh


set -e

dir=$(dirname "$0")

for file in `find ${dir}/*.yaml`; do
    crd=$(yq eval '.metadata.name' ${file})
    echo "Cleaning up ${crd}"
    kubectl delete ${crd} --all-namespaces --all
done
