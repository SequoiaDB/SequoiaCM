#!/bin/sh

source ../conf/scm_env.sh

while read line
do
  workspace=`echo $line | awk '{print $1}'`
  month=`echo $line | awk '{print $2}'`
 
  # transfer
  sh scmTransfer.sh ${workspace} ${month}
  if [ ! $? -eq 0 ];  then
    time=`date "+%Y-%m-%d %H:%M:%S"`
    echo "${time} transfer failed,workspace=${workspace},month=${month}" >> ../log/error.out
    exit 1
  fi


  # start clean background
  time=`date "+%Y-%m-%d %H:%M:%S"`
  echo "${time} start to clean,workspace=${workspace},month=${month}"
  echo "nohup sh scmClean.sh ${workspace} ${month} >> ../log/clean.out 2>&1 &"

  nohup sh scmClean.sh ${workspace} ${month} >> ../log/clean.out 2>&1 &

  # clean zookeeper node
  time=`date "+%Y-%m-%d %H:%M:%S"`
  echo "${time} start clean zookeeper node"

  commandStr="java -jar $(./scmFileCleanJars.sh) zkClean --zkUrls ${zkUrls} --maxBuffer ${maxBuffer} --maxResidualTime ${maxResidualTime} --maxChildNum ${maxChildNum}"

  #echo $commandStr
  eval $commandStr

  exit_code=$?
  if [ ! $exit_code -eq 0 ]; then
     time=`date "+%Y-%m-%d %H:%M:%S"`
     echo "${time} failed to clean zookeeper node,exit code:${exit_code}" 
  fi
 
done < $1

