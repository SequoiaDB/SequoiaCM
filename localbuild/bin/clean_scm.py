#!/usr/bin/python
# -*- coding:utf-8 -*-
import ConfigParser
import getopt
import os
import sys
from scmCmdExecutor import ScmCmdExecutor
from SSHConnection import SSHConnection
from logUtil import Logging

rootDir = sys.path[0] + os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
LOG_PATH = rootDir + '..' + os.sep + 'tmp' + os.sep + 'cleanscm.log'
log = Logging(LOG_PATH).get_logger()
cmdExecutor = ScmCmdExecutor(False)
HOST_LIST = ""
SSH_ARR = []
SSH_INFO_FILE = ""
TMP_DIR = rootDir + '..' + os.sep + 'tmp' + os.sep


def display(exit_code):
    print(" --help | -h       : print help message")
    print(" --host <arg>      : hostname : required, ',' separate ")
    print(" --ssh-file <arg>  : ssh information : filled in localbuild.conf ")
    sys.exit(exit_code)


def parse_command():
    global HOST_LIST, SSH_INFO_FILE
    try:
        options, args = getopt.getopt(sys.argv[1:], "h", ["help", "host=", "ssh-file="])
    except getopt.GetoptError, e:
        log.error(e, exc_info=True)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--host"):
            HOST_LIST = value
        elif name in ("--ssh-file"):
            SSH_INFO_FILE = value


# 获取ssh连接信息
def getSSHInfo():
    try:
        sshArr = {}
        cf = ConfigParser.ConfigParser()
        cf.read(SSH_INFO_FILE)
        arr = cf.sections()
        count = 0
        val = 0
        while count < len(arr):
            user = cf.get(arr[count], "user")
            pwd = cf.get(arr[count], "password")
            if str(arr[count]) != "host":
                val = int(str(arr[count]).replace('host', ''))
            sshArr[val] = str(user) + "," + str(pwd)
            count += 1
        return sshArr
    except Exception as ex:
        log.error("The information you filled in have insufficient in localbuild.conf!, error info:" + ex)
        raise ex


def cleanScm(host, sshInfo):
    dbStr = "/opt/sequoiadb/bin/sdb -s \"db = new Sdb();var arr = db.listCollectionSpaces(); String.prototype.endWith = function(endStr){ var d = this.length-endStr.length; return (d >= 0 && this.lastIndexOf(endStr) == d) } ;while(arr.next()) {var obj = arr.current();var str1 = \\\"SCMSYSTEM\\\";var str2 = \\\"SCMAUDIT\\\";var name = obj.toObj().Name.toString();if( name===str1 || name===str2 || name.endWith(\\\"_META\\\")){ db.dropCS(obj.toObj().Name);}}\""
    daemonStr = "ps -ef | grep -v grep|grep sequoiacm/daemon |awk '{print $10}'|awk -F 'daemon/' '{print $1}'"
    pidStr = "ps -ef | grep -v grep|grep sequoiacm |grep -v /localbuild/bin |awk '{print $2}'"
    try:
        info = str(sshInfo).split(",")
        ssh = SSHConnection(host=host, user=info[0], pwd=info[1])
        # 删除DB中CM所建数据
        dbRes = ssh.cmd("su - sdbadmin -c sdblist")
        if dbRes[0] == 0:
            ssh.cmd(dbStr)
        # 杀死守护进程
        daemonRes = ssh.cmd(daemonStr)
        if daemonRes[0] == 0:
            arr = str(daemonRes[1]).split()
            count = 0
            while count < int(len(arr)):
                ssh.cmd("sh " + str(arr[count]) + "daemon/bin/scmd.sh stop")
                count += 1
        # 杀死SCM 服务进程
        pidRes = ssh.cmd(pidStr)
        if pidRes[0] == 0:
            arr = str(pidRes[1]).split()
            count = 0
            while count < int(len(arr)):
                ssh.cmd("kill -9 " + arr[count])
                count += 1
        # 删除/opt/sequoiacm下相关文件
        res = ssh.cmd("ls -A /opt/sequoiacm")
        if res[0] == 0:
            ssh.cmd("rm -rf /opt/sequoiacm/*")
        ssh.close()
    except Exception as e:
        log.error(e)
        raise e


if __name__ == '__main__':
    try:
        parse_command()
        log.info("start uninstall SCM cluster")
        if len(HOST_LIST.strip()) == 0:
            raise Exception("Missing hostname!")
        if not os.path.exists(SSH_INFO_FILE) or len(SSH_INFO_FILE.strip()) == 0:
            raise Exception("Missing ssh info or path is not exists, path=" + SSH_INFO_FILE)
        scmHostArr = HOST_LIST.split(",")
        sshDict = getSSHInfo()
        sshArr = []
        count = 0
        while count < int(len(scmHostArr)):
            if sshDict.has_key(count + 1):
                sshArr = sshDict[count + 1]
            else:
                sshArr = sshDict[0]
            cleanScm(scmHostArr[count], sshArr)
            count += 1
    except Exception as e:
        log.error(e, exc_info=True)
        raise e