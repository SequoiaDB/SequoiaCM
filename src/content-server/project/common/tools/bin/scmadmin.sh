#!/bin/bash
# -----------------------------------------------------------------------------
# scmctl Script
# -----------------------------------------------------------------------------

BINPATH=$(dirname $0)
USER_WORKING_DIR=$(pwd)
cd $BINPATH
. ./setEnv.sh

cd ../lib

MAINCLASS="com.sequoiacm.tools.ScmAdmin"

$JAVA -DuserWorkingDirectory=$USER_WORKING_DIR -cp $CLASSPATH $MAINCLASS "$@"