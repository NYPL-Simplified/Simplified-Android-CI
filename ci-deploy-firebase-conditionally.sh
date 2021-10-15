#!/bin/bash

#------------------------------------------------------------------------
# A script to deploy to Firebase (conditionally) to various locations.
#

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "ci-deploy-firebase-conditionally.sh: fatal: $1" 1>&2
  exit 1
}

FAILED=0

error()
{
  echo "ci-deploy-firebase-conditionally.sh: error: $1" 1>&2
  FAILED=1
}

info()
{
  echo "ci-deploy-firebase-conditionally.sh: info: $1" 1>&2
}

export PATH="${PATH}:.ci:."

#------------------------------------------------------------------------
# Run Firebase if configured.
#

if [ -f ".ci-local/deploy-firebase-apps.conf" ]
then
  FIREBASE_APPLICATIONS=$(egrep -v '^#' ".ci-local/deploy-firebase-apps.conf") ||
    error "could not list Firebase applications"

  for PROJECT in ${FIREBASE_APPLICATIONS}
  do
    ci-deploy-firebase-install.sh "${PROJECT}" ||
      error "could not install Firebase"
    ci-deploy-firebase.sh "${PROJECT}" ||
      error "could not deploy Firebase app"
  done
else
  info ".ci-local/deploy-firebase-apps.conf does not exist; will not deploy any Firebase apps"
fi

#------------------------------------------------------------------------
# Check if any of the above failed, and give up if they did.

if [ ${FAILED} -eq 1 ]
then
  fatal "one or more deployment steps failed"
fi
