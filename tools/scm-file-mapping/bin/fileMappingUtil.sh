#!/bin/bash
# -----------------------------------------------------------------------------
# File Mapping Script
# -----------------------------------------------------------------------------
BINPATH=$(dirname $0)
USER_WORKING_DIR=$(pwd)
cd $BINPATH
cd ../

if [ -z $JAVA_HOME ]; then
    echo "missing JAVA_HOME environment variables"
    exit 1
fi

JAVA=$JAVA_HOME/bin/java

MAINCLASS="com.sequoiacm.mappingutil.FileMappingUtil"
JARPATH=$(./bin/findToolsJar.sh)
if [ $? -ne 0 ]; then
    echo "failed to find tools jar:" $JARPATH
    exit 1
fi

CLASSPATH="$JARPATH"

$JAVA -DuserWorkingDirectory=$USER_WORKING_DIR -cp $CLASSPATH $MAINCLASS "$@"