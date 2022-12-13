#!/bin/bash

USER_WORKING_DIR=$(pwd)
BINPATH=$(dirname $0)
cd $BINPATH
cd ../

if [ -z $JAVA_HOME ]; then
    echo "missing JAVA_HOME environment variables"
    exit 1
fi

JAVA=$JAVA_HOME/bin/java

MAINCLASS="com.sequoiacm.diagnose.DiagnoseEntry"
JARPATH=$(./bin/diagnoseFindJars.sh)
if [ $? -ne 0 ]; then
    echo "failed to collector tools jar:" $JARPATH
    exit 1
fi

CLASSPATH="$JARPATH"

$JAVA -DuserWorkingDirectory=$USER_WORKING_DIR -cp $CLASSPATH $MAINCLASS "$@"