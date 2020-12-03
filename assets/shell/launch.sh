#!/bin/bash
#  Launcher for Winfoom - Basic Proxy Facade

usage() {
  echo "Usage: launch [arguments]"
  echo "where [arguments] must be any of the following:"
  echo "  --debug             start in debug mode"
  echo "  --systemjre         use the system jre"
  echo "  --gui               start with the graphical user interface"
}

if [ "$1" == "--help" ]; then
  usage
  exit 0
fi

ARGS="-server -XX:+UseG1GC -XX:MaxHeapFreeRatio=30 -XX:MinHeapFreeRatio=10 -Dnashorn.args=--no-deprecation-warning"

if [ ! -z ${FOOM_ARGS+x} ]; then
  ARGS="$ARGS $FOOM_ARGS"
fi

for arg in "$@"; do
  if [[ "$arg" != "--debug" && "$arg" != "--systemjre" && "$arg" != "--gui" ]]; then
    echo "Invalid command, try 'launch --help' for more information"
    exit 1
  fi
  if [ "$arg" == "--debug" ]; then
    ARGS="$ARGS -Dlogging.level.root=DEBUG -Dlogging.level.java.awt=INFO -Dlogging.level.sun.awt=INFO -Dlogging.level.javax.swing=INFO -Dlogging.level.jdk=INFO"
    continue
  fi
  if [ "$arg" == "--systemjre" ]; then
    JAVA_EXE=java
    continue
  fi
  if [ "$arg" == "--gui" ]; then
    ARGS="$ARGS -Dspring.profiles.active=gui"
    continue
  fi
done

if [ -z ${JAVA_EXE+x} ]; then
  JAVA_EXE="./jdk/bin/java"
fi

($JAVA_EXE $ARGS -cp . -jar winfoom.jar > out.log 2>&1 &) && (tail -f out.log)
