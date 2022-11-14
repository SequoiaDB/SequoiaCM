#!/bin/bash

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
