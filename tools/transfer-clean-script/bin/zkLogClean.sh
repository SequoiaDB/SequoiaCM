#!/bin/sh

zkBinDir=/opt/sequoiacm/zookeeper-3.4.12/bin
zkDataDir=/opt/sequoiacm/zookeeper-3.4.12/data/1
sleepTime=180

while true
do

    nowDate=`date`
    echo $nowDate "start to clean zookeeper"
    $zkBinDir/zkCleanup.sh $zkDataDir -n 3
    sleep $sleepTime

done;
