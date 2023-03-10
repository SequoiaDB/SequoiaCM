#!/usr/bin/python
# -*- coding:utf-8 -*-
import shutil
import sys, getopt
import os
import re
import linecache
import ConfigParser

rootDir = sys.path[0] + os.sep
sys.path.append(rootDir)
from scmCmdExecutor import ScmCmdExecutor
from SSHConnection import SSHConnection

TMP_DIR = rootDir + '..' + os.sep + 'tmp' + os.sep
TEST_EXECUTOR_DIR = rootDir + '..' + os.sep + 'tmp' + os.sep + 'test_executor'
SCM_TEST_PATH = rootDir + '..' + os.sep + 'test_executor' + os.sep
TEST_CONF_PATH = rootDir + '..' + os.sep + 'test_executor' + os.sep + "conf" + os.sep
cmdExecutor = ScmCmdExecutor(False)
from logUtil import Logging

LOG_PATH = rootDir + '..' + os.sep + 'tmp' + os.sep + 'runtest.log'
log = Logging(LOG_PATH).get_logger()

SCM_INFO_FILE = ""
SSH_FILE = ""
HOST_LIST = ""
MACHINE_STR = ""
PROJECT = ""
SITE = ""
RUNBASE = False
TESTNG_CONF = ""
PACKAGES = ""
CLASSES = ""
WORK_PATH = rootDir + '..' + os.sep + 'tmp' + os.sep + 'test_executor'
CONF = rootDir + '..' + os.sep + 'test_executor' + os.sep + 'conf'

GATEWAY_URL = ""
S3_URL = ""
S3ACCESSKEYID = ""
S3SECRETKEY = ""
WORKSPACE_FILE = ""
S3_HOST_INFO  = ""
TEST_RESULT = "======================================TEST RESULT=================================================\n"


def display(exit_code):
    print("")
    print(" --help | -h                 : print help message")
    print(" --scm-info       <arg>      : SCM information:scm.info")
    print(" --project        <arg>      : test project: tdd, story, sdv, all")
    print(" --site           <arg>      : site quantity: oneSite, twoSite, fourSite")
    print(" --runbase                   : test base case, can be used with project,site")
    print(" --testng-conf    <arg>      : support testng, testng-serial")
    print(" --ssh-info       <arg>      : ssh information : filled in localbuild.conf")
    print(" --packages       <arg>      : specified package, separated by ',', depend on testng-conf")
    print(" --classes        <arg>      : specified classes, separated by ',', depend on testng-conf")
    print(" --work-path      <arg>      : test-executor work path, default: ./localbuild/tmp/executor")
    print(" --conf           <arg>      : test-executor conf path, default: ./localbuild/test-executor/conf")
    sys.exit(exit_code)


def parse_command():
    global SCM_INFO_FILE, SSH_FILE, HOST_LIST, PROJECT, SITE, RUNBASE, TESTNG_CONF, PACKAGES, CLASSES, WORK_PATH, CONF, WORKSPACE_FILE
    try:
        options, args = getopt.getopt(sys.argv[1:], "h",
                                      ["help", "scm-info=", "ssh-file=", "host=",
                                       "project=", "site=", "runbase", "testng-conf=", "packages=", "classes=",
                                       "work-path=", "conf=", "workspace-file="])
    except getopt.GetoptError, e:
        log.error(e, exc_info=True)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--scm-info"):
            SCM_INFO_FILE = value
        elif name in ("--ssh-file"):
            SSH_FILE = value
        elif name in ("--host"):
            HOST_LIST = value
        elif name in ("--project"):
            PROJECT = value
        elif name in ("--site"):
            SITE = value
        elif name in ("--runbase"):
            RUNBASE = True
        elif name in ("--testng-conf"):
            TESTNG_CONF = value
        elif name in ("--packages"):
            PACKAGES = value
        elif name in ("--classes"):
            CLASSES = value
        elif name in ("--work-path"):
            WORK_PATH = value
        elif name in ("--conf"):
            CONF = value
        elif name in ("--workspace-file"):
            WORKSPACE_FILE = value

def updateWs(gateWayInfo, tmpWorkspaceFile):
    with open(tmpWorkspaceFile, 'r') as file:
        data = file.read()
        data = data.replace("hostname:8080", str(gateWayInfo))
    with open(tmpWorkspaceFile, 'w') as file:
        file.write(data)

def getS3Info(s3Res):
    s3Arr = s3Res[1].split('\n')
    key = s3Arr[1].strip()
    res = re.compile(r'\b[a-zA-Z0-9]+\b',re.I).findall(key)
    if len(res) != 3:
        raise Exception("the S3key is wrong!")
    return res

def execTestCase(project, site, xmlName, runbase, packages, classes, workPath, confPath):
    try:
        log.info('success to update scmtest.properties')
        packageStr = ""
        classesStr = ""
        xmlStr = ""
        if len(packages) != 0:
            packageStr = " --packages " + packages
        if len(classes) != 0:
            classesStr = " --classes " + classes
        if len(xmlName) != 0:
            xmlStr = " --testng-conf " + xmlName
        testStr = "python " + SCM_TEST_PATH + "scmtest.py runtest --project " + project + xmlStr + " --sites " + site + packageStr + classesStr + " --work-path " + workPath + " --conf " + confPath
        if runbase != "":
            testStr += "  --runbase"
        res = cmdExecutor.command(testStr)
        log.info('exec cmd:' + testStr)
        if res == 0:
            generateTestFile(project, site, runbase, workPath)
    except Exception as e:
        raise e


def generateTestFile(moudleName, site, runbase, workPath):
    global TEST_RESULT
    res = ""
    path = workPath + os.sep + "tmp" + os.sep + "test.result"
    if os.path.exists(path):
        with open(path) as lines:
            for line in lines:
                line = line.strip()
                if len(line) != 0:
                    if moudleName == "tdd" or moudleName == "sdv":
                        res = str(moudleName) + " | " + str(site) + " : " + line + "\n"
                    elif runbase != "":
                        res = str(moudleName) + " | " + str(site) + " | " + runbase + " : " + line + "\n"
                    else:
                        res = str(moudleName) + " | " + str(site) + " : " + line + "\n"
                TEST_RESULT += res


def updateScmTestByScmInfo(scmInfoPath, workPath):
    global S3ACCESSKEYID, S3SECRETKEY
    # 　读取scm.info 信息
    with open(scmInfoPath) as lines:
        for line in lines:
            line = line.strip()
            if line.startswith("mainSdbUrl="):
                mainSdbUrl = line.replace("mainSdbUrl=", "")
            if line.startswith("gateWayUrl="):
                gateWayUrl = line.replace("gateWayUrl=", "")
            if line.startswith("sshuser="):
                sshuser = line.replace("sshuser=", "")
            if line.startswith("sshpassword="):
                sshpassword = line.replace("sshpassword=", "")
            if line.startswith("omUrl="):
                omUrl = line.replace("omUrl=", "")
            if line.startswith("sdbuser="):
                sdbuser = line.replace("sdbuser=", "")
            if line.startswith("sdbpassword="):
                sdbpassword = line.replace("sdbpassword=", "")
    # 读取并修改scmtest.properties 信息
    temp = ""
    with open(workPath + os.sep + 'conf' + os.sep + 'scmtest.properties', 'r') as file:
        lines = file.readlines()
        for line in lines:
            if line.find('=') > 0:
                strs = line.replace('\n', '').split('=')
                if str(strs[0]) == 'scm.test.workers':
                    strs[1] = MACHINE_STR
                    temp += str(strs[0]) + "=" + str(strs[1]) + "\n"
                elif str(strs[0]) == 'scm.test.param.GATEWAYS':
                    strs[1] = gateWayUrl
                    temp += str(strs[0]) + "=" + str(strs[1]) + "\n"
                elif str(strs[0]) == 'scm.test.param.SSHUSER':
                    strs[1] = sshuser
                    temp += str(strs[0]) + "=" + str(strs[1]) + "\n"
                elif str(strs[0]) == 'scm.test.param.SSHPASSWD':
                    strs[1] = sshpassword
                    temp += str(strs[0]) + "=" + str(strs[1]) + "\n"
                elif str(strs[0]) == 'scm.test.param.MAINSDBURL':
                    strs[1] = mainSdbUrl
                    temp += str(strs[0]) + "=" + str(strs[1]) + "\n"
                elif str(strs[0]) == 'scm.test.param.SDBUSER':
                    strs[1] = sdbuser
                    temp += str(strs[0]) + "=" + str(strs[1]) + "\n"
                elif str(strs[0]) == 'scm.test.param.SDBPASSWD':
                    strs[1] = sdbpassword
                    temp += str(strs[0]) + "=" + str(strs[1]) + "\n"
                elif str(strs[0]) == 'scm.test.param.OMSERVERURL':
                    strs[1] = omUrl
                    temp += str(strs[0]) + "=" + str(strs[1]) + "\n"
                else:
                    temp += str(strs[0]) + "=" + str(strs[1]) + "\n"
    temp += 'scm.test.param.S3ACCESSKEYID=' + S3ACCESSKEYID + "\n"
    temp += 'scm.test.param.S3SECRETKEY=' + S3SECRETKEY + "\n"
    with open(workPath  + os.sep + 'conf' + os.sep + 'scmtest.properties', 'w') as file:
        file.write(temp)

def createWs(wsPath):
    global TMP_DIR, WORKSPACE_FILE, GATEWAY_URL
    log.info('create workspace, use template:' + wsPath)
    shutil.copy(wsPath, TMP_DIR)
    tmpWorkspaceFile = TMP_DIR + os.path.basename(WORKSPACE_FILE)
    updateWs(GATEWAY_URL, tmpWorkspaceFile)
    res = cmdExecutor.command("python "+ TMP_DIR  + "sequoiacm" + os.sep + "scm.py  workspace --clean-all --conf "+ tmpWorkspaceFile + " --create --grant-all-priv")
    if res != 0:
        raise Exception("Failed to create workspace")
    log.info('create workspace success!')

def generateS3Key():
    global S3_HOST_INFO, S3_URL, GATEWAY_URL, S3ACCESSKEYID, S3SECRETKEY
    sshArr = S3_HOST_INFO.split(",")
    if len(sshArr) == 0:
        raise Exception("Missing sshInfo !")
    arr = str(S3_URL).split(":")
    ssh = SSHConnection(host = arr[0], user = str(sshArr[0]), pwd =  str(sshArr[1]))
    ssh.cmd("chmod +x /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh")
    ssh.cmd("sh /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh set-default-region -r ws_default -u admin -p admin --url " + str(GATEWAY_URL) )
    res = ssh.cmd("sh /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh refresh-accesskey -t admin -u admin -p admin -s " + str(S3_URL))
    s3Arr = getS3Info(res)
    S3ACCESSKEYID = str(s3Arr[1])
    S3SECRETKEY = str(s3Arr[2])


def runTest(project, site, testngConf, packages, classes, scmInfoPath, workPath, scmTestConf, wsPath):
    try:
        createWs(wsPath)
        generateS3Key()

        confPath = workPath + os.sep + 'conf'
        shutil.copytree(scmTestConf, confPath)
        updateScmTestByScmInfo(scmInfoPath, workPath)
        log.info('start to test case:')
        if project == 'all':
            if len(testngConf) != 0:
                raise Exception("The testng-conf parameter is conflict with the all parameter!")
            if RUNBASE:
                # execTestCase("tdd", site, "testng", "runbase", "", "", workPath, confPath)
                execTestCase("sdv", site, "", "runbase", "", "", workPath, confPath)
                execTestCase("story", site, "", "runbase", "", "", workPath, confPath)
            else:
                #  execTestCase("tdd", site, "testng", "", "", "", workPath, confPath)
                execTestCase("sdv", site, "", "", "", "", workPath, confPath)
                execTestCase("story", site, "", "", "", "", workPath, confPath)
        elif project == 'tdd' or project == 'story' or project == 'sdv':
            if RUNBASE:
                execTestCase(project, site, testngConf, "runbase", packages, classes, workPath, confPath)
            else:
                execTestCase(project, site, testngConf, "", packages, classes, workPath, confPath)
        log.info("\n" + TEST_RESULT + "see more detail:" + TMP_DIR + "test_executor" + os.sep + "test-output")
    except Exception as e:
        raise e


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


def getWorkMachineInfo(hostArr, sshDict):
    global MACHINE_STR
    count = 1
    while count <= len(hostArr):
        if sshDict.has_key(count):
            # workers = root:sequoiadb:XXX:XXX:22, root:sequoiadb:XXX:XX:22
            sshArr = str(sshDict[count]).split(",")
            MACHINE_STR += str(sshArr[0]) + ":" + sshArr[1] + ":" + str(hostArr[count - 1]) + ":22" + ","
        else:
            sshArr = str(sshDict[0]).split(",")
            MACHINE_STR += str(sshArr[0]) + ":" + sshArr[1] + ":" + str(hostArr[count - 1]) + ":22" + ","
        count += 1
    if len(MACHINE_STR.strip()) == 0:
        raise Exception("The worker machine is null!")

def parseScmInfo(scmFile):
    global S3_HOST_INFO, S3_URL, GATEWAY_URL
    with open(scmFile) as lines:
        for line in lines:
            line = line.strip()
            if line.startswith("S3Url="):
                S3_URL = line.replace("S3Url=", "")
            if line.startswith("sshHostInfo="):
                S3_HOST_INFO = line.replace("sshHostInfo=", "")
            if line.startswith("gateWayUrl="):
                GATEWAY_URL = line.replace("gateWayUrl=", "")


if __name__ == '__main__':
    try:
        parse_command()
        if not os.path.exists(SCM_INFO_FILE) or len(SCM_INFO_FILE.strip()) == 0:
            raise Exception("Missing scm info !")
        if not os.path.exists(SSH_FILE) or len(SSH_FILE.strip()) == 0:
            raise Exception("Missing SSH info file or SSH info file is not exists!")
        if PROJECT != "all" and PROJECT != "tdd" and PROJECT != "story" and PROJECT != "sdv":
            raise Exception("The project parameter is invalid!")
        if SITE != "oneSite" and SITE != "twoSite" and SITE != "fourSite":
            raise Exception("The site parameter is invalid!")
        hostArr = HOST_LIST.split(",")
        if len(hostArr) == 0:
            raise Exception("Missing hostname!")
        if len(WORK_PATH.strip()) == 0 or WORK_PATH == '/':
            raise Exception("The work-path parameter is invalid! work-path = " + WORK_PATH)
        if len(CONF.strip()) == 0:
            raise Exception("The conf parameter is invalid!")
        if os.path.exists(WORK_PATH):
            shutil.rmtree(WORK_PATH)
            os.makedirs(WORK_PATH)
        parseScmInfo(SCM_INFO_FILE)
        sshDict = getSSHInfo(SSH_FILE)
        getWorkMachineInfo(hostArr, sshDict)
        runTest(PROJECT, SITE, TESTNG_CONF, PACKAGES, CLASSES, SCM_INFO_FILE, WORK_PATH, CONF, WORKSPACE_FILE)
    except Exception as e:
        log.error(e, exc_info=True)
        raise e
