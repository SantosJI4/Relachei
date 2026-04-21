#!/bin/bash
#
# Gradle start up script for UN*X
#

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Attempt to set APP_HOME
APP_HOME=`pwd`

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec java \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
