#!/bin/bash
# -----------------------------------------------------------------------------
# scmadmin Script
# -----------------------------------------------------------------------------
BINPATH=$(dirname $0)
cd $BINPATH
cd ../

if [ -z $JAVA_HOME ]; then
    echo "missing JAVA_HOME environment variables"
    exit 1
fi

JAVA=$JAVA_HOME/bin/java

MAINCLASS="com.sequoiacm.cloud.tools.ScmAdmin"
CSPATH=$(pwd)
JARPATH=$(./bin/findCloudToolsJar.sh)
if [ $? -ne 0 ]; then
    echo "failed to find tools jar:"$JARPATH
    exit 1
fi
CLASSPATH="$JARPATH:${CSPATH}/jars/*"

$JAVA -cp $CLASSPATH $MAINCLASS "$@"