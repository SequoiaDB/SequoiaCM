#!/bin/sh

source ../conf/scm_env.sh
cl_workspace=$1
cl_month=$2

time=`date "+%Y-%m-%d %H:%M:%S"`
echo "${time} start clean workspace:${cl_workspace},month=${cl_month}"


commandStr="java -Xmx2048m -jar $(./scmFileCleanJars.sh) scmFileClean --holdingDataSiteId ${targetSiteId} --holdingDataSiteInstances ${targetSiteInstances}  --workspace ${cl_workspace} --cleanSiteId ${srcSiteId} --queueSize ${queueSize} --thread ${thread}  --metaSdbCoord ${metaSdbCoord}  --metaSdbUser ${metaSdbUser}  --metaSdbPassword ${metaSdbPassword} --cleanSiteLobSdbCoord ${cleanSiteLobSdbCoord}  --cleanSiteLobSdbUser ${cleanSiteLobSdbUser} --cleanSiteLobSdbPassword ${cleanSiteLobSdbPassword} --zkUrls ${zkUrls} --fileMatcher '{ \"create_month\": \"${cl_month}\", \"site_list.\$1.site_id\": ${srcSiteId} }'"

#echo $commandStr
eval $commandStr

exit_code=$?
if [ ! $exit_code -eq 0 ]; then
   time=`date "+%Y-%m-%d %H:%M:%S"`
   echo "${time} failed to clean, workspace:${cl_workspace},month=${cl_month},exit code=${exit_code}"
fi

