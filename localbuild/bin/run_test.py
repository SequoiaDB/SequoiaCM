#!/usr/bin/python
# -*- coding:utf-8 -*-
import shutil
import sys
import getopt
import os
import ConfigParser

rootDir = sys.path[0] + os.sep
sys.path.append(rootDir)
from scmCmdExecutor import ScmCmdExecutor

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
    global SCM_INFO_FILE, SSH_FILE, HOST_LIST, PROJECT, SITE, RUNBASE, TESTNG_CONF, PACKAGES, CLASSES, WORK_PATH, CONF
    try:
        options, args = getopt.getopt(sys.argv[1:], "h",
                                      ["help", "scm-info=", "ssh-file=", "host=",
                                       "project=", "site=", "runbase", "testng-conf=", "packages=", "classes=",
                                       "work-path=", "conf="])
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
    with open(workPath + os.sep + 'conf' + os.sep + 'scmtest.properties', 'w') as file:
        file.write(temp)


def runTest(project, site, testngConf, packages, classes, scmInfoPath, workPath, scmTestConf):
    try:
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


def generateTestWorkers(hostArr, sshDict):
    global MACHINE_STR
    # scm.test.workers=
    # root:sequoiadb:XXX:XXX:22,root:sequoiadb:XXX:XX:22
    for idx, host in enumerate(hostArr):
        if idx != 0:
            MACHINE_STR += ","
        if (idx + 1) in sshDict:
            sshArr = str(sshDict[idx + 1]).split(",")
            MACHINE_STR += str(sshArr[0]) + ":" + sshArr[1] + ":" + str(hostArr[idx]) + ":22"
        else:
            sshArr = str(sshDict[0]).split(",")
            MACHINE_STR += str(sshArr[0]) + ":" + sshArr[1] + ":" + str(hostArr[idx]) + ":22"
    if len(MACHINE_STR.strip()) == 0:
        raise Exception("The worker machine is null!")


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
        sshDict = getSSHInfo(SSH_FILE)
        generateTestWorkers(hostArr, sshDict)
        runTest(PROJECT, SITE, TESTNG_CONF, PACKAGES, CLASSES, SCM_INFO_FILE, WORK_PATH, CONF)
    except Exception as e:
        log.error(e, exc_info=True)
        raise e
