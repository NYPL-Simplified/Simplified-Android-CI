#!/bin/sh

fatal()
{
  echo "ci-version-and-type.sh: fatal: $1" 1>&2
  exit 1
}

fatalQuiet()
{
  echo "ci-version-and-type.sh: $1" 1>&2
  exit 1
}

if [ $# -ne 1 ]
then
  echo "ci-version-and-type.sh: usage: project" 1>&2
  exit 1
fi

FILE="$1"
shift

if [ ! -f "${FILE}" ]
then
  fatal "${FILE} does not exist"
fi

VERSION_TEXT=$(grep -E -- "VERSION_NAME=" "${FILE}") ||
  fatalQuiet "${FILE} did not provide a VERSION_NAME"
VERSION_NAME=$(echo "${VERSION_TEXT}" | sed 's/VERSION_NAME=//g') ||
  fatalQuiet "${FILE} did not provide a VERSION_NAME"

VERSION_SNAP=$(echo "${VERSION_NAME}" | grep -E -o -- '-SNAPSHOT$')
if [ "${VERSION_SNAP}" == '-SNAPSHOT' ]
then
  echo "snapshot ${VERSION_NAME}"
else
  echo "release ${VERSION_NAME}"
fi
