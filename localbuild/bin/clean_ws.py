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

SCM_INFO_FILE = ""
WORKSPACE_FILE = ""

TEMP_DIR = rootDir + '..' + os.sep + 'temp_package' + os.sep
CONF_DIR = rootDir + '..' + os.sep + 'conf' + os.sep
def display(exit_code):
    print(" --help | -h              : print help message")
    print(" --scm-info <arg>         : scm.info path")
    print(" --workspace-file <arg>   : Workspace Template File")
    sys.exit(exit_code)

def parse_command():
    global SCM_INFO_FILE, WORKSPACE_FILE
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc:at:", ["help", "scm-info=", "workspace-file="])
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

def getScmInfo(scmInfoFile):
    with open(scmInfoFile) as lines:
        for line in lines:
            line = line.strip()
            if line.startswith("getWayUrl="):
                gateWayInfo = line.replace("getWayUrl=", "")
    return gateWayInfo

def updateWs(getWayInfo):
    with open(TEMP_DIR + "workspace_template.json", 'r') as file:
        data = file.read()
        data = data.replace("hostname:8080", str(getWayInfo))
    with open(TEMP_DIR + "workspace_template.json", 'w') as file:
        file.write(data)

def cleanWs(scmInfoFile, workspaceFile):
    try:
        gateWayInfo = getScmInfo(scmInfoFile)
        shutil.copy(workspaceFile, TEMP_DIR)
        workspaceName = workspaceFile.split(os.sep)[-1]
        updateWs(gateWayInfo)
        res = cmdExecutor.command("python " + TEMP_DIR  + "sequoiacm" + os.sep + "scm.py workspace --clean-all --conf " + TEMP_DIR + workspaceName)
        if res != 0:
            raise Exception("Failed to clean all workspace")
    except Exception as e:
        print(e)
        raise e



if __name__ == '__main__':
    try:
        parse_command()
        print("----------------------------start clean workspace-------------------------------------")
        if len(SCM_INFO_FILE.strip()) == 0:
            raise Exception("Missing scm.info !")
        if len(WORKSPACE_FILE.strip()) == 0:
            raise Exception("Missing workspace_template.json !")
        cleanWs(SCM_INFO_FILE, WORKSPACE_FILE)
    except Exception as e:
        print(e)
        raise e