#!/bin/sh

unamestr=`uname`
if [ "$JAVA_HOME" = '' ]; then
  if [ "$unamestr" = 'Darwin' ]; then
     export JAVA_HOME=`/usr/libexec/java_home`
  else
     echo "JAVA_HOME has not been set."
     exit 0;
  fi
fi

# Requires the jar to be built using
# mvn package
# or
# ant jar
#
#
# You may need to set -Xmx (max heap) and -XX:MaxPermSize
# if your hotspot.log references a lot of classes

CLASSPATH=$CLASSPATH:lib/logback-classic-1.1.2.jar
CLASSPATH=$CLASSPATH:lib/logback-core-1.1.2.jar
CLASSPATH=$CLASSPATH:lib/slf4j-api-1.7.7.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/jre/lib/jfxrt.jar
CLASSPATH=$CLASSPATH:lib/richtextfx-0.6.3.jar
CLASSPATH=$CLASSPATH:lib/flowless-0.4.3.jar
CLASSPATH=$CLASSPATH:lib/undofx-1.1.jar
CLASSPATH=$CLASSPATH:lib/wellbehavedfx-0.1.1.jar
CLASSPATH=$CLASSPATH:lib/reactfx-2.0-M4.jar
CLASSPATH=$CLASSPATH:target/jitwatch-1.0.0-SNAPSHOT.jar

"$JAVA_HOME/bin/java" -cp "$CLASSPATH" $@ org.adoptopenjdk.jitwatch.launch.LaunchUI
