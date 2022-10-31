#!/usr/bin/python
import sys, getopt
import os
import tarfile
import shutil
import glob
import paramiko
import ConfigParser
import re
import linecache
rootDir = sys.path[0]+os.sep
sys.path.append(rootDir)
import scmCmdExecutor
from scmCmdExecutor import ScmCmdExecutor
from SSHConnection import SSHConnection
TEMP_DIR = rootDir + '..' + os.sep + 'temp_package' + os.sep
XML_PATH = rootDir + ".." + os.sep  + ".." + os.sep + "testcases" + os.sep + "v2.0" + os.sep + "story" + os.sep + "java" + os.sep + "src" + os.sep + "test" + os.sep + "resources" + os.sep + "testng.xml"
POM_PATH = rootDir + ".." + os.sep  + ".." + os.sep + "testcases" + os.sep + "v2.0" + os.sep + "story" + os.sep + "java" + os.sep + "pom.xml"
XML_SERIAL_PATH = rootDir + ".." + os.sep  + ".." + os.sep + "testcases" + os.sep + "v2.0" + os.sep + "story" + os.sep + "java" + os.sep + "src" + os.sep + "test" + os.sep + "resources" + os.sep + "testng-serial.xml"
STORY_SERIAL_PATH = rootDir + ".." + os.sep + "temp_package" + os.sep + "test-report" + os.sep + "testng_serial"
STORY_PATH = rootDir + ".." + os.sep  + "temp_package" + os.sep + "test-report" + os.sep + "testng"
cmdExecutor = ScmCmdExecutor(False)


GET_WAY_URL = ""
MAIN_SDB_URL = ""
S3_URL = ""
WORKSPACE_FILE = ""
TEST_FILE = ""
SCM_INFO_FILE = ""
TEST_RESULT = "======================================TEST RESULT=================================================\n"

def display(exit_code):
    print("")
    print(" --help | -h       : print help message")
    print(" --scm-info        : SCM information:scm.info")
    print(" --workspace-file  : Workspace Template File ")
    sys.exit(exit_code)

def parse_command():
    global SCM_INFO_FILE, WORKSPACE_FILE, TEST_FILE
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc:at:", ["help", "scm-info=", "workspace-file=", "test-file="])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--scm-info"):
            SCM_INFO_FILE = value
        elif name in ("--workspace-file"):
            WORKSPACE_FILE = value
        elif name in ("--testfile"):
            TEST_FILE = value


def updateWs(getWayInfo):
    with open(TEMP_DIR + "workspace_template.json", 'r') as file:
        data = file.read()
        data = data.replace("hostname:8080", str(getWayInfo))
    with open(TEMP_DIR + "workspace_template.json", 'w') as file:
        file.write(data)

def getS3Info(s3Res):
    s3Arr = s3Res[1].split('\n')
    key = s3Arr[1].strip()
    res = re.compile(r'\b[a-zA-Z0-9]+\b',re.I).findall(key)
    if len(res) != 3:
        raise Exception("the S3key is wrong!")
    return res

def execTestBase(mainSdbUrl, getWayInfo, S3Url, xmlPath, reportFile, pomPath):
    try:
        arr = str(mainSdbUrl).split(":")
        if len(arr) == 0:
            raise Exception("mainSdbUrl is not exists!")
        ws_res = cmdExecutor.command("python "+ TEMP_DIR  + "sequoiacm" + os.sep + "scm.py  workspace --clean-all --conf "+ TEMP_DIR + "workspace_template.json --create --grant-all-priv")
        if ws_res == 0:
            ssh1 = SSHConnection(host = arr[0], user = 'root', pwd = 'sequoiadb')
            ssh1.cmd("chmod +x /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh")
            ssh1.cmd("sh /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh set-default-region -r ws_default -u admin -p admin --url " + str(getWayInfo) )
            s3res = ssh1.cmd("sh /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh refresh-accesskey -t admin -u admin -p admin -s " + str(S3Url) )
            if s3res[0] == 0:
                print("----------------------------Success to create S3 key----------------------------------------- ")
                s3Arr = getS3Info(s3res)
                res = cmdExecutor.command("mvn -f "+ pomPath +" surefire-report:report -DxmlFileName="+ xmlPath + " -DreportDir="+ reportFile + " -DmainSdbUrl="+ mainSdbUrl + " -Dsite=twoSite  -DisBaseTest=true  -DgateWayInfo="+ getWayInfo + " -DS3user="+ str(s3Arr[1]) +" -DS3Password="+ str(s3Arr[2]))
                testName = reportFile.split(os.sep)[-1]
                if res !=0:
                    raise Exception("Fail to test " + testName + "case !" )
                else:
                    generateTestFile(reportFile, testName + ": ")
            else:
                raise Exception("Failed to create S3 key")
            ssh1.close()
        else:
            raise Exception("Failed to create workspace")
    except Exception as e:
        raise e

def generateTestFile(path, testItem):
    global TEST_RESULT
    filePath = path + os.sep + "TestSuite.txt"
    if os.path.exists(filePath):
        res = str(testItem) + linecache.getline(filePath, 4) + "see more detail in path: " + path + "\n"
        temp = TEST_RESULT
        TEST_RESULT = temp + res

def runTest(getWayInfo, mainSdbUrl, S3Url, wsPath, testFile):
    try:
        print("----------------------------------create workspace-------------------------------------")
        shutil.copy(wsPath, TEMP_DIR)
        updateWs(getWayInfo)
        res = cmdExecutor.command("python " + rootDir + ".." + os.sep  + ".." + os.sep +"dev.py --compileTest all ")
        if res != 0:
            raise Exception("Failed to compileTest")
        execTestBase(mainSdbUrl, getWayInfo, S3Url, XML_PATH, STORY_PATH, POM_PATH)
        execTestBase(mainSdbUrl, getWayInfo, S3Url, XML_SERIAL_PATH, STORY_SERIAL_PATH, POM_PATH)
        print(TEST_RESULT)
    except Exception as e:
        raise e

def getScmInfo(scmFile):
    global MAIN_SDB_URL, GET_WAY_URL, S3_URL
    with open(scmFile) as lines:
        for line in lines:
            line = line.strip()
            if line.startswith("mainSdbUrl="):
                MAIN_SDB_URL = line.replace("mainSdbUrl=", "")
            if line.startswith("getWayUrl="):
                GET_WAY_URL = line.replace("getWayUrl=", "")
            if line.startswith("S3Url="):
                S3_URL = line.replace("S3Url=", "")

if __name__ == '__main__':
    try:
        parse_command()
        if not os.path.exists(SCM_INFO_FILE) or  len(SCM_INFO_FILE.strip()) == 0:
            raise Exception("Missing scm info !")
        getScmInfo(SCM_INFO_FILE)
        runTest(GET_WAY_URL, MAIN_SDB_URL, S3_URL, WORKSPACE_FILE, TEST_FILE)
    except Exception as e:
        print(e)
        raise e






