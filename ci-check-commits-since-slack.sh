#!/bin/sh

#------------------------------------------------------------------------
# A script to determine if commits have been made to a branch.
#

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "ci-check-commits-since-slack.sh: fatal: $1" 1>&2
  exit 1
}

error()
{
  echo "ci-check-commits-since-slack.sh: error: $1" 1>&2
  FAILED=1
}

info()
{
  echo "ci-check-commits-since-slack.sh: info: $1" 1>&2
}

CI_BIN_DIRECTORY=$(realpath .ci) ||
  fatal "could not determine bin directory"

if [ -z "${SLACK_WEBHOOK}" ]
then
  fatal "SLACK_WEBHOOK is not set"
fi

export PATH="${PATH}:${CI_BIN_DIRECTORY}:."

java -jar "${CI_BIN_DIRECTORY}/ci-tools.jar" \
  check-commits-since \
  "@.ci-local/check-commits-since.txt" | tee "commits-check.json"

exec curl -d "@commits-check.json" "${SLACK_WEBHOOK}"
