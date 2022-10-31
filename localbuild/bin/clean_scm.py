#!/usr/bin/python
import sys, getopt
import os
import tarfile
import shutil
import glob
import paramiko
import ConfigParser
import time
rootDir = sys.path[0]+os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
import scmCmdExecutor
from scmCmdExecutor import ScmCmdExecutor
from SSHConnection import SSHConnection

cmdExecutor = ScmCmdExecutor(False)

HOST_LIST = ""
SSH_ARR = []
SSH_INFO_FILE = ""
TEMPLATE = ""
WORKSPACE_FILE = ""
TEMP_DIR = rootDir + '..' + os.sep + 'temp_package' + os.sep

def display(exit_code):
    print(" --help | -h       : print help message")
    print(" --host            : hostname : required, ',' separate ")
    print(" --template        : deploy SCM  template File ")
    sys.exit(exit_code)

def parse_command():
    global HOST_LIST, SSH_INFO_FILE, TEMPLATE, WORKSPACE_FILE
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc:at:", ["help", "host=", "ssh-file=", "template=", "workspace-file="])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--host"):
            HOST_LIST = value
        elif name in ("--ssh-file"):
            SSH_INFO_FILE = value
        elif name in ("--template"):
            TEMPLATE = value
        elif name in ("--workspace-file"):
            WORKSPACE_FILE = value

def getSSHInfo(confFile):
    try:
        sshArr= []
        cf = ConfigParser.ConfigParser()
        cf.read(confFile)
        arr = cf.sections()
        count = 0
        val = 0
        while count < len(arr):
            user = cf.get(arr[count],"user")
            pwd = cf.get(arr[count],"password")
            if str(arr[count]) != "host":
                val = str(arr[count]).replace('host', '')
            sshArr.append(str(user) + "," + str(pwd) + ","+ str(val))
            count += 1
        return sshArr
    except Exception as e:
        print(e)
        print("The information you filled in have insufficient in localbuild.conf!")

def updateCfg(template, hostList):
    with open(template, 'r') as file:
        data = file.read()
        count = 1
        while count <= int(len(hostList)):
            sum = count - 1
            data = data.replace("hostname"+ str(count), str(hostList[sum]))
            count += 1
    with open(template, 'w') as file:
        file.write(data)

def updateWs(getWayInfo):
    with open(TEMP_DIR + "workspace_template.json", 'r') as file:
        data = file.read()
        data = data.replace("hostname:8080", str(getWayInfo))
    with open(TEMP_DIR + "workspace_template.json", 'w') as file:
        file.write(data)

def cleanScm(host, sshInfo, count):
    sshItemArr = []
    sshInfoArr = []
    try:
        for ch in sshInfo:
            arr1 = str(ch).split(",")
            sshItemArr.append(arr1[2])
            sshInfoArr.append(str(arr1[0]) + "," + arr1[1])
        value = 0
        if len(sshItemArr) > 1:
            i = 0
            while i < len(sshItemArr):
                if val == str(count+1):
                    value = i
                    break
                i += 1
        info = str(sshInfoArr[value]).split(",")
        ssh = SSHConnection(host = host, user = info[0], pwd = info[1])
        dbRes = ssh.cmd("su - sdbadmin -c sdblist")
        if dbRes[0] == 0:
            cmdStr = "/opt/sequoiadb/bin/sdb -s \"db = new Sdb();var arr = db.listCollectionSpaces(); String.prototype.endWith = function(endStr){ var d = this.length-endStr.length; return (d >= 0 && this.lastIndexOf(endStr) == d) } ;while(arr.next()) {var obj = arr.current();var str1 = \\\"SCMSYSTEM\\\";var str2 = \\\"SCMAUDIT\\\";var name = obj.toObj().Name.toString();if( name===str1 || name===str2 || name.endWith(\\\"_META\\\")){ db.dropCS(obj.toObj().Name);}}\""
            ssh.cmd(cmdStr)
        else:
            raise Exception(str(deRes[2]))
        daemonRes = ssh.cmd("ps -ef | grep -v grep|grep sequoiacm/daemon |awk '{print $10}'|awk -F 'daemon/' '{print $1}'")
        if daemonRes[0] == 0:
            arr = str(daemonRes[1]).split()
            count = 0
            while count < int(len(arr)):
                ssh.cmd("sh " + str(arr[count]) + "daemon/bin/scmd.sh stop")
                count += 1
        else:
            raise Exception(str(daemonRes[2]))
        pidRes = ssh.cmd("ps -ef | grep -v grep|grep /opt/sequoiacm |awk '{print $2}'")
        if pidRes[0] == 0:
            arr = str(pidRes[1]).split()
            count = 0
            while count < int(len(arr)):
                ssh.cmd("kill -9 " + arr[count])
                count += 1
        else:
            raise Exception(str(pidRes[2]))
        res = ssh.cmd("ls -A /opt/sequoiacm")
        if res[0] == 0:
            ssh.cmd("rm -rf /opt/sequoiacm/*")
        else:
            raise Exception(str(res[2]))
        ssh.close()
    except Exception as e:
        print(e)
        raise e





if __name__ == '__main__':
    try:
        parse_command()
        print("----------------------------start uninstall SCM cluster-------------------------------------")
        if len(HOST_LIST.strip()) == 0:
            raise Exception("Missing hostname!")
        if len(SSH_INFO_FILE.strip()) == 0:
            raise Exception("Missing ssh info!")
        if len(WORKSPACE_FILE.strip()) == 0:
            raise Exception("Missing workspace template !")
        scmHostArr = HOST_LIST.split(",")
        sshArr = getSSHInfo(SSH_INFO_FILE)
        count = 0
        while count < int(len(scmHostArr)):
            cleanScm(scmHostArr[count], sshArr, count)
            count += 1
    except Exception as e:
        print(e)
        raise e