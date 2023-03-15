#!/usr/bin/python
# -*- coding:utf-8 -*-
import ConfigParser
import getopt
import linecache
import os
import shutil
import sys
import tarfile
import re

rootDir = sys.path[0] + os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
reload(sys)
sys.setdefaultencoding("utf-8")
from scmCmdExecutor import ScmCmdExecutor
from SSHConnection import SSHConnection
from logUtil import Logging

REMOTE_WORK_DIR = '/opt/scm-localbuild/'
REMOTE_LIB_DIR = REMOTE_WORK_DIR + 'lib/'
REMOTE_JACOCO_AGENT_PATH = REMOTE_LIB_DIR + 'jacocoagent.jar'

LIB_DIR = rootDir + '..' + os.sep + 'lib' + os.sep
JACOCO_LIB_DIR = LIB_DIR + 'jacoco' + os.sep
JACOCO_AGENT_PATH = JACOCO_LIB_DIR + 'jacocoagent.jar'

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
    print(" --output         <arg>       : dir output with SCM information file")
    print(" --ssh-info       <arg>       : ssh information : filled in localbuild.conf")
    print(" --template       <arg>       : deploy template File with your hostname's number in ./localbuild/conf")
    print(" --force                      : force to install SCM cluster")
    sys.exit(exit_code)


def parse_command():
    global PACKAGE_FILE, TEMPLATE, SDB_INFO_FILE, SCM_INFO_FILE, SCM_DEPLOY_INFO_FILE, HOST_LIST, SSH_FILE, IS_FORCE
    try:
        options, args = getopt.getopt(sys.argv[1:], "h",
                                      ["help", "package-file=", "template=", "sdb-info=", "host=", "output=",
                                       "ssh-file=", "force"])
    except getopt.GetoptError as ex:
        log.error(ex, exc_info=True)
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
            SCM_INFO_FILE = value + os.sep + "scm.info"
            SCM_DEPLOY_INFO_FILE = value + os.sep + "scm_deploy.info"
        elif name in ("--ssh-file"):
            SSH_FILE = value
        elif name in ("--force"):
            IS_FORCE = True


def generateScmInfo(scmInfoFile, deployCfgFile, hostList, sshInfo):
    with open(deployCfgFile, 'r') as file:
        data = file.readlines()
    line = 1
    for ele in data:
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
            msStr = linecache.getline(deployCfgFile, int(line + 2))
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


def generateScmDeployInfo(scmDeployInfoFile, deployCfgFile):
    with open(deployCfgFile, 'r') as f:
        data = f.readlines()
    with open(scmDeployInfoFile, 'w') as targetFile:
        targetFile.write('serviceName,hostname,port,coveragePort\n')
        for ele in data:
            if 'jacocoagent.jar' in ele:
                arr = str(ele).split(",")
                serviceName = str(arr[1]).strip()
                hostName = str(arr[2]).strip()
                port = str(arr[3]).strip()
                covPort = re.search(r'port=(\d+)', str(ele)).group(1)
                targetFile.write('{},{},{},{}\n'.format(serviceName, hostName, port, covPort))


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


def updateDeployCfg(template, hostList, sdbFile, sshInfo):
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
            info = str(sshArr).split(',')
            data = data.replace("hostname" + str(count), str(hostList[sum])).replace("sshUser" + str(count),
                                                                                     str(info[0])).replace(
                "sshPassword" + str(count), str(info[1]))
            count += 1
    with open(template, 'w') as file:
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


def execDeployScm(hostList, sshInfoList, scmInfoFile, scmDeployInfoFile, deployCfgFile):
    cmdStr = "python " + TMP_DIR + "sequoiacm" + os.sep + "scm.py cluster --deploy --conf " + deployCfgFile
    log.info('exec cmd:' + cmdStr)
    res = cmdExecutor.command(cmdStr)
    if res == 0:
        log.info('success to deploy scm cluster')
        generateScmInfo(scmInfoFile, str(deployCfgFile), hostList, sshInfoList)
        generateScmDeployInfo(scmDeployInfoFile, str(deployCfgFile))
    else:
        raise Exception("Failed to deploy SCM cluster")


def checkSDBStatus(sdbFile, sshInfo, hostList):
    log.info('start to check sdb environment:')
    cf = ConfigParser.ConfigParser()
    cf.read(sdbFile)
    arrSections = cf.sections()
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


def installScm(packageFile, hostList, template, sdbInfoFile, sshInfoList, scmInfoFile, scmDeployInfoFile, isForce):
    # 检查 DB 集群状态
    checkSDBStatus(sdbInfoFile, sshInfoList, hostList)

    # 基于 cfg 模板生成部署文件，放到 tmp 目录下
    cfgName = os.path.basename(template)
    shutil.copy(template, TMP_DIR)
    updateDeployCfg(TMP_DIR + cfgName, hostList, sdbInfoFile, sshInfoList)

    # 使用最新的 SCM 安装包
    if os.path.exists(TMP_DIR + os.sep + "sequoiacm"):
        shutil.rmtree(TMP_DIR + os.sep + "sequoiacm")
    tarName = os.path.basename(packageFile)
    dirPath = os.path.dirname(packageFile)
    if os.path.normpath(dirPath) != os.path.normpath(TMP_DIR):
        shutil.copy(packageFile, TMP_DIR)
    tar = tarfile.open(TMP_DIR + tarName)
    tar.extractall(path=TMP_DIR)

    # 1. 检查执行机上是否存在 SCM 节点
    # 2. 上传 SCM 部署所需要的代理包
    existsSCMCluster = False
    for idx, host in enumerate(hostList):
        if sshInfoList.has_key(idx + 1):
            sshInfo = str(sshInfoList[idx + 1]).split(",")
            ssh = SSHConnection(host=hostList[idx], user=sshInfo[0], pwd=sshInfo[1])
        else:
            sshInfo = str(sshInfoList[0]).split(",")
            ssh = SSHConnection(host=hostList[idx], user=sshInfo[0], pwd=sshInfo[1])
        res = ssh.cmd("ps -ef | grep -v grep|grep sequoiacm | grep -v /localbuild/bin ")
        if res[0] == 0:
            existsSCMCluster = True
        ssh.makedirs(REMOTE_LIB_DIR)
        ssh.upload(JACOCO_AGENT_PATH, REMOTE_JACOCO_AGENT_PATH)
        ssh.close()

    if existsSCMCluster:
        log.info('SCM cluster is exists!')
        if not isForce:
            chooseContinueOrExit()
        execCleanScm()
    execDeployScm(hostList, sshInfoList, scmInfoFile, scmDeployInfoFile, TMP_DIR + cfgName)


def chooseContinueOrExit():
    while True:
        print("Do you want to clean up the old SCM cluster ?(y/n):\n")
        res = raw_input("Please enter your choice:")
        if res == "Y" or res == "y":
            break
        elif res == "N" or res == "n":
            print("know your choice ,exiting!")
            sys.exit(0)
        else:
            print("I don't know your choice,please enter again")


def execCleanScm():
    log.info('Clean up the old SCM cluster!')
    cmdStr = "python " + BIN_DIR + "clean_scm.py --host " + HOST_LIST + " --ssh-file " + SSH_FILE
    log.info('exec cmd:' + cmdStr)
    res = cmdExecutor.command(cmdStr)
    if res != 0:
        raise Exception("Failed to clean scm environment")


if __name__ == '__main__':
    try:
        parse_command()
        if not os.path.exists(PACKAGE_FILE) or len(PACKAGE_FILE.strip()) == 0:
            raise Exception("Missing SCM installation package or package file is not exists!")
        if not os.path.exists(TEMPLATE) or len(TEMPLATE.strip()) == 0:
            raise Exception("Missing deploy SCM Template or SCM Template file is not exists!")
        if not os.path.exists(SSH_FILE) or len(SSH_FILE.strip()) == 0:
            raise Exception("Missing SSH info file or SSH info file is not exists!")
        if not os.path.exists(SDB_INFO_FILE) or len(SDB_INFO_FILE.strip()) == 0:
            raise Exception("Missing SDB info file or SDB info file is not exists!")
        log.info("start install and deploy SCM cluster")
        if os.path.exists(SCM_INFO_FILE):
            os.remove(SCM_INFO_FILE)
        if os.path.exists(SCM_DEPLOY_INFO_FILE):
            os.remove(SCM_DEPLOY_INFO_FILE)
        hostArr = HOST_LIST.split(",")
        sshArr = getSSHInfo(SSH_FILE)
        log.info('get ssh info, info=' + str(sshArr))
        if len(hostArr) == 0:
            raise Exception("Missing hostname")
        installScm(PACKAGE_FILE, hostArr, TEMPLATE, SDB_INFO_FILE, sshArr, SCM_INFO_FILE, SCM_DEPLOY_INFO_FILE,
                   IS_FORCE)
    except Exception as e:
        log.error(e, exc_info=True)
        raise e
