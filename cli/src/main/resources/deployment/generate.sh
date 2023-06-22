#!/bin/sh

find $(dirname "$0")/crds -type f -name '*.yaml' | sort | xargs cat > $(dirname "$0")/crds.yaml
