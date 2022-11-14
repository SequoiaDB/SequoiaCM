#!/bin/bash

cd `dirname $0`
source ../conf/scm_env.sh
source ./function.sh
cl_workspace=$1
cl_month=$2

setLogPath

checkResult=$(checkOutFileParameter)
if [ ! $? -eq 0 ]; then
  printTimeAndMsg "$checkResult" "error.out"
  exit 1
fi

printTimeAndMsg "start clean workspace:${cl_workspace},month=${cl_month}" "clean.out"

jarPath=$(findJar "clean")
if [ ! $? -eq 0 ] ;then
  printTimeAndMsg "$jarPath"  "error.out"
  exit 1
fi

commandStr="java -Xmx2048m -jar $jarPath scmFileClean --logbackPath $cleanLogbackPath --holdingDataSiteName ${targetSiteName} --targetSiteInstances ${targetSiteInstances} --workspace ${cl_workspace} --cleanSiteName ${srcSiteName} --datasourceConf ${datasourceConf} --queueSize ${queueSize} --thread ${thread} --srcSitePasswordFile ${srcSitePasswordFile} --srcSitePassword ${srcSitePassword} --metaSdbCoord ${metaSdbCoord}  --metaSdbUser ${metaSdbUser} --metaSdbPassword ${metaSdbPassword} --metaSdbPasswordFile ${metaSdbPasswordFile} --connectTimeout ${connectTimeout} --socketTimeout ${socketTimeout} --maxConnectionNum ${maxConnectionNum} --keepAliveTimeout ${keepAliveTimeout} --zkUrls ${zkUrls} --fileMatcher '{ \"create_month\": \"${cl_month}\" }'"

#echo $commandStr
eval $commandStr

exit_code=$?
if [ ! $exit_code -eq 0 ]; then
  printTimeAndMsg "failed to clean, workspace:${cl_workspace},month=${cl_month},exit code=${exit_code}" "error.out"
  exit 1
fi
