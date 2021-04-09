#!/bin/sh

set -e

podman build . -t quay.io/cpaas/agogos-poc-builder-dummy:latest
