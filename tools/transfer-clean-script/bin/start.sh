#!/bin/bash

basePath=$(cd `dirname $0`; pwd)
parentDir=$(cd `dirname ${basePath}`; pwd)
source ${parentDir}/conf/scm_env.sh
source ${basePath}/function.sh
wsConfPath="$(pwd)/$1"
cd `dirname $0`

setLogPath

checkResult=$(checkOutFileParameter)
if [ ! $? -eq 0 ] ;then
  printTimeAndMsg "$checkResult" "error.out"
  exit 1
fi

eval "${basePath}/zkNodeCleanCron.sh start"
 
while read line
do
  workspace=`echo $line | awk '{print $1}'`
  month=`echo $line | awk '{print $2}'`
  
  # transfer
  sh ${basePath}/scmTransfer.sh ${workspace} ${month}
  if [ ! $? -eq 0 ];  then
    printTimeAndMsg "transfer failed,workspace=${workspace},month=${month}" "error.out"
    exit 1
  fi

  # start clean background
  printTimeAndMsg "start to clean,workspace=${workspace},month=${month}"
  printTimeAndMsg "nohup sh scmClean.sh ${workspace} ${month}" "clean.out"

  nohup sh scmClean.sh ${workspace} ${month} >> ../log/nohup.out 2>&1 &
done < $wsConfPath

# del crontab
sleep 1
cmd="ps -ef | grep scmClean.sh | grep -v 'grep'"
psout=`eval $cmd`
while [ ! -z "$psout" ]
do
  sleep 1
  psout=`eval $cmd`
done
eval "${basePath}/zkNodeCleanCron.sh stop"