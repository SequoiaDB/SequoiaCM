#!/bin/bash

cd `dirname $0`
source ../conf/scm_env.sh
source ./function.sh

processPrintVersionInfo "transfer" $@

ts_workspace=$1
ts_month=$2

setLogPath

checkResult=$(checkOutFileParameter)
if [ ! $? -eq 0 ] ;then
  printTimeAndMsg "$checkResult" "error.out"
  exit 1
fi

# start transfer
printTimeAndMsg "start transfer,workspace=${ts_workspace},month=${ts_month}" "clean.out"

jarPath=$(findJar "transfer")
if [ ! $? -eq 0 ] ;then
  printTimeAndMsg "$jarPath"  "error.out"
  exit 1
fi

commandStr="java -Xmx2048m -jar $jarPath --logbackPath $transferLogbackPath --fileMatcher '{ create_month: \"${ts_month}\" }' --sdbCoord ${metaSdbCoord} --sdbUser ${metaSdbUser} --sdbPassword ${metaSdbPassword} --sdbPasswordFile ${metaSdbPasswordFile} --scmPassword ${scmPassword} --scmPasswordFile ${scmPasswordFile} --siteName ${targetSiteName} --url ${url} --scmUser ${scmUser} --workspace ${ts_workspace} --batchSize ${batchSize} --fileTransferTimeout ${fileTransferTimeout} --fileStatusCheckBatchSize ${fileStatusCheckBatchSize} --fileStatusCheckInterval ${fileStatusCheckInterval}"

#echo $commandStr
eval $commandStr

if [ ! $? -eq 0 ]; then
  printTimeAndMsg "failed to transfer,workspace=${ts_workspace},month=${ts_month}" "error.out"
  exit 1
fi