#!/bin/bash

#------------------------------------------------------------------------
# A script to deploy a Firebase application.
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
  fatal "usage: project"
fi

PROJECT="$1"
shift

CI_BIN_DIRECTORY=$(realpath .ci) ||
  fatal "could not determine bin directory"

export PATH="${PATH}:${CI_BIN_DIRECTORY}:."

NODE_MODULES_BIN=$(realpath node_modules/.bin) ||
  fatal "node modules are not installed"

FIREBASE="${NODE_MODULES_BIN}/firebase"

CI_FIREBASE_TOKEN=$(head -n 1 ".ci/credentials/Firebase/token.txt") ||
  fatal "could not read firebase token from credentials repository"

info "deploying ${PROJECT}"

if [ -f "${PROJECT}/firebase-aab.conf" ]
then
  ci-deploy-firebase-aab.sh "${PROJECT}" ||
    fatal "could not deploy ${PROJECT} AAB to Firebase"
elif [ -f "${PROJECT}/firebase-apk.conf" ]
then
  ci-deploy-firebase-apk.sh "${PROJECT}" ||
    fatal "could not deploy ${PROJECT} APK to Firebase"
fi
