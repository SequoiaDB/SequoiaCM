#!/bin/bash

basePath=$(cd `dirname $0`; pwd)
parentDir=$(cd `dirname ${basePath}`; pwd)
source ${parentDir}/conf/scm_env.sh
source ${basePath}/function.sh

cd `dirname $0`

setLogPath

checkResult=$(checkOutFileParameter)
if [ ! $? -eq 0 ] ;then
  printTimeAndMsg "$checkResult" "error.out"
  exit 1
fi

function Usage()
{
    echo  "Usage: zkNodeCleanCron <subcommand>"
    echo  "Command options:"
    echo  "  help                      help information"
    echo  "  start                     start zkNodeClean cron"
    echo  "  stop                      stop the zkNodeClean cron"
    echo  "  list                      list zkNodeClean cron"
}

function Start()
{
   grepout=`crontab -l | grep -F "${cron} ${basePath}/zkClean.sh"`

   # add crontab with PATH
   if [ ! -n "$grepout" ]; then
      printTimeAndMsg "add clean zookeeper node crontab"
      (crontab -l | grep -v -F "${cron} ${basePath}/zkClean.sh";echo "${cron} ${basePath}/zkClean.sh ${PATH}") | crontab -
   else
      printTimeAndMsg "clean zookeeper node crontab is already exists"
   fi
}

function Stop()
{
   (crontab -l | grep -v -F "${basePath}/zkClean.sh") | crontab -
}

function Status()
{
   grepout=`crontab -l | grep -F "${basePath}/zkClean.sh"`
   if [ -n "$grepout" ]; then
      printTimeAndMsg "$grepout"
   fi
}

if [ "$1" == "start" ]; then
   Start
elif [ "$1" == "stop" ]; then
   Stop
elif [ "$1" == "list" ]; then
   Status
elif [ "$1" == "help" ]; then
   Usage
   exit 0
else
   Usage
   exit 0
fi

