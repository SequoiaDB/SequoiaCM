#!/bin/bash
# -----------------------------------------------------------------------------
# scmctl Script
# -----------------------------------------------------------------------------
BINPATH=$(dirname $0)
cd $BINPATH
cd ../

if [ -z $JAVA_HOME ]; then
    echo "missing JAVA_HOME environment variables"
    exit 1
fi

JAVA=$JAVA_HOME/bin/java

MAINCLASS="com.sequoiacm.tools.tag.ScmTagUpgrade"
CSPATH=$(pwd)
JARPATH=$(./bin/findToolsJar.sh)
if [ $? -ne 0 ]; then
    echo "failed to find tools jar:"$JARPATH
    exit 1
fi
CLASSPATH="$JARPATH:${CSPATH}/jars/*"

LOG_BACK_FILE=${CSPATH}/conf/logback.xml
$JAVA -Dlogback.configurationFile=$LOG_BACK_FILE -cp $CLASSPATH $MAINCLASS "$@"



