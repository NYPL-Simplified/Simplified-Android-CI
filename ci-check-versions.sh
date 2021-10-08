#!/bin/sh

#------------------------------------------------------------------------
# A script to determine if dependencies are up-to-date.
#

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "ci-check-versions.sh: fatal: $1" 1>&2
  exit 1
}

error()
{
  echo "ci-check-versions.sh: error: $1" 1>&2
  FAILED=1
}

info()
{
  echo "ci-check-versions.sh: info: $1" 1>&2
}

CI_BIN_DIRECTORY=$(realpath .ci) ||
  fatal "could not determine bin directory"

export PATH="${PATH}:${CI_BIN_DIRECTORY}:."

exec java -jar "${CI_BIN_DIRECTORY}/ci-tools.jar" check-versions --configuration .ci-local/check-versions.properties
