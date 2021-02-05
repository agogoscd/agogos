#!/bin/sh

find $(dirname "$0") -name '*.yaml'  | xargs -n1 kubectl apply -f

