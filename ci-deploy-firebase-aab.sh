#!/bin/bash

#------------------------------------------------------------------------
# A script to deploy a single Firebase application.
#

#------------------------------------------------------------------------
# Utility methods
#

fatal()
{
  echo "ci-deploy-firebase-aab.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "ci-deploy-firebase-aab.sh: info: $1" 1>&2
}

#------------------------------------------------------------------------

if [ $# -ne 1 ]
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

CI_FIREBASE="${NODE_MODULES_BIN}/firebase"

CI_FIREBASE_TOKEN=$(head -n 1 ".ci/credentials/Firebase/token.txt") ||
  fatal "could not read firebase token from credentials repository"

START_DIRECTORY=$(pwd) ||
  fatal "could not retrieve starting directory"
cd "${PROJECT}" ||
  fatal "could not switch to project directory"

CI_FIREBASE_AAB=$(head -n 1 "firebase-aab.conf") ||
  fatal "could not read firebase-aab.conf"
CI_FIREBASE_APP_ID=$(head -n 1 "firebase-app-id.conf") ||
  fatal "could not read firebase-app-id.conf"
CI_FIREBASE_GROUPS=$(head -n 1 "firebase-groups.conf") ||
  fatal "could not read firebase-groups.conf"
CI_FIREBASE_AAB=$(realpath "${CI_FIREBASE_AAB}") ||
  fatal "could not resolve AAB"

info "firebase: AAB:    ${CI_FIREBASE_AAB}"
info "firebase: app:    ${CI_FIREBASE_APP_ID}"
info "firebase: groups: ${CI_FIREBASE_GROUPS}"

CI_FIREBASE_AAB_SIZE=$(wc -c "${CI_FIREBASE_AAB}" | cut -d' ' -f1) ||
  fatal "could not determine AAB size"
if [ "${CI_FIREBASE_AAB_SIZE}" == "0" ]
then
  fatal "attempted to submit a zero-size AAB file"
fi

PROPERTY_FILE="${PROJECT}/gradle.properties"
VERSION_AND_TYPE=$(ci-version-and-type.sh "${PROPERTY_FILE}") ||
  fatal "could not determine version and type"
VERSION_TYPE=$(echo "${VERSION_AND_TYPE}" | awk '{print $1}') ||
  fatal "could not determine version type"

info "${PROPERTY_FILE} specifies ${VERSION_AND_TYPE}"
case "${VERSION_TYPE}" in
  release)
    # Use the existing README-CHANGES.txt
    ;;
  snapshot)
    ci-git-commit.sh "${START_DIRECTORY}/.git" > "${START_DIRECTORY}/README-CHANGES.txt"
    echo "..." >> "${START_DIRECTORY}/README-CHANGES.txt"
    ;;
  *)
    fatal "unrecognized release type: ${VERSION_TYPE}"
    ;;
esac

exec "${CI_FIREBASE}" appdistribution:distribute \
  --token "${CI_FIREBASE_TOKEN}" \
  --release-notes-file "${START_DIRECTORY}/README-CHANGES.txt" \
  --app "${CI_FIREBASE_APP_ID}" \
  --groups "${CI_FIREBASE_GROUPS}" \
  "${CI_FIREBASE_AAB}"
