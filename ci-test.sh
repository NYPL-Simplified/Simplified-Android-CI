#!/bin/bash

#------------------------------------------------------------------------
# A script to execute tests in different manners
#

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "ci-test.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "ci-test.sh: info: $1" 1>&2
}

BUILD_TYPE="$1"
shift

if [ -z "${BUILD_TYPE}" ]
then
  BUILD_TYPE="pull-request"
fi

JVM_ARGUMENTS="-Xmx2g -XX:+PrintGC -XX:+PrintGCDetails -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8"

info "Gradle JVM arguments: ${JVM_ARGUMENTS}"

case ${BUILD_TYPE} in
  pull-request)
    ./gradlew \
      -Porg.librarysimplified.no_signing=true \
      -Dorg.gradle.jvmargs="${JVM_ARGUMENTS}" \
      -Dorg.gradle.parallel=true \
      -Dorg.gradle.internal.publish.checksums.insecure=true \
      testDebug -x :simplified-tests:testDebug || fatal "could not test"
    ;;

  release)
    ./gradlew \
      -Porg.librarysimplified.no_signing=true \
      -Dorg.gradle.jvmargs="${JVM_ARGUMENTS}" \
      -Dorg.gradle.parallel=true \
      -Dorg.gradle.internal.publish.checksums.insecure=true \
      test || fatal "could not test"
    ;;
esac
