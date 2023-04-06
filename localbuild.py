#!/usr/bin/python
# -*- coding:utf-8 -*-
import sys, getopt
import os
import glob
import datetime
rootDir = sys.path[0]+os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
from scmCmdExecutor import ScmCmdExecutor
from logUtil import Logging
LOG_PATH = rootDir + 'localbuild' + os.sep + 'tmp' + os.sep + 'localbuild.log'
log = Logging(LOG_PATH).get_logger()

BIN_DIR = sys.path[0] + os.sep + 'localbuild' + os.sep + 'bin' + os.sep
CONF_DIR = sys.path[0] + os.sep + 'localbuild' + os.sep + 'conf' + os.sep
TMP_DIR = sys.path[0] + os.sep + 'localbuild' + os.sep + 'tmp' + os.sep

COMPILE = False
IS_INSTALL_SDB = False
IS_INSTALL_SCM = False
IS_RUN_TEST = False
IS_RUN_UNIT_TEST = False
IS_STATISTICAL_COV = False
IS_FORCE = False
IS_CLEAN_SCM = False
IS_CLEAN_WS = False
HOST_LIST = ""
SITE = "twoSite"
PROJECT = "all"
RUN_BASE = False

cmdExecutor = ScmCmdExecutor(False)

CONF_FILE = CONF_DIR + 'localbuild.conf'
WORKSPACE_FILE = CONF_DIR + 'workspace_template.json'
SDB_INFO_FILE = TMP_DIR + 'sdb.info'
SCM_INFO_FILE = TMP_DIR + 'scm.info'
SCM_DEPLOY_INFO_FILE = TMP_DIR + 'scm_deploy.info'
SCM_UT_INFO_FILE = TMP_DIR + 'scm_ut.info'
TMP_STATISTICAL_COV_DIR = TMP_DIR + 'statistical_cov' + os.sep


def display(exit_code):
    print(" --help | -h       : print help message")
    print(" --compile | -c    : compile ")
    print(" --installsdb      : install SDB cluster ")
    print(" --installscm      : install SCM cluster ")
    print(" --cleanscm        : clean SCM cluster ")
    print(" --runtest         : execute basic test cases")
    print(" --runut           : execute unit test cases")
    print(" --statistical-cov : execute statistical coverage rate")
    print(" --host <arg>      : hostname : required, ',' separate ")
    print(" --force           : force to install SCM or SDB ")
    print(" --site <arg>      : the number of site: twoSite, fourSite ")
    print(" --project <arg>   : test project: tdd, story, sdv, all  ")
    print(" --runbase         : test base case, can be used with project,site ")
    sys.exit(exit_code)


def parse_command():
    global IS_INSTALL_SCM, IS_INSTALL_SDB, COMPILE, IS_RUN_TEST, IS_RUN_UNIT_TEST, IS_STATISTICAL_COV, HOST_LIST, IS_FORCE, IS_CLEAN_SCM, IS_CLEAN_WS, SITE, PROJECT, RUN_BASE
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc", ["help", "compile", "installsdb", "installscm", "cleanscm", "runtest", "runut", "statistical-cov", "host=", "force", "cleanws", "site=", "project=", "runbase"])
    except getopt.GetoptError, e:
        log.error("Error:", e)
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
        elif name in ("--runut"):
            IS_RUN_UNIT_TEST = True
        elif name in ("--statistical-cov"):
            IS_STATISTICAL_COV = True
        elif name in ("--host"):
            HOST_LIST = value
        elif name in ("--force"):
            IS_FORCE = True
        elif name in ("--site"):
            SITE = value
        elif name in ("--project"):
            PROJECT = value
        elif name in ("--runbase"):
            RUN_BASE = True


def compileScm():
    cmdStr = "python " + BIN_DIR + "compile.py --package-path " + TMP_DIR
    log.info('exec compile:' + cmdStr)
    cmdExecutor.command(cmdStr)


def installSdb():
    runPath = TMP_DIR + 'sequoiadb-*-installer.run'
    localRunPath = glob.glob(runPath)
    if int(len(localRunPath)) == 0:
        raise Exception("Missing SDB installation package")
    if len(HOST_LIST.strip()) == 0:
        raise Exception("Missing hostname!")
    HostArr = HOST_LIST.split(",")
    if SITE == "twoSite":
        cfgPath = CONF_DIR + "deploysdb1Host2Cluster_template.cfg"
    elif SITE == "fourSite":
        cfgPath = CONF_DIR + "deploysdb1Host4Cluster_template_full.cfg"
    else:
        raise Exception("The parameter of site is invalid !")
    cmd = "python "+ BIN_DIR +"deploy_sdb.py --package-file " + localRunPath[0] + " --host " + str(HostArr[0]) + " --template " + cfgPath + " --output " + SDB_INFO_FILE + " --ssh-file " + CONF_FILE
    if IS_FORCE:
        log.info('exec installsdb: ' + cmd + " --force")
        cmdExecutor.command(cmd + " --force")
    else:
        log.info('exec installsdb: ' + cmd )
        cmdExecutor.command(cmd)


def installScm():
    if len(HOST_LIST.strip()) == 0:
        raise Exception("Missing hostname!")
    scmHostArr = HOST_LIST.split(",")
    if SITE == "twoSite":
        cfgPath = CONF_DIR + "deployscm" + str(len(scmHostArr))+ "Host_template.cfg"
    elif SITE == "fourSite":
        cfgPath = CONF_DIR + "deployscm" + str(len(scmHostArr))+ "Host_template_full.cfg"
    else:
        raise Exception("The parameter of site is invalid !")
    tarPath = glob.glob(TMP_DIR + 'sequoiacm-*-release.tar.gz')
    if int(len(tarPath)) == 0 or int(len(tarPath)) != 1:
        raise Exception("Missing SCM installation package or more than two SCM installation package!")
    tarPathStr = str(tarPath[0]).replace(' ', '\ ').replace('(', '\(').replace(')', '\)')
    cmd = "python " + BIN_DIR +"deploy_scm.py --package-file " + tarPathStr + " --host " + str(HOST_LIST) + " --template " + cfgPath + " --sdb-info " + SDB_INFO_FILE + " --output " + TMP_DIR + " --ssh-file " + CONF_FILE
    if IS_FORCE:
        log.info('exec installscm: ' + cmd + " --force")
        cmdExecutor.command(cmd + " --force")
    else:
        log.info('exec installscm: ' + cmd + " --force")
        cmdExecutor.command(cmd)


def cleanScm():
    if len(HOST_LIST.strip()) == 0:
        raise Exception("Missing hostname!")
    cmd = "python " + BIN_DIR + "clean_scm.py --host " + str(HOST_LIST)  + " --ssh-file " + CONF_FILE
    log.info('exec cleanscm: ' + cmd )
    cmdExecutor.command(cmd)


def cleanWs():
    if not os.path.exists(SCM_INFO_FILE) or len(SCM_INFO_FILE.strip()) == 0 :
        raise Exception("Missing scm.info or scm info path is not exists! ")
    cmd = "python " + BIN_DIR +"clean_ws.py --scm-info " + str(SCM_INFO_FILE) + " --workspace-file " + str(WORKSPACE_FILE)
    log.info('exec cleanws:' + cmd )
    cmdExecutor.command(cmd)


def runTest():
    if len(HOST_LIST.strip()) == 0:
        raise Exception("Missing hostname !")
    if PROJECT != "tdd" and PROJECT != "story" and PROJECT != "sdv" and PROJECT != "all":
        raise Exception("The parameter of project is invalid !")
    if SITE != "oneSite" and SITE != "twoSite" and SITE != "fourSite":
        raise Exception("The parameter of site is invalid !")
    cmdStr = "python " + BIN_DIR + "run_test.py --scm-info " + SCM_INFO_FILE + " --ssh-file " + CONF_FILE + " --host " + str(HOST_LIST) + " --project " + PROJECT + " --site " + SITE
    log.info('exec runtest: ' + cmdStr )
    if RUN_BASE:
        cmdExecutor.command(cmdStr + " --runbase")
    else:
        cmdExecutor.command(cmdStr)


def runUnitTest():
    cmdStr = "python " + BIN_DIR + "run_unit_test.py --scm-info " + SCM_INFO_FILE + " --output " + SCM_UT_INFO_FILE
    log.info('exec runut: ' + cmdStr)
    cmdExecutor.command(cmdStr)


def statisticalCov():
    cmdStr = "python " + BIN_DIR + "statistical_cov.py --work-path " + TMP_STATISTICAL_COV_DIR
    if os.path.exists(SCM_DEPLOY_INFO_FILE):
        cmdStr += " --scm-deploy-info " + SCM_DEPLOY_INFO_FILE
    if os.path.exists(SCM_UT_INFO_FILE):
        cmdStr += " --scm-ut-info " + SCM_UT_INFO_FILE
    log.info('exec statistical-cov: ' + cmdStr)
    cmdExecutor.command(cmdStr)


def formatTime(sum):
    hour = sum / 3600
    min = (sum % 3600) / 60
    sec = sum % 60
    costs = "costs: "
    if hour > 0:
        costs = costs + str(hour) + " h "
    if min > 0:
        costs = costs + str(min) + " min "
    if sec > 0:
        costs = costs + str(sec) + " s "
    return costs


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

        if IS_RUN_UNIT_TEST:
            start_time = datetime.datetime.now()
            runUnitTest()
            end_time = datetime.datetime.now()
            modules.append('runut')
            costs.append(int((end_time - start_time).seconds))
            total += int((end_time - start_time).seconds)
            doSomething = True

        if IS_STATISTICAL_COV:
            start_time = datetime.datetime.now()
            statisticalCov()
            end_time = datetime.datetime.now()
            modules.append('statistical-cov')
            costs.append(int((end_time - start_time).seconds))
            total += int((end_time - start_time).seconds)
            doSomething = True

        if not doSomething:
            display(0)
        else:
            print("==============================COST SUMMARY:======================================")
            count = 0
            while count < int(len(modules)):
                print "modules:", modules[count], formatTime(int(costs[count]))
                count += 1
            print "Total:  ", formatTime(total)
            print("=================================================================================")
    except Exception as e:
        log.error(e)
        sys.exit(1)

