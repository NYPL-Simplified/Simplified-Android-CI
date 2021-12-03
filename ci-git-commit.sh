#!/bin/sh

if [ $# -ne 1 ]
then
  echo "usage: .git" 1>&2
  exit 1
fi

GIT_DIRECTORY="$1"
shift

git --git-dir="${GIT_DIRECTORY}" log -n 1 HEAD | head -c 250
