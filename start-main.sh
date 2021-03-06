#!/bin/bash

BASEDIR=$(dirname $0)
JAVA=java

if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"
fi

$JAVA -Djava.security.policy="$BASEDIR"/java.policy -jar "$BASEDIR"/admwl-demo/target/admwl-demo-1.0-SNAPSHOT.jar $*
