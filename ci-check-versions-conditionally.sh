#!/bin/bash

#------------------------------------------------------------------------
# A script to check dependency versions conditionally.
#

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "ci-check-versions-conditionally.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "ci-check-versions-conditionally.sh: info: $1" 1>&2
}

export PATH="${PATH}:.ci:."

#------------------------------------------------------------------------
# Check dependency versions if requested.
#

if [ -f ".ci-local/check-versions.properties" ]
then
  PROPERTY_FILES=$(find . -maxdepth 2 -name gradle.properties | sort) ||
    fatal "could not locate gradle.properties"

  VERSION_RELEASE=0
  for PROPERTY_FILE in ${PROPERTY_FILES}
  do
    info "checking ${PROPERTY_FILE}"
    VERSION_AND_TYPE=$(ci-version-and-type.sh "${PROPERTY_FILE}")
    if [ $? -eq 0 ]
    then
      VERSION_TYPE=$(echo "${VERSION_AND_TYPE}" | awk '{print $1}') || continue

      info "${PROPERTY_FILE} specifies ${VERSION_AND_TYPE}"
      if [ "${VERSION_TYPE}" == "release" ]
      then
        VERSION_RELEASE=1
      fi
    fi
  done

  if [ ${VERSION_RELEASE} -eq 1 ]
  then
    info "at least one file specified a release version; we will check dependency versions"
    ci-check-versions.sh || fatal "Dependencies are out of date!"
  else
    info "none of the properties files specified a release version, so we won't check dependency versions"
  fi
else
  info "no .ci-local/check-versions.properties file exists, so we won't check dependency versions"
fi
