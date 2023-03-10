#!/usr/bin/python
# -*- coding:utf-8 -*-
import getopt
import os
import shutil
import sys
from scmCmdExecutor import ScmCmdExecutor
from logUtil import Logging
rootDir = sys.path[0] + os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
LOG_PATH = rootDir + '..' + os.sep + 'tmp' + os.sep + 'cleanws.log'
log = Logging(LOG_PATH).get_logger()
cmdExecutor = ScmCmdExecutor(False)
SCM_INFO_FILE = ""
WORKSPACE_FILE = ""
TMP_DIR = rootDir + '..' + os.sep + 'tmp' + os.sep
CONF_DIR = rootDir + '..' + os.sep + 'conf' + os.sep
PROJECT_PATH = ""


def display(exit_code):
    print(" --help | -h              : print help message")
    print(" --scm-info <arg>         : scm.info path")
    print(" --workspace-file <arg>   : Workspace Template File")
    sys.exit(exit_code)


def parse_command():
    global SCM_INFO_FILE, WORKSPACE_FILE
    try:
        options, args = getopt.getopt(sys.argv[1:], "h", ["help", "scm-info=", "workspace-file="])
    except getopt.GetoptError, e:
        log.error(e, exc_info=True)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--scm-info"):
            SCM_INFO_FILE = value
        elif name in ("--workspace-file"):
            WORKSPACE_FILE = value


def getScmInfo():
    global PROJECT_PATH
    with open(SCM_INFO_FILE) as lines:
        for line in lines:
            line = line.strip()
            if line.startswith("getWayUrl="):
                gateWayInfo = line.replace("getWayUrl=", "")
            if line.startswith("projectPath="):
                PROJECT_PATH = line.replace("projectPath=", "")
    return gateWayInfo


def updateWs(gateWayInfo, tmpWorkspaceFile):
    with open(tmpWorkspaceFile, 'r') as file1:
        data = file1.read()
        data = data.replace("hostname:8080", str(gateWayInfo))
    with open(tmpWorkspaceFile, 'w') as file2:
        file2.write(data)


def cleanWs():
    try:
        gateWayInfo = getScmInfo()
        shutil.copy(WORKSPACE_FILE, TMP_DIR)
        # workspaceName = WORKSPACE_FILE.split(os.sep)[-1]
        tmpWorkspaceFile = TMP_DIR + os.path.basename(WORKSPACE_FILE)
        updateWs(gateWayInfo, tmpWorkspaceFile)
        cleanWsStr = "python " + PROJECT_PATH + os.sep + "scm.py workspace --clean-all --conf " + tmpWorkspaceFile
        log.info('exec cmd:' + cleanWsStr)
        res = cmdExecutor.command(cleanWsStr)
        if res != 0:
            raise Exception("Failed to clean all workspace")
        log.info("success to clean all workspace")
    except Exception as e:
        log.error(e)
        raise e


if __name__ == '__main__':
    try:
        parse_command()
        log.info("start clean workspace")
        if not os.path.exists(SCM_INFO_FILE) or len(SCM_INFO_FILE.strip()) == 0:
            raise Exception("Missing scm.info or path is not exists, path=" + SCM_INFO_FILE)
        if not os.path.exists(WORKSPACE_FILE) or len(WORKSPACE_FILE.strip()) == 0:
            raise Exception("Missing workspace_template.json or path is not exists, path=" + WORKSPACE_FILE)
        cleanWs()
    except Exception as e:
        log.error(e, exc_info=True)
        raise e
