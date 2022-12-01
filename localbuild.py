#!/usr/bin/python
import sys, getopt
import os
import tarfile
import shutil
import glob
import paramiko
import time
import datetime
rootDir = sys.path[0]+os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
import scmCmdExecutor
from scmCmdExecutor import ScmCmdExecutor

BIN_DIR = sys.path[0] + os.sep + 'localbuild' + os.sep + 'bin' + os.sep
CONF_DIR = sys.path[0] + os.sep + 'localbuild' + os.sep + 'conf' + os.sep
TEMP_DIR = sys.path[0] + os.sep + 'localbuild' + os.sep + 'temp_package' + os.sep

COMPILE = False
IS_INSTALL_SDB = False
IS_INSTALL_SCM = False
IS_RUN_TEST = False
IS_FORCE = False
IS_CLEAN_SCM = False
IS_CLEAN_WS = False
HOST_LIST = ""

cmdExecutor = ScmCmdExecutor(False)

CONF_FILE = CONF_DIR + 'localbuild.conf'
WORKSPACE_FILE = CONF_DIR + 'workspace_template.json'
SDB_INFO_FILE = TEMP_DIR + 'sdb.info'
SCM_INFO_FILE = TEMP_DIR + 'scm.info'


def display(exit_code):
    print(" --help | -h       : print help message")
    print(" --compile | -c    : compile ")
    print(" --installsdb      : install to SDB cluster ")
    print(" --installscm      : install to SCM cluster ")
    print(" --cleanscm        : clean to SCM cluster ")
    print(" --runTest         : execute basic test cases")
    print(" --host <arg>      : hostname : required, ',' separate ")
    print(" --force           : force to install scm or sdb ")
    sys.exit(exit_code)

def parse_command():
    global IS_INSTALL_SCM, IS_INSTALL_SDB, COMPILE, IS_RUN_TEST, HOST_LIST, IS_FORCE, IS_CLEAN_SCM, IS_CLEAN_WS
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc", ["help", "compile", "installsdb", "installscm", "cleanscm", "runtest", "host=", "force", "cleanws"])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("-c","--compile"):
            COMPILE = True
        elif name in ("--installsdb"):
            IS_INSTALL_SDB = True
        elif name in ("--installscm"):
            IS_INSTALL_SCM = True
        elif name in ("--cleanscm"):
            IS_CLEAN_SCM = True
        elif name in ("--cleanws"):
            IS_CLEAN_WS = True
        elif name in ("--runtest"):
            IS_RUN_TEST = True
        elif name in ("--host"):
            HOST_LIST = value
        elif name in ("--force"):
            IS_FORCE = True

def compileScm():
    cmdExecutor.command("python "+ BIN_DIR + "compile.py --package-path " + TEMP_DIR)

def installSdb():
    runPath = TEMP_DIR + 'sequoiadb-*-installer.run'
    localRunPath = glob.glob(runPath)
    if int(len(localRunPath)) == 0:
        raise Exception("Missing SDB installation package")
    if len(HOST_LIST.strip()) == 0:
        raise Exception("Missing hostname!")
    HostArr = HOST_LIST.split(",")
    cfgPath = CONF_DIR + "deploysdb1Host2Cluster_template.cfg"
    cmd = "python "+ BIN_DIR +"deploy_sdb.py --package-file " + localRunPath[0] + " --host " + str(HostArr[0]) + " --template " + cfgPath + " --output " + SDB_INFO_FILE + " --ssh-file " +CONF_FILE
    if IS_FORCE:
        cmdExecutor.command(cmd + " --force")
    else:
        cmdExecutor.command(cmd)


def installScm():
    if len(HOST_LIST.strip()) == 0:
        raise Exception("Missing hostname!")
    scmHostArr = HOST_LIST.split(",")
    cfgPath = CONF_DIR + "deployscm" + str(len(scmHostArr))+ "Host_template.cfg"
    tarPath = glob.glob(TEMP_DIR + 'sequoiacm-*-release.tar.gz')
    if int(len(tarPath)) == 0 or int(len(tarPath)) != 1:
        raise Exception("Missing SCM installation package or more than two SCM installation package!")
    tarPathStr = str(tarPath[0]).replace(' ', '\ ').replace('(', '\(').replace(')', '\)')
    cmd = "python "+ BIN_DIR +"deploy_scm.py --package-file " + tarPathStr + " --host " + str(HOST_LIST) + " --template " + cfgPath + " --sdb-info " + SDB_INFO_FILE + " --output " + SCM_INFO_FILE + " --ssh-file " + CONF_FILE
    if IS_FORCE:
        cmdExecutor.command(cmd + " --force")
    else:
        cmdExecutor.command(cmd)

def cleanScm():
    if len(HOST_LIST.strip()) == 0:
        raise Exception("Missing hostname!")
    cmd = "python " + BIN_DIR +"clean_scm.py --host " + str(HOST_LIST)  + " --ssh-file " + CONF_FILE
    cmdExecutor.command(cmd)

def cleanWs():
    if not os.path.exists(SCM_INFO_FILE) or len(SCM_INFO_FILE.strip()) == 0 :
        raise Exception("Missing scm.info or scm info path is not exists! ")
    cmd = "python " + BIN_DIR +"clean_ws.py --scm-info " + str(SCM_INFO_FILE) + " --workspace-file " + str(WORKSPACE_FILE)
    cmdExecutor.command(cmd)

def runTest():
    if not os.path.exists(SCM_INFO_FILE):
        raise Exception("Missing scm.info!")
    cmd = "python "+ BIN_DIR +"run_test.py --scm-info " + SCM_INFO_FILE + " --workspace-file " + WORKSPACE_FILE
    cmdExecutor.command(cmd)


if __name__ == '__main__':
    modules = []
    costs = []
    total = 0
    try:
        parse_command()
        doSomething = False
        if COMPILE:
            start_time = datetime.datetime.now()
            compileScm()
            end_time = datetime.datetime.now()
            modules.append('compile')
            costs.append(int((end_time - start_time).seconds))
            total += int((end_time - start_time).seconds)
            doSomething = True

        if IS_INSTALL_SDB:
            start_time = datetime.datetime.now()
            installSdb()
            end_time = datetime.datetime.now()
            modules.append('installsdb')
            costs.append(int((end_time - start_time).seconds))
            total += int((end_time - start_time).seconds)
            doSomething = True

        if IS_INSTALL_SCM:
            start_time = datetime.datetime.now()
            installScm()
            end_time = datetime.datetime.now()
            modules.append('installscm')
            costs.append(int((end_time - start_time).seconds))
            total += int((end_time - start_time).seconds)
            doSomething = True

        if IS_CLEAN_SCM:
            start_time = datetime.datetime.now()
            cleanScm()
            end_time = datetime.datetime.now()
            modules.append('cleanscm')
            costs.append(int((end_time - start_time).seconds))
            total += int((end_time - start_time).seconds)
            doSomething = True

        if IS_CLEAN_WS:
            start_time = datetime.datetime.now()
            cleanWs()
            end_time = datetime.datetime.now()
            modules.append('cleanws')
            costs.append(int((end_time - start_time).seconds))
            total += int((end_time - start_time).seconds)
            doSomething = True

        if IS_RUN_TEST:
            start_time = datetime.datetime.now()
            runTest()
            end_time = datetime.datetime.now()
            modules.append('runtest')
            costs.append(int((end_time - start_time).seconds))
            total += int((end_time - start_time).seconds)
            doSomething = True

        if doSomething == False:
            display(0)
        else:
            print("==============================COST SUMMARY:======================================")
            count = 0
            while count < int(len(modules)):
                print "modules:", modules[count],"   costs:",costs[count], "S"
                count += 1
            print "Total:  ", total, "S"
            print("=================================================================================")
    except Exception as e:
        print(e)
        sys.exit(1)

