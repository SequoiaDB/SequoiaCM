#!/bin/bash
# -----------------------------------------------------------------------------
# scmd Script
# -----------------------------------------------------------------------------
BINPATH=$(dirname $0)
cd $BINPATH
cd ../

if [ -z $JAVA_HOME ]; then
    echo "missing JAVA_HOME environment variables"
    exit 1
fi

JAVA=$JAVA_HOME/bin/java

MAINCLASS="com.sequoiacm.daemon.Scmd"
#CSPATH=$(pwd)
JARPATH=$(./bin/scmdFindJars.sh)
if [ $? -ne 0 ]; then
    echo "failed to find tools jar:"$JARPATH
    exit 1
fi

CLASSPATH="$JARPATH"

$JAVA -cp $CLASSPATH $MAINCLASS "$@"