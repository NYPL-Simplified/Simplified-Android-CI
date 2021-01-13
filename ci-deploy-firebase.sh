#!/bin/bash

#------------------------------------------------------------------------
# A script to deploy all Firebase applications.
#

#------------------------------------------------------------------------
# Utility methods
#

fatal()
{
  echo "ci-deploy-firebase.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "ci-deploy-firebase.sh: info: $1" 1>&2
}

#------------------------------------------------------------------------

if [ $# -lt 1 ]
then
  fatal "usage: project [project ...]"
fi

PROJECT_LIST="$1"

NODE_MODULES_BIN=$(realpath node_modules/.bin) ||
  fatal "node modules are not installed"

FIREBASE="${NODE_MODULES_BIN}/firebase"

CI_FIREBASE_TOKEN=$(head -n 1 ".ci/credentials/Firebase/token.txt") ||
  fatal "could not read firebase token from credentials repository"

info "deploying ${PROJECT_LIST}"

for PROJECT in ${PROJECT_LIST}
do
  info "deploying ${PROJECT}"
  ci-deploy-firebase-one.sh "${PROJECT}" ||
    fatal "could not deploy ${PROJECT} to Firebase"
done
