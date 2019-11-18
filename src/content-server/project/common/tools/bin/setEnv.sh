#!/bin/bash
# -----------------------------------------------------------------------------
# init env
# -----------------------------------------------------------------------------
if [ -z $JAVA_HOME ]; then
    echo "missing JAVA_HOME environment variables"
    exit 9
fi

JARPATH=$(./findToolsJar.sh)
if [ $? -ne 0 ]; then
    echo "failed to find tools jar:"$JARPATH
    exit 1
fi

export JRE_HOME=$JAVA_HOME/jre
export CLASSPATH=.:$JAVA_HOME/lib:$JRE_HOME/lib:$JARPATH
export PATH=$JAVA_HOME/bin:$JRE_HOME/bin:$PATH
export JAVA=$JAVA_HOME/bin/java
