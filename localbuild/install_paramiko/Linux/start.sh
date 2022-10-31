#!/bin/bash

targetDir="/usr/local/Linux"

function installParamiko()
{  
   tar -xvf $targetDir/ecdsa-0.10.tar.gz -C $targetDir 
   tar -xvf $targetDir/pycrypto-2.6.1.tar.gz -C $targetDir
   tar -xvf $targetDir/paramiko-1.13.0.tar.gz -C $targetDir
   cd $targetDir/ecdsa-0.10
   python setup.py install
   res=$?
   if [ $res -eq 0 ] ; then
       cd $targetDir/pycrypto-2.6.1
       python setup.py install
       res_py=$?
       if [ $res_py -eq 0 ]; then
           cd $targetDir/paramiko-1.13.0
           python setup.py install
           res_pa=$?
           if [ $res_pa -eq 0 ]; then
                 echo "Succeed to install paramiko"
           fi
       fi
   fi 
   cd $rootDir  
   configStr="KexAlgorithms curve25519-sha256@libssh.org,ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521,diffie-hellman-group-exchange-sha256,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group1-sha1"
   kexStr=`cat /etc/ssh/sshd_config | grep 'KexAlgorithms'`
   echo $kexStr
   if [ -z "$kexStr" ] ;then
       echo $configStr >> /etc/ssh/sshd_config
       /etc/init.d/ssh restart
   fi
   #javaStr=`cat /etc/profile | grep -E 'export JAVA_HOME=|JAVA_HOME='`
   javaStr=`echo $JAVA_HOME`
   enVirStr=`cat /etc/environment | grep 'export JAVA_HOME'`
   if [ -z "$enVirStr" ] ;then
       echo "export JAVA_HOME=$javaStr" >> /etc/environment
       source /etc/environment
   fi
}

installParamiko
