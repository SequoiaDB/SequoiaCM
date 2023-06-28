#!/bin/bash

# $1 用于传递环境变量，当通过cron执行zkClean时，脚本执行的环境变量可能会缺失，导致 zkClean 脚本无法执行，
# 因此需要调用者将环境变量通过参数的方式传递进来，参见zkNodeCleanCron.sh的调用方式
# 不通过 cron 执行 zkClean 时不需要指定 $1 
if [ -n "$1" ]; then
   PATH="$1:"$PATH
fi

cd `dirname $0`
source ../conf/scm_env.sh
source ./function.sh

setLogPath

checkResult=$(checkOutFileParameter)
if [ ! $? -eq 0 ] ;then
  printTimeAndMsg "$checkResult" "error.out"
  exit 1
fi

psout=`ps -ef | grep 'sequoiacm-clean-file-*.jar zkClean' | grep -v 'grep'`

if [ ! -n "$psout" ]; then
  # clean zookeeper node
  printTimeAndMsg "start clean zookeeper node" "zkClean.out"

  jarPath=$(findJar "clean")
  if [ ! $? -eq 0 ] ;then
    printTimeAndMsg "$jarPath"  "error.out"
    exit 1
  fi
  commandStr="java -jar $jarPath zkClean --zkUrls ${zkUrls} --maxBuffer ${maxBuffer} --maxResidualTime ${maxResidualTime} --maxChildNum ${maxChildNum}"
  eval $commandStr
else
  printTimeAndMsg "zk cleanner is running" "zkClean.out"
fi

exit_code=$?
if [ ! $exit_code -eq 0 ]; then
  printTimeAndMsg "failed to clean zookeeper node,exit code:${exit_code}" "error.out"
  exit 1
fi
