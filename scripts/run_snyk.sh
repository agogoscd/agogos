#!/bin/bash
#
# This script is for Static Application Security Testing (SAST)
# using the SNYK (https://snyk.io/) testing tool.
#
#  Usage: 
#   - in pom.xml in snyk section set: <skip>false</skip>
#   - in service folder run: ./scripts/run_snyk.sh
#  SNYK will create snyk-results.json file with scan results in service folder
#

export VAULT_ADDR=https://vault.corp.redhat.com:8200
export VAULT_NAMESPACE=exd
export HV_TOKEN=$(vault login -method=oidc -field=token 2>/dev/null)

export SNYK_TOKEN=$(vault kv get -field=key kv2/sp/guilds/cp/agogos/dev/snyk)

mvn clean snyk:test


