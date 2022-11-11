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
from  xml.etree.ElementTree import ElementTree,Element
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
TEST_JAR_PATH = rootDir + ".." + os.sep  + ".." + os.sep + "driver" + os.sep + "java" + os.sep + "target" + os.sep
cmdExecutor = ScmCmdExecutor(False)


GET_WAY_URL = ""
MAIN_SDB_URL = ""
S3_URL = ""
SSH_HOST_INFO = ""
WORKSPACE_FILE = ""
TEST_FILE = ""
SCM_INFO_FILE = ""
TEST_RESULT = "======================================TEST RESULT=================================================\n"

def display(exit_code):
    print("")
    print(" --help | -h       : print help message")
    print(" --scm-info  <arg>      : SCM information:scm.info")
    print(" --workspace-file <arg> : Workspace Template File ")
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

def read_xml(xmlPath):
    print(xmlPath)
    tree = ElementTree()
    tree.parse(xmlPath)
    return tree

def write_xml(tree, xmlPath):
    tree.write(xmlPath, encoding="utf-8", xml_declaration=True)

def find_nodes(tree, path):
    return tree.findall(path)

def if_match(node, kv_map):
    for key in kv_map:
        if node.get(key) != kv_map.get(key):
            return False
    return True

def get_node_by_keyvalue(nodelist, kv_map):
    res_nodes = []
    for node in nodelist:
        if if_match(node, kv_map):
            res_nodes.append(node)
    return res_nodes

def change_node_properties(nodelist, kv_map, is_delete=False):
    for node in nodelist:
        for key in kv_map:
            if is_delete:
                del node.attrib[key]
            else:
                node.set(key, kv_map.get(key))

def UpdateXml(xmlPath, getWayInfo, mainSdbUrl, s3Arr):
    tree = read_xml(xmlPath)
    nodes = find_nodes(tree,'parameter')
    res_site =get_node_by_keyvalue(nodes,{ "name" : "SITES" })
    change_node_properties(res_site,{"value" : "twoSite" })
    res_test =get_node_by_keyvalue(nodes,{ "name" : "RUNBASETEST" })
    change_node_properties(res_test,{"value" : "true" })
    res_getway =get_node_by_keyvalue(nodes,{ "name" : "GATEWAYS" })
    change_node_properties(res_getway,{"value" : str(getWayInfo) })
    res_sdb =get_node_by_keyvalue(nodes,{ "name" : "MAINSDBURL" })
    change_node_properties(res_sdb,{"value" : str(mainSdbUrl) })
    res_id =get_node_by_keyvalue(nodes,{ "name" : "S3ACCESSKEYID" })
    change_node_properties(res_id,{"value" : str(s3Arr[1]) })
    res_key =get_node_by_keyvalue(nodes,{ "name" : "S3SECRETKEY" })
    change_node_properties(res_key,{"value" : str(s3Arr[2]) })
    write_xml(tree,xmlPath)

def copyJarToLib():
    path = TEST_JAR_PATH + "sequoiacm-driver-*-release" + os.sep + "sequoiacm-driver-*" + os.sep + "*-driver-*.jar"
    hf_path = rootDir + ".." + os.sep  + ".." + os.sep + "src" + os.sep + "imexport" + os.sep + "target" + os.sep
    destPath = rootDir + ".." + os.sep  + ".." + os.sep + "testcases" + os.sep + "v2.0" + os.sep + "testcase-base" + os.sep + "lib" + os.sep
    if os.path.exists(destPath):
        del_files = glob.glob(destPath + "*-driver-*.jar")
        for f in del_files:
            os.remove(f)
    if os.path.exists(TEST_JAR_PATH):
        files = glob.glob(path)
        for file in files:
            jarName = file.split(os.sep)[-1]
            shutil.copyfile(file, destPath + jarName)
    if os.path.exists(hf_path):
        files = glob.glob(hf_path + "sequoiacm-imexport-*.jar")
        for file in files:
            jarName = file.split(os.sep)[-1]
            shutil.copyfile(file, destPath + jarName)

def execTestBase(mainSdbUrl, getWayInfo, S3Url, xmlPath, reportFile, pomPath):
    try:
        sshArr = SSH_HOST_INFO.split(",")
        if len(sshArr) == 0:
            raise Exception("Missing sshInfo !")
        arr = str(S3Url).split(":")
        if len(arr) == 0:
            raise Exception("mainSdbUrl is not exists!")
        ws_res = cmdExecutor.command("python "+ TEMP_DIR  + "sequoiacm" + os.sep + "scm.py  workspace --clean-all --conf "+ TEMP_DIR + "workspace_template.json --create --grant-all-priv")
        if ws_res == 0:
            ssh1 = SSHConnection(host = arr[0], user = str(sshArr[0]), pwd =  str(sshArr[1]))
            ssh1.cmd("chmod +x /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh")
            ssh1.cmd("sh /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh set-default-region -r ws_default -u admin -p admin --url " + str(getWayInfo) )
            s3res = ssh1.cmd("sh /opt/sequoiacm/sequoiacm-s3/bin/s3admin.sh refresh-accesskey -t admin -u admin -p admin -s " + str(S3Url) )
            if s3res[0] == 0:
                print("----------------------------Success to create S3 key----------------------------------------- ")
                s3Arr = getS3Info(s3res)
                shutil.copy(xmlPath, TEMP_DIR)
                xmlTempPath = TEMP_DIR + xmlPath.split(os.sep)[-1]
                UpdateXml(xmlTempPath, getWayInfo, mainSdbUrl, s3Arr)
                res = cmdExecutor.command("mvn -f "+ pomPath +" surefire-report:report -DxmlFileName="+ xmlTempPath + " -DreportDir="+ reportFile )
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
        copyJarToLib()
        if os.path.exists(TEMP_DIR + "test-report" ):
            shutil.rmtree(TEMP_DIR + "test-report")
        execTestBase(mainSdbUrl, getWayInfo, S3Url, XML_PATH, STORY_PATH, POM_PATH)
        execTestBase(mainSdbUrl, getWayInfo, S3Url, XML_SERIAL_PATH, STORY_SERIAL_PATH, POM_PATH)
        print(TEST_RESULT)
    except Exception as e:
        raise e

def getScmInfo(scmFile):
    global MAIN_SDB_URL, GET_WAY_URL, S3_URL, SSH_HOST_INFO
    with open(scmFile) as lines:
        for line in lines:
            line = line.strip()
            if line.startswith("mainSdbUrl="):
                MAIN_SDB_URL = line.replace("mainSdbUrl=", "")
            if line.startswith("getWayUrl="):
                GET_WAY_URL = line.replace("getWayUrl=", "")
            if line.startswith("S3Url="):
                S3_URL = line.replace("S3Url=", "")
            if line.startswith("sshHostInfo="):
                SSH_HOST_INFO = line.replace("sshHostInfo=", "")

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






