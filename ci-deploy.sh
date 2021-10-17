#!/bin/bash

#------------------------------------------------------------------------
# A script to deploy binaries to various locations.
#

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "ci-deploy.sh: fatal: $1" 1>&2
  exit 1
}

FAILED=0

error()
{
  echo "ci-deploy.sh: error: $1" 1>&2
  FAILED=1
}

info()
{
  echo "ci-deploy.sh: info: $1" 1>&2
}

export PATH="${PATH}:.ci:."

ci-deploy-firebase-conditionally.sh ||
  error "could not deploy Firebase builds"
ci-deploy-fastlane-conditionally.sh ||
  error "could not deploy Fastlane builds"
ci-deploy-central.sh ||
  error "could not deploy to Maven Central"
ci-deploy-git-binaries.sh ||
  error "could not deploy git binaries"

#------------------------------------------------------------------------
# Check if any of the above failed, and give up if they did.

if [ ${FAILED} -eq 1 ]
then
  fatal "one or more deployment steps failed"
fi

#------------------------------------------------------------------------
# Run local deploy hooks if present.
#

if [ -f .ci-local/deploy.sh ]
then
  .ci-local/deploy.sh || fatal "local deploy hook failed"
fi
