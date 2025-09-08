#!/bin/bash

if [ -z $JAVA_HOME ]; then
    JAVA_BIN=java
else
    JAVA_BIN=$JAVA_HOME/bin/java
fi

echo "JAVA_OPTS: ${JAVA_OPTS}"
echo "ARGS: ${ARGS}"

SCRIPT_PATH=$(dirname $0)

echo "CURRENT_PATH: $SCRIPT_PATH"
cd $SCRIPT_PATH

JAR="$(ls *.jar)"

echo "Command: $JAVA_BIN -jar $JAVA_OPTS $(ls *.jar) $ARGS"

$JAVA_BIN -jar $JAVA_OPTS $(ls *.jar) $ARGS