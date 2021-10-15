#!/bin/sh

if [ $# -ne 1 ]
then
  echo "ci-version.sh: usage: project" 1>&2
  exit 1
fi

PROJECT="$1"
shift

grep "VERSION_NAME=" "${PROJECT}/gradle.properties" | grep -v PREVIOUS | awk -F= '{print $NF}'
