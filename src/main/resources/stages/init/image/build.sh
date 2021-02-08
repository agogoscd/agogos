#!/bin/sh

set -e

podman build . -t quay.io/redhat_emp1/cpaas-next-poc-stage-init:latest
