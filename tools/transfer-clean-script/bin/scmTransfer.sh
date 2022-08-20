#!/bin/sh

source ../conf/scm_env.sh
ts_workspace=$1
ts_month=$2

# start transfer
time=`date "+%Y-%m-%d %H:%M:%S"`
echo "${time} start transfer,worksapce=${ts_workspace},month=${ts_month}"

commandStr="java -Xmx2048m -jar $(./scmFileTransferJars.sh)  --fileMatcher '{ create_month: \"${ts_month}\", \$and: [ { \$not: [ { \"site_list.\$0.site_id\": ${targetSiteId} } ] } ] }' --sdbCoord ${metaSdbCoord} --sdbUser ${metaSdbUser} --sdbPassword ${metaSdbPassword} --scmPassword ${scmPassword} --siteId ${targetSiteId} --url ${url} --scmUser ${scmUser} --workspace ${ts_workspace}"

#echo $commandStr
eval $commandStr

if [ ! $? -eq 0 ]; then
   time=`date "+%Y-%m-%d %H:%M:%S"`
   echo "${time} failed to transfer,workspace=${ts_workspace},month=${ts_month}"
   exit 1
fi


