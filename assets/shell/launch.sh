#!/bin/bash
#
# Copyright (c) 2020. Eugen Covaci
# Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and limitations under the License.
#
#

#  Launcher for Winfoom - Basic Proxy Facade

usage() {
  echo "Usage: launch [arguments]"
  echo "where [arguments] must be any of the following:"
  echo "  --debug             start in debug mode"
  echo "  --systemjre         use the system jre"
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
  if [[ "$arg" != "--debug" && "$arg" != "--systemjre" ]]; then
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
done

if [ -z ${JAVA_EXE+x} ]; then
  JAVA_EXE="./jdk/bin/java"
fi

if [ -e out.log ]; then
  if rm -rf out.log; then
    echo "Cannot remove 'out.log' file. Is there another application instance running?"
    exit 2
  fi
fi

$JAVA_EXE $ARGS -cp . -jar winfoom.jar >out.log 2>&1 &

echo "You can check the application log with: \$tail -f ~/.winfoom/logs/winfoom.log"
echo "If application failed to start, you may get the reason with: \$cat out.log"
