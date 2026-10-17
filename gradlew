#!/bin/sh

##############################################################################
# Gradle startup script for POSIX. Generated for KinoStream CI.
##############################################################################

APP_BASE_NAME=`basename "$0"`
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  case $link in
    /*) PRG="$link" ;;
    *) PRG=`dirname "$PRG"`"/$link" ;;
  esac
done

SAVED_DIR=`pwd`
cd "`dirname "$PRG"`/" >/dev/null 2>&1
APP_HOME=`pwd -P`
cd "$SAVED_DIR" >/dev/null 2>&1

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$CLASSPATH" ]; then
  echo "Gradle wrapper JAR not found: $CLASSPATH" >&2
  exit 1
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD=java
fi

exec "$JAVA_CMD" \
  -Dorg.gradle.appname="$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
