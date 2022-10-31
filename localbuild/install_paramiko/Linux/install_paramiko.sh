#!/bin/bash

hostname="localhost"
rootDir=$(cd "$(dirname "$0")";pwd)

function display(){
   echo "$0 --help | -h"
   echo "$0 [--host]"
   echo ""
   echo " --host                : 主机名（可多个，逗号分隔）"
   echo ""
   exit $1
}

while [ "$1" != "" ]; do
   case $1 in
      --host )            hostname=$2
                          shift
                          ;;
      --help | -h )       display 0
                          ;;
      * )                 echo "Invalid argument: $p"
                          display 1
   esac
   shift
done

user=root
password=sequoiadb
ip_str=$hostname
ip_arr=(${ip_str//,/ })
localhost=$(ip addr |grep 'inet' |grep -v 'inet6\|127.0.0.1' |grep -v grep | awk -F '/' '{print $1}' | awk '{print $2}' )
count=0
cmd="source $rootDir/start.sh"
while (( $count<${#ip_arr[*]} ))
do
for ip in ${localhost};do
   if [[ "${ip}" == "${ip_arr[$count]}"  ]]; then
      ip_check=true
   fi
done
if [[ ${ip_check} != true ]]; then
expect << EOF
set timeout 60
spawn scp -r $rootDir $user@${ip_arr[$count]}:/usr/local
expect {
"yes/no" {send "yes\n";exp_continue}
"password" {send "$password\n"}
}
spawn ssh $user@${ip_arr[$count]} bash /usr/local/Linux/start.sh
expect {
"yes/no" {send "yes\n";exp_continue}
"password" {send "$password\n"}
}
expect eof
EOF
 
else
   cp -r $rootDir /usr/local/Linux
   bash  $rootDir/start.sh
fi
let "count++"
done





