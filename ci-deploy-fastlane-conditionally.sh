#!/bin/bash

#------------------------------------------------------------------------
# A script to deploy to Fastlane (conditionally) to various locations.
#

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "ci-deploy-fastlane-conditionally.sh: fatal: $1" 1>&2
  exit 1
}

FAILED=0

error()
{
  echo "ci-deploy-fastlane-conditionally.sh: error: $1" 1>&2
  FAILED=1
}

info()
{
  echo "ci-deploy-fastlane-conditionally.sh: info: $1" 1>&2
}

export PATH="${PATH}:.ci:."

#------------------------------------------------------------------------
# Run Fastlane if configured.
#

if [ -f ".ci-local/deploy-fastlane-apps.conf" ]
then
  FASTLANE_APPLICATIONS=$(egrep -v '^#' ".ci-local/deploy-fastlane-apps.conf") ||
    fatal "could not list Fastlane applications"

  VERSION_TAG=$(git describe --tags HEAD --exact-match 2>/dev/null)
  if [ -n "${VERSION_TAG}" ]
  then
    info "the current commit is tagged; we will deploy Fastlane apps"
    for PROJECT in ${FASTLANE_APPLICATIONS}
    do
      ci-deploy-fastlane-install.sh "${PROJECT}" ||
        error "could not install Fastlane"
      ci-deploy-fastlane.sh "${PROJECT}" ||
        error "could not deploy Fastlane app"
    done
  else
    info "the current commit is not tagged; we will not deploy any Fastlane apps"
  fi
else
  info ".ci-local/deploy-fastlane-apps.conf does not exist; will not deploy any Fastlane apps"
fi

#------------------------------------------------------------------------
# Check if any of the above failed, and give up if they did.

if [ ${FAILED} -eq 1 ]
then
  fatal "one or more deployment steps failed"
fi
