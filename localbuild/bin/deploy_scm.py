#!/usr/bin/python
import sys, getopt
import os
import tarfile
import shutil
import glob
import paramiko
import ConfigParser
import time
import linecache
rootDir = sys.path[0]+os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
import scmCmdExecutor
from scmCmdExecutor import ScmCmdExecutor
from SSHConnection import SSHConnection

BIN_DIR = rootDir + '..' + os.sep + 'bin' + os.sep
TEMP_DIR = rootDir + '..' + os.sep + 'temp_package' + os.sep
CONF_DIR = rootDir + '..' + os.sep + 'conf' + os.sep
cmdExecutor = ScmCmdExecutor(False)

HOST_LIST = []
SCM_INFO_FILE = ""
SSH_ARR = []
SSH_FILE = ""
PACKAGE_FILE = ""
TEMPLATE = ""
SDB_INFO_FILE = ""
IS_FORCE = False

def display(exit_code):
    print("")
    print(" --help | -h                  : print help message")
    print(" --package-file   <arg>       : SCM installation package path ")
    print(" --host           <arg>       : hostname--at least one, ',' separated ")
    print(" --sdb-info       <arg>       : SDB information:sdb.info")
    print(" --output         <arg>       : output SCM information:scm.info")
    print(" --ssh-info       <arg>       : ssh information : filled in localbuild.conf")
    print(" --template       <arg>       : deploy template File with your hostname's number in ./localbuild/conf")
    print(" --force                      : force to install SCM cluster")
    sys.exit(exit_code)

def parse_command():
    global PACKAGE_FILE, TEMPLATE, SDB_INFO_FILE, SCM_INFO_FILE, HOST_LIST, SSH_FILE, IS_FORCE
    try:
        options, args = getopt.getopt(sys.argv[1:], "h", ["help","package-file=", "template=", "sdb-info=", "host=", "output=", "ssh-file=","force"])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--package-file"):
            PACKAGE_FILE = value
        elif name in ("--template"):
            TEMPLATE = value
        elif name in ("--sdb-info"):
            SDB_INFO_FILE = value
        elif name in ("--host"):
            HOST_LIST = value
        elif name in ("--output"):
            SCM_INFO_FILE = value
        elif name in ("--ssh-file"):
            SSH_FILE = value
        elif name in ("--force"):
            IS_FORCE = True


def generateScmInfo(scmInfoFile, deployFile, sshInfo, template):
    hostNum = getS3record(template)
    if sshInfo.has_key(int(hostNum)):
        info = sshInfo[int(hostNum)]
    else:
        info =  sshInfo[0]
    with open(deployFile, 'r') as file:
        data = file.readlines()
    count = 1
    for ele in data:
        if 'ds1,   sequoiadb' in ele:
            arr1 = str(ele).split(",")
            mainSdbStr = str(arr1[2]).strip()
        if 'gateway' in ele:
            arr2 = str(ele).split(",")
            getWayStr = str(arr2[2]).strip() + ":" + str(arr2[3]).strip()
        if 'BindingSite' in ele:
            S3Str = linecache.getline(deployFile, int(count+1))
            arr3 = str(S3Str).split(",")
            S3Url = str(arr3[2]).strip() + ":" + str(arr3[3]).strip()
        count += 1
    with open(scmInfoFile, 'w') as file:
        file.write("mainSdbUrl=")
        file.write(mainSdbStr + "\n")
        file.write("getWayUrl=")
        file.write(getWayStr + "\n")
        file.write("S3Url=")
        file.write(S3Url + "\n")
        file.write("sshHostInfo=")
        file.write(info + "\n")

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

def getS3record(template):
    with open(template, 'r') as file:
        data = file.readlines()
    count = 1
    for ele in data:
        if 'BindingSite' in ele:
            S3Str = linecache.getline(template, int(count+1))
            arr = str(S3Str).split(",")
            hostNum = str(arr[2]).strip().replace("hostname", "")
        count += 1
    return hostNum

def updateWs(getWayInfo):
    with open(TEMP_DIR + "workspace_template.json", 'r') as file:
        data = file.read()
        data = data.replace("hostname:8080", str(getWayInfo))
    with open(TEMP_DIR + "workspace_template.json", 'w') as file:
        file.write(data)

def getSSHInfo(confFile):
    try:
        sshArr= {}
        cf = ConfigParser.ConfigParser()
        cf.read(confFile)
        arr = cf.sections()
        count = 0
        val = 0
        while count < len(arr):
            user = cf.get(arr[count],"user")
            pwd = cf.get(arr[count],"password")
            if str(arr[count]) != "host":
                val = int(str(arr[count]).replace('host', ''))
            sshArr[val] = str(user) + "," + str(pwd)
            count += 1
        return sshArr
    except Exception as e:
        print("The information you filled in have insufficient in localbuild.conf!")
        raise e

def execDeployScm(hostList, template, sdbFile, scmInfoFile, packageFile, sshInfo):
    tarName = packageFile.split(os.sep)[-1]
    cfgName = template.split(os.sep)[-1]
    try:
        tar = tarfile.open(TEMP_DIR + tarName)
        tar.extractall(path = TEMP_DIR)
        shutil.copy(template, TEMP_DIR)
        updateCfg(TEMP_DIR + cfgName, hostList)
        scm_res = cmdExecutor.command("python " + TEMP_DIR  + "sequoiacm" + os.sep + "scm.py cluster --deploy --conf " + TEMP_DIR + cfgName)
        if scm_res == 0 :
            print("Please wait, deploying...")
            time.sleep(60)
            print("-----------------------------------Succeed to deploy SCM cluster---------------------------")
            if os.path.exists(scmInfoFile):
                os.remove(scmInfoFile)
            generateScmInfo(scmInfoFile, str(TEMP_DIR + cfgName), sshInfo, template)
        else:
            raise Exception("Failed to deploy SCM cluster")
    except Exception as e:
        raise e

def existsSDBCluster(sdbFile, sshInfo):
    try:
        cf = ConfigParser.ConfigParser()
        cf.read(sdbFile)
        arrSections = cf.sections()
        count = 0
        arrItems = []
        sshArr = []
        for ele in arrSections:
            arrItems = cf.options(ele)
            if sshInfo.has_key(count+1):
                sshArr = str(sshInfo[count+1]).split(",")
            else:
                sshArr = str(sshInfo[0]).split(",")
            for temp in arrItems:
                hostStr = cf.get(ele, temp)
                sdbArr = hostStr.split(",")
                temp = 0
                while temp < len(sdbArr)-1 :
                    sdbInfo = str(sdbArr[temp]).split(":")
                    ssh = SSHConnection(host = sdbInfo[0], user = sshArr[0], pwd = sshArr[1])
                    res = ssh.cmd("ps -ef | grep -v grep | grep 'sequoiadb(" +  sdbInfo[1] + ")'")
                    if res[0] != 0:
                        raise Exception( str(sdbInfo[0]) +":" + sdbInfo[1] + "is not exists !" )
                    ssh.close()
                    temp += 1
            count += 1
    except Exception as e:
        raise e

def execRedeployScm(hostList, cfgFile, cfgName, scmInfoFile, sshInfo, template):
    try:
        print("uninstalling and reinstalling ,please wait\n")
        #clean scm
        cleanRes = cmdExecutor.command("python " + BIN_DIR + "clean_scm.py --host " + HOST_LIST  + " --ssh-file " + SSH_FILE )
        if cleanRes != 0:
            raise Exception("Failed to clean scm environment")
        scmRes = cmdExecutor.command("python " + TEMP_DIR  + "sequoiacm" + os.sep + "scm.py cluster --deploy --conf " + str(cfgFile))
        if scmRes == 0 :
            print("Please wait, deploying...")
            time.sleep(60)
            print("-----------------------------------Succeed to deploy SCM cluster---------------------------")
            if os.path.exists(scmInfoFile):
                os.remove(scmInfoFile)
            generateScmInfo(scmInfoFile, str(TEMP_DIR + cfgName), sshInfo, template)
        else:
            raise Exception("Failed to deploy SCM cluster")
    except Exception as e:
        raise e


def redeployScm(hostList, template, sdbFile, scmInfoFile, packageFile, cfgFile, isForce, sshInfo):
    cfgName = template.split(os.sep)[-1]
    try:
        if isForce:
            execRedeployScm(hostList, cfgFile, cfgName, scmInfoFile, sshInfo, template)
        else:
            while (True):
                print("Do you want uninstall and reinstall it ?(y/n):\n")
                res = raw_input("Please enter your choice:")
                if res == "Y" or res == "y":
                    execRedeployScm(hostList, cfgFile, cfgName, scmInfoFile, sshInfo, template)
                    break
                elif res == "N" or res == "n":
                    print("know your choice ,exiting!")
                    sys.exit(0)
                else:
                    print("I don't know your choice,please enter again")
                    continue
    except Exception as e:
        raise e

def installScm(packageFile, hostList, template, sdbInfoFile, sshInfo, scmInfoFile, isForce):
    count = 0
    flag_yes = 0
    flag_no = 0
    try:
        existsSDBCluster(sdbInfoFile, sshInfo)
        while count < len(hostList):
            if sshInfo.has_key(count+1):
                info = str(sshInfo[count+1]).split(",")
                ssh = SSHConnection(host = hostList[count], user = info[0], pwd = info[1])
            else:
                info = str(sshInfo[0]).split(",")
                ssh = SSHConnection(host = hostList[count], user = info[0], pwd = info[1])
            scm_res = ssh.cmd("ps -ef | grep -v grep|grep sequoiacm | grep -v /localbuild/bin ")
            if scm_res[0] == 0:
                flag_yes += 1
            else:
                flag_no += 1
            ssh.close()
            count += 1
        if flag_yes <= len(hostList) and flag_yes > 0:
            print("You already install SCM cluster!")
            cfgName = template.split(os.sep)[-1]
            shutil.copy(template, TEMP_DIR)
            updateCfg(TEMP_DIR + cfgName, hostList)
            redeployScm(hostList, template, sdbInfoFile, scmInfoFile, packageFile, TEMP_DIR + cfgName, isForce, sshInfo)
        if flag_no == len(hostList):
            if os.path.exists(TEMP_DIR + os.sep + "sequoiacm"):
                shutil.rmtree(TEMP_DIR + os.sep + "sequoiacm")
            execDeployScm(hostList, template, sdbInfoFile, scmInfoFile, packageFile, sshInfo)
    except Exception as e:
            raise e

if __name__ == '__main__':
    try:
        parse_command()
        print("----------------------------start install and deploy SCM cluster-------------------------------------")
        if os.path.exists(SCM_INFO_FILE):
            os.remove(SCM_INFO_FILE)
        if not os.path.exists(PACKAGE_FILE) or len(PACKAGE_FILE.strip()) == 0:
            raise Exception("Missing SDB installation package or package file is not exists!")
        if not os.path.exists(TEMPLATE) or len(TEMPLATE.strip()) == 0 :
            raise Exception("Missing deploy SCM Template or SCM Template file is not exists!")
        if not os.path.exists(SSH_FILE) or len(SSH_FILE.strip()) == 0 :
            raise Exception("Missing SSH info file or SSH info file is not exists!")
        if not os.path.exists(SDB_INFO_FILE) or len(SDB_INFO_FILE.strip()) == 0 :
            raise Exception("Missing SDB info file or SDB info file is not exists!")
        if len(SCM_INFO_FILE.strip()) == 0 :
            raise Exception("Missing SCM info file path !")
        hostArr = HOST_LIST.split(",")
        sshArr = getSSHInfo(SSH_FILE)
        if len(hostArr)==0:
            raise Exception("Missing hostname")
        installScm(PACKAGE_FILE, hostArr, TEMPLATE, SDB_INFO_FILE, sshArr, SCM_INFO_FILE, IS_FORCE)
    except Exception as e:
        print(e)
        raise e






