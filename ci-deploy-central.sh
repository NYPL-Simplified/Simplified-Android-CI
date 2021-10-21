#!/bin/bash

#------------------------------------------------------------------------
# A script to deploy binaries to Maven Central.
#

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "ci-deploy-central.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "ci-deploy-central.sh: info: $1" 1>&2
}

if [ ! -f ".ci-local/deploy-maven-central.conf" ]
then
  info ".ci-local/deploy-maven-central.conf does not exist, will not deploy binaries"
  exit 0
fi

#------------------------------------------------------------------------
# Determine version and whether or not this is a snapshot.
#

VERSION_NAME=$(ci-version.sh .) || fatal "Could not determine project version"
VERSION_TYPE=none

echo "${VERSION_NAME}" | grep -E -- '-SNAPSHOT$'
if [ $? -eq 0 ]
then
  VERSION_TYPE=snapshot
else
  VERSION_TAG=$(git describe --tags HEAD --exact-match 2>/dev/null)
  if [ -n "${VERSION_TAG}" ]
  then
    VERSION_TYPE=tag
  fi
fi

info "Version to be deployed is ${VERSION_NAME}"

#------------------------------------------------------------------------
# Publish the built artifacts to wherever they need to go.
#

case ${VERSION_TYPE} in
  none)
    info "Current version is not a snapshot, and there is no tag"
    ;;
  snapshot)
    exec ci-deploy-central-snapshot.sh "${VERSION_NAME}"
    ;;
  tag)
    exec ci-deploy-central-release.sh "${VERSION_NAME}"
    ;;
esac
