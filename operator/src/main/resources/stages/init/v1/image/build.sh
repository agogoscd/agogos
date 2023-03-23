#!/bin/sh

set -e

DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
PARENT=`dirname ${DIR}`
VERSION=`basename ${PARENT}`

echo "Building image"
podman build . -t quay.io/agogos/stage-init:${VERSION}

if [[ ! -z "${PUSH}" ]]; then
    echo "Pushing image"
    podman push quay.io/agogos/stage-init:${VERSION}
else
    echo "Pushing skipped"
fi
