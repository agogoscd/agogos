#!/bin/sh

# find $(dirname "$0") -type f -name '*.yaml' ! -name agogos.yaml | sort | xargs cat > $(dirname "$0")/agogos.yaml

find $(dirname "$0")/crds -type f -name '*.yaml' | sort | xargs cat > $(dirname "$0")/crds.yaml