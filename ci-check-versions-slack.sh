#!/bin/sh

#------------------------------------------------------------------------
# A script to determine if dependencies are up-to-date and post the results
# to Slack.
#

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "ci-check-versions-slack.sh: fatal: $1" 1>&2
  exit 1
}

error()
{
  echo "ci-check-versions-slack.sh: error: $1" 1>&2
  FAILED=1
}

info()
{
  echo "ci-check-versions-slack.sh: info: $1" 1>&2
}

CI_BIN_DIRECTORY=$(realpath .ci) ||
  fatal "could not determine bin directory"

if [ -z "${SLACK_WEBHOOK}" ]
then
  fatal "SLACK_WEBHOOK is not set"
fi

export PATH="${PATH}:${CI_BIN_DIRECTORY}:."

java -jar "${CI_BIN_DIRECTORY}/ci-tools.jar" \
  check-versions \
  --configuration .ci-local/check-versions.properties \
  --formatter slack | tee version-check.json

exec curl -d "@version-check.json" "${SLACK_WEBHOOK}"
