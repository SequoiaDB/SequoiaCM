#!/usr/bin/python
# -*- coding:utf-8 -*-
import getopt
import os
import csv
import shutil
import sys
import tarfile

rootDir = sys.path[0] + os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
from scmCmdExecutor import ScmCmdExecutor
from logUtil import Logging

LIB_DIR = rootDir + '..' + os.sep + 'lib' + os.sep
JACOCO_LIB_DIR = LIB_DIR + 'jacoco' + os.sep
JACOCO_CLI_PATH = JACOCO_LIB_DIR + 'jacococli.jar'

SCM_SRC_DIR = rootDir + '..' + os.sep + '..' + os.sep + 'src'
TMP_DIR = rootDir + '..' + os.sep + 'tmp' + os.sep
LOG_PATH = TMP_DIR + 'statistical_cov.log'
cmdExecutor = ScmCmdExecutor(False)
log = Logging(LOG_PATH).get_logger()

SCM_DEPLOY_INFO_FILE = ""
SCM_UT_FILE = ""
WORK_PATH = ""
EXEC_DIR = ""
EXEC_MERGE_FILE = ""
SOURCE_DIR = ""
CLASSES_DIR = ""
REPORT_DIR = ""


def display(exit_code):
    print("")
    print(" --help | -h                  : print help message")
    print(" --work-path       <arg>      : Work path of statistical coverage rate")
    print(" --scm-deploy-info <arg>      : SCM deploy information:scm_deploy.info")
    print(" --scm-ut-info     <arg>      : SCM unit test information:scm_ut.info")
    sys.exit(exit_code)


def parse_command():
    global SCM_DEPLOY_INFO_FILE, SCM_UT_FILE, WORK_PATH, EXEC_DIR, EXEC_MERGE_FILE, CLASSES_DIR, SOURCE_DIR, REPORT_DIR
    try:
        options, args = getopt.getopt(sys.argv[1:], "h",
                                      ["help", "work-path=", "scm-deploy-info=", "scm-ut-info="])
    except getopt.GetoptError as ex:
        log.error(ex, exc_info=True)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in "--work-path":
            WORK_PATH = value + os.sep
            EXEC_DIR = WORK_PATH + 'exec' + os.sep
            EXEC_MERGE_FILE = EXEC_DIR + 'jacoco_merge.exec'
            CLASSES_DIR = WORK_PATH + 'classes' + os.sep
            SOURCE_DIR = WORK_PATH + 'source' + os.sep
            REPORT_DIR = WORK_PATH + 'report' + os.sep
        elif name in "--scm-deploy-info":
            SCM_DEPLOY_INFO_FILE = value
        elif name in "--scm-ut-info":
            SCM_UT_FILE = value


def collectScmSrcAndClasses(scmSrcDir, classesPath, sourcePath):
    log.info("scan SCM source and class file...")
    collectClassesDir = os.path.join(classesPath, 'com', 'sequoiacm')
    collectSourceDir = os.path.join(sourcePath, 'com', 'sequoiacm')
    cleanOrInitDir(collectClassesDir, collectSourceDir)

    for root, dirs, files in os.walk(scmSrcDir):
        if 'pom.xml' not in files:
            continue
        # 收集模块下的类文件
        moduleClassesDir = os.path.join(root, 'target', 'classes', 'com', 'sequoiacm')
        if os.path.exists(moduleClassesDir):
            copyDirectoryTree(moduleClassesDir, collectClassesDir)
        # 收集模块下的源代码文件
        moduleSourceDir = os.path.join(root, 'src', 'main', 'java', 'com', 'sequoiacm')
        if os.path.exists(moduleSourceDir):
            copyDirectoryTree(moduleSourceDir, collectSourceDir)


# 拷贝 sourceDir 下的所有文件/子目录到 destDir（不覆盖 destDir 已有文件）
def copyDirectoryTree(sourceDir, destDir):
    for root, dirs, files in os.walk(sourceDir):
        for _dir in dirs:
            subDir = os.path.join(root, _dir)
            destSubDir = os.path.join(destDir, os.path.relpath(subDir, sourceDir))
            if not os.path.exists(destSubDir):
                os.makedirs(destSubDir)
        for _file in files:
            srcFile = os.path.join(root, _file)
            destFile = os.path.join(destDir, os.path.relpath(srcFile, sourceDir))
            shutil.copy(srcFile, destFile)


def generateReport(workPath, execMergeFile, classesDir, sourceDir, reportDir):
    log.info("generate jacoco report...")
    # 执行生成覆盖率报告
    cmdStr = 'java -jar ' + JACOCO_CLI_PATH + ' report ' + execMergeFile + ' --html ' + reportDir + ' --classfiles ' + classesDir + ' --sourcefiles ' + sourceDir
    cmdExecutor.command(cmdStr)
    # 报告打成 tar 包
    reportTarFile = workPath + 'jacoco_report.tar'
    if os.path.exists(reportTarFile):
        shutil.rmtree(reportTarFile)
    with tarfile.open(reportTarFile, 'w') as tar:
        tar.add(reportDir, arcname='report')
    log.info("generate jacoco report success! \nDownload and see detail in: " + reportTarFile)


def dumpAndMergeExecFile(scmDeployInfoFile, scmUtFile, execDir, execMergeFile):
    # 下载集群测试覆盖率文件
    log.info("dump remote jacoco file...")
    nodeList = []
    with open(scmDeployInfoFile, 'r') as f:
        csv_reader = csv.reader(f)
        # 使用 next() 函数跳过第一行标头行
        next(csv_reader)
        for row in csv_reader:
            if not row:
                continue
            if len(row) != 4:
                raise ValueError('Invalid row: {}'.format(row))
            serviceName, hostname, port, coveragePort = row
            nodeList.append([serviceName, hostname, port, coveragePort])
    for nodeInfo in nodeList:
        hostname = nodeInfo[1]
        port = nodeInfo[2]
        coveragePort = nodeInfo[3]
        destFile = execDir + hostname + '_' + port + '.exec'
        dumpCmd = 'java -jar ' + JACOCO_CLI_PATH + ' dump --address=' + hostname + " --port=" + coveragePort + ' --destfile=' + destFile
        cmdExecutor.command(dumpCmd)

    # 拷贝单元测试覆盖率文件
    log.info("collect unittest jacoco file...")
    moduleList = []
    with open(scmUtFile, 'r') as f:
        csv_reader = csv.reader(f)
        next(csv_reader)
        for row in csv_reader:
            if not row:
                continue
            if len(row) != 2:
                raise ValueError('Invalid row: {}'.format(row))
            moduleName, modulePath = row
            moduleList.append([moduleName, modulePath])
    for moduleInfo in moduleList:
        moduleName = moduleInfo[0]
        modulePath = moduleInfo[1]
        destFile = execDir + moduleName + '.exec'
        srcFile = os.path.join(modulePath, 'target', 'jacoco.exec')
        if os.path.exists(srcFile):
            shutil.copy(srcFile, destFile)

    # 合并覆盖率文件
    log.info("merge jacoco file...")
    mergeCmd = 'java -jar ' + JACOCO_CLI_PATH + ' merge '
    for root, dirs, files in os.walk(execDir):
        for f in files:
            mergeCmd += os.path.basename(f) + ' '
    mergeCmd += ' --destfile=' + execMergeFile
    cmdExecutor.command('cd {} && {}'.format(execDir, mergeCmd))


def cleanOrInitDir(*dirs):
    for d in dirs:
        if os.path.exists(d):
            shutil.rmtree(d)
            log.debug('clean dir, path=' + os.path.abspath(d))
        else:
            os.makedirs(d)
            log.debug('create dir, path=' + os.path.abspath(d))


def statisticsCov(scmDeployInfoFile, scmUtFile, workPath, execDir, execMergeFile, classesDir, sourceDir, reportDir, scmSrcDir):
    # 清空/创建覆盖率统计所需的工作目录
    cleanOrInitDir(workPath, execDir, classesDir, sourceDir, reportDir)
    # 扫描收集 SCM 所有的类文件、源代码
    collectScmSrcAndClasses(scmSrcDir, classesDir, sourceDir)
    # 下载并合并覆盖率文件
    dumpAndMergeExecFile(scmDeployInfoFile, scmUtFile, execDir, execMergeFile)
    # 生成代码覆盖率报告
    generateReport(workPath, execMergeFile, classesDir, sourceDir, reportDir)


if __name__ == '__main__':
    try:
        parse_command()
        if len(SCM_DEPLOY_INFO_FILE.strip()) and len(SCM_UT_FILE.strip()) == 0:
            raise Exception("At least on of --scm-deploy-info and --scm-ut-info is required!")
        if len(WORK_PATH.strip()) == 0:
            raise Exception("Missing --work-path!")
        if not os.path.exists(WORK_PATH):
            os.makedirs(WORK_PATH)
        statisticsCov(SCM_DEPLOY_INFO_FILE, SCM_UT_FILE, WORK_PATH, EXEC_DIR, EXEC_MERGE_FILE, CLASSES_DIR, SOURCE_DIR, REPORT_DIR, SCM_SRC_DIR)
    except Exception as e:
        log.error(e, exc_info=True)
        raise e
