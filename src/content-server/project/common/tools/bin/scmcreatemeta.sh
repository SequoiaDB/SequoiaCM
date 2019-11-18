#!/bin/bash
# -----------------------------------------------------------------------------
# scmctl Script
# -----------------------------------------------------------------------------

BINPATH=$(dirname $0)
cd $BINPATH
. ./setEnv.sh

cd ../lib

MAINCLASS="com.sequoiacm.tools.ScmGenerateMetaToolImpl"

$JAVA -cp $CLASSPATH $MAINCLASS "$@"