#!/usr/bin/python
# -*- coding:utf-8 -*-
import ConfigParser
import getopt
import linecache
import os
import shutil
import sys
import tarfile
import time
import re

rootDir = sys.path[0] + os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
reload(sys)
sys.setdefaultencoding("utf-8")
from scmCmdExecutor import ScmCmdExecutor
from SSHConnection import SSHConnection
from logUtil import Logging

LOG_PATH = rootDir + '..' + os.sep + 'tmp' + os.sep + 'deployscm.log'
BIN_DIR = rootDir + '..' + os.sep + 'bin' + os.sep
TMP_DIR = rootDir + '..' + os.sep + 'tmp' + os.sep
CONF_DIR = rootDir + '..' + os.sep + 'conf' + os.sep
cmdExecutor = ScmCmdExecutor(False)
log = Logging(LOG_PATH).get_logger()

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
        options, args = getopt.getopt(sys.argv[1:], "h",
                                      ["help", "package-file=", "template=", "sdb-info=", "host=", "output=",
                                       "ssh-file=", "force"])
    except getopt.GetoptError, e:
        log.error(e, exc_info=True)
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


def generateScmInfo(scmInfoFile, deployFile, hostList, sshInfo, template):
    hostNum = getS3record(template)
    if sshInfo.has_key(int(hostNum)):
        info = sshInfo[int(hostNum)]
    else:
        info =  sshInfo[0]
    with open(deployFile, 'r') as file:
        data = file.readlines()
    gateWayHost = ""
    line = 1
    for ele in data:
        if 'BindingSite' in ele:
            S3Str = linecache.getline(deployFile, int(line+1))
            arr = str(S3Str).split(",")
            S3Url = str(arr[2]).strip() + ":" + str(arr[3]).strip()
        if 'ds1,   sequoiadb' in ele:
            arr1 = str(ele).split(",")
            mainSdbUrl = str(arr1[2]).strip()
        if 'gateway' in ele:
            arr2 = str(ele).split(",")
            gateWayHost = str(arr2[2]).strip()
            gateWayUrl = str(arr2[2]).strip() + ":" + str(arr2[3]).strip()
            count = 0
            while count < len(hostList):
                if str(hostList[count]) == gateWayHost:
                    if sshInfo.has_key(count):
                        arr = str(sshInfo[count]).split(',')
                    else:
                        arr = str(sshInfo[0]).split(',')
                    sshuser = str(arr[0])
                    sshpassword = str(arr[1])
                count += 1
        if '[metasource]' in ele:
            msStr = linecache.getline(deployFile, int(line + 2))
            arr3 = str(msStr).split(',')
            sdbuser = str(arr3[2].strip())
            sdbpassword = str(arr3[3].strip())
        if 'om-server' in ele:
            arr4 = str(ele).split(",")
            omUrl = str(arr4[2]).strip() + ":" + str(arr4[3]).strip()
        line += 1
    linecache.clearcache()
    with open(scmInfoFile, 'w') as file:
        file.write("mainSdbUrl=")
        file.write(mainSdbUrl + "\n")
        file.write("gateWayUrl=")
        file.write(gateWayUrl + "\n")
        file.write("sshuser=")
        file.write(sshuser + "\n")
        file.write("sshpassword=")
        file.write(sshpassword + "\n")
        file.write("omUrl=")
        file.write(omUrl + "\n")
        file.write("sdbuser=")
        file.write(sdbuser + "\n")
        file.write("sdbpassword=")
        file.write(sdbpassword + "\n")
        file.write("S3Url=")
        file.write(S3Url + "\n")
        file.write("sshHostInfo=")
        file.write(info + "\n")

def updateCfgBySdbInfo(sdbInfoArr, template):
    with open(template, 'r') as file:
        dataLine = file.readlines()
        count = 1
        for ele in dataLine:
            if '[metasource]' in ele:
                str1 = linecache.getline(template, int(count + 2))
            count += 1
    linecache.clearcache()
    with open(template, 'r') as file:
        data = file.read()
        str2 = "'" + str(sdbInfoArr[0]) + "', domain1,  sdbadmin, sequoiadb,"
        data = data.replace(str(str1.strip()), str(str2))
        res = re.findall(r',   sequoiadb(.*?)sdbadmin', data)
        if len(sdbInfoArr) < len(res):
            raise Exception("The number of coord in sdb.info less than the deploy template ! ")
        count = 0
        for ele in res:
            data = data.replace(str(ele), "," + str(sdbInfoArr[count] + ","))
            count += 1
    with open(template, 'w') as file:
        file.write(data)


def updateCfg(template, hostList, sdbFile, sshInfo):
    sdbInfoArr = []
    cf = ConfigParser.ConfigParser()
    cf.read(sdbFile)
    arrSections = cf.sections()
    for ele in arrSections:
        arrItems = cf.options(ele)
        for temp in arrItems:
            hostStr = cf.get(ele, temp)
            str1 = hostStr.split(',')
            sdbInfoArr.append(str1[0])
    updateCfgBySdbInfo(sdbInfoArr, template)
    with open(template, 'r') as file:
        data = file.read()
        count = 1
        while count <= int(len(hostList)):
            sum = count - 1
            if sshInfo.has_key(count):
                sshArr = sshInfo[count]
            else:
                sshArr = sshInfo[0]
            info =  str(sshArr).split(',')
            data = data.replace("hostname" + str(count), str(hostList[sum])).replace("sshUser" + str(count), str(info[0])).replace("sshPassword" + str(count), str(info[1]))
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
    with open(TMP_DIR + "workspace_template.json", 'r') as file:
        data = file.read()
        data = data.replace("hostname:8080", str(getWayInfo))
    with open(TMP_DIR + "workspace_template.json", 'w') as file:
        file.write(data)


def getSSHInfo(confFile):
    try:
        sshArr = {}
        cf = ConfigParser.ConfigParser()
        cf.read(confFile)
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
    except Exception as e:
        log.error("The information you filled in have insufficient in localbuild.conf!")
        raise e


def execDeployScm(hostList, template, sdbFile, scmInfoFile, packageFile, sshInfo):
    tarName = packageFile.split(os.sep)[-1]
    cfgName = template.split(os.sep)[-1]
    try:
        dirPath = os.path.dirname(packageFile)
        if os.path.normpath(dirPath) != os.path.normpath(TMP_DIR):
            # defferent path  
            shutil.copy(packageFile, TMP_DIR)
        tar = tarfile.open(TMP_DIR + tarName)
        tar.extractall(path=TMP_DIR)
        shutil.copy(template, TMP_DIR)
        updateCfg(TMP_DIR + cfgName, hostList, sdbFile, sshInfo)
        scm_res_str = "python " + TMP_DIR + "sequoiacm" + os.sep + "scm.py cluster --deploy --conf " + TMP_DIR + cfgName
        scm_res = cmdExecutor.command(scm_res_str)
        log.info('exec cmd:' + scm_res_str)
        if scm_res == 0:
            print("Please wait, deploying...")
            log.info('success to deploy scm cluster')
            time.sleep(60)
            if os.path.exists(scmInfoFile):
                os.remove(scmInfoFile)
            generateScmInfo(scmInfoFile, str(TMP_DIR + cfgName), hostList, sshInfo, template)
        else:
            raise Exception("Failed to deploy SCM cluster")
    except Exception as e:
        raise e


def existsSDBCluster(sdbFile, sshInfo, hostList):
    log.info('start to check sdb environment:')
    try:
        cf = ConfigParser.ConfigParser()
        cf.read(sdbFile)
        arrSections = cf.sections()
        arrItems = []
        sshArr = []
        for ele in arrSections:
            arrItems = cf.options(ele)
            for temp in arrItems:
                hostStr = cf.get(ele, temp)
                sdbArr = hostStr.split(",")
                temp = 0
                while temp < len(sdbArr) - 1:
                    sdbInfo = str(sdbArr[temp]).split(":")
                    count = 0
                    while count < len(hostList):
                        if str(sdbInfo[0]) == str(hostList[count]):
                            if count == 0 or len(sshInfo) == 1:
                                sshArr = str(sshInfo[0]).split(",")
                            else:
                                sshArr = str(sshInfo[count + 1]).split(",")
                        count += 1
                    if len(sshArr) == 0:
                        sshArr = str(sshInfo[0]).split(",")
                    ssh = SSHConnection(host=sdbInfo[0], user=sshArr[0], pwd=sshArr[1])
                    res = ssh.cmd("ps -ef | grep -v grep | grep 'sequoiadb(" + sdbInfo[1] + ")'")
                    if res[0] != 0:
                        raise Exception(str(sdbInfo[0]) + ":" + sdbInfo[1] + "is not exists !")
                    ssh.close()
                    temp += 1
    except Exception as e:
        raise e


def execRedeployScm(hostList, cfgFile, cfgName, scmInfoFile, sshInfo, template, packageFile):
    try:
        print("uninstalling and reinstalling ,please wait\n")
        # clean scm
        cleanscmStr = "python " + BIN_DIR + "clean_scm.py --host " + HOST_LIST + " --ssh-file " + SSH_FILE
        log.info('exec cmd:' + cleanscmStr)
        cleanRes = cmdExecutor.command(cleanscmStr)
        if cleanRes != 0:
            raise Exception("Failed to clean scm environment")
        tarName = packageFile.split(os.sep)[-1]
        dirPath = os.path.dirname(packageFile)
        if os.path.exists(TMP_DIR + os.sep + "sequoiacm"):
            shutil.rmtree(TMP_DIR + os.sep + "sequoiacm")
        if os.path.normpath(dirPath) != os.path.normpath(TMP_DIR):
            # defferent path  
            shutil.copy(packageFile, TMP_DIR)
        tar = tarfile.open(TMP_DIR + tarName)
        tar.extractall(path=TMP_DIR)
        scmResStr = "python " + TMP_DIR + "sequoiacm" + os.sep + "scm.py cluster --deploy --conf " + str(cfgFile)
        scmRes = cmdExecutor.command(scmResStr)
        log.info('exec cmd:' + scmResStr)
        if scmRes == 0:
            print("Please wait, deploying...")
            log.info('success to deploy scm cluster')
            time.sleep(60)
            if os.path.exists(scmInfoFile):
                os.remove(scmInfoFile)
            generateScmInfo(scmInfoFile, str(TMP_DIR + cfgName), hostList, sshInfo, template)
        else:
            raise Exception("Failed to deploy SCM cluster")
    except Exception as e:
        raise e


def redeployScm(hostList, template, sdbFile, scmInfoFile, packageFile, cfgFile, isForce, sshInfo):
    cfgName = template.split(os.sep)[-1]
    try:
        if isForce:
            execRedeployScm(hostList, cfgFile, cfgName, scmInfoFile, sshInfo, template, packageFile)
        else:
            while (True):
                print("Do you want uninstall and reinstall it ?(y/n):\n")
                res = raw_input("Please enter your choice:")
                if res == "Y" or res == "y":
                    execRedeployScm(hostList, cfgFile, cfgName, scmInfoFile, sshInfo, template, packageFile)
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
    existsSCMCluster = False

    existsSDBCluster(sdbInfoFile, sshInfo, hostList)
    while count < len(hostList):
        if sshInfo.has_key(count + 1):
            info = str(sshInfo[count + 1]).split(",")
            ssh = SSHConnection(host=hostList[count], user=info[0], pwd=info[1])
        else:
            info = str(sshInfo[0]).split(",")
            ssh = SSHConnection(host=hostList[count], user=info[0], pwd=info[1])
        scm_res = ssh.cmd("ps -ef | grep -v grep|grep sequoiacm | grep -v /localbuild/bin ")
        if scm_res[0] == 0:
            existsSCMCluster = True
        ssh.close()
        count += 1
    if existsSCMCluster:
        log.info('SCM cluster is exists!')
        cfgName = template.split(os.sep)[-1]
        shutil.copy(template, TMP_DIR)
        updateCfg(TMP_DIR + cfgName, hostList, sdbInfoFile,sshInfo)
        redeployScm(hostList, template, sdbInfoFile, scmInfoFile, packageFile, TMP_DIR + cfgName, isForce, sshInfo)
    else:
        if os.path.exists(TMP_DIR + os.sep + "sequoiacm"):
            shutil.rmtree(TMP_DIR + os.sep + "sequoiacm")
        execDeployScm(hostList, template, sdbInfoFile, scmInfoFile, packageFile, sshInfo)


if __name__ == '__main__':
    try:
        parse_command()
        log.info("start install and deploy SCM cluster")
        if os.path.exists(SCM_INFO_FILE):
            os.remove(SCM_INFO_FILE)
        if not os.path.exists(PACKAGE_FILE) or len(PACKAGE_FILE.strip()) == 0:
            raise Exception("Missing SCM installation package or package file is not exists!")
        if not os.path.exists(TEMPLATE) or len(TEMPLATE.strip()) == 0:
            raise Exception("Missing deploy SCM Template or SCM Template file is not exists!")
        if not os.path.exists(SSH_FILE) or len(SSH_FILE.strip()) == 0:
            raise Exception("Missing SSH info file or SSH info file is not exists!")
        if not os.path.exists(SDB_INFO_FILE) or len(SDB_INFO_FILE.strip()) == 0:
            raise Exception("Missing SDB info file or SDB info file is not exists!")
        if len(SCM_INFO_FILE.strip()) == 0:
            raise Exception("Missing SCM info file path !")
        hostArr = HOST_LIST.split(",")
        sshArr = getSSHInfo(SSH_FILE)
        log.info('get ssh info, info=' + str(sshArr))
        if len(hostArr) == 0:
            raise Exception("Missing hostname")
        installScm(PACKAGE_FILE, hostArr, TEMPLATE, SDB_INFO_FILE, sshArr, SCM_INFO_FILE, IS_FORCE)
    except Exception as e:
        log.error(e, exc_info=True)
        raise e