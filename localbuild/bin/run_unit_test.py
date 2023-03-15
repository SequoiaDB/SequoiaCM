#!/usr/bin/python
# -*- coding:utf-8 -*-
import getopt
import os
import re
import sys
import xml.etree.ElementTree as ET

rootDir = sys.path[0] + os.sep
sys.path.append(rootDir + "localbuild" + os.sep + "bin")
from scmCmdExecutor import ScmCmdExecutor
from logUtil import Logging

SCM_SRC_DIR = rootDir + '..' + os.sep + '..' + os.sep + 'src'
TMP_DIR = rootDir + '..' + os.sep + 'tmp' + os.sep
LOG_PATH = TMP_DIR + 'run_ut.log'
SCM_INFO_FILE = ""
SCM_UT_INFO_FILE = ""
cmdExecutor = ScmCmdExecutor(False)
log = Logging(LOG_PATH).get_logger()


def display(exit_code):
    print("")
    print(" --help | -h                  : print help message")
    print(" --scm-info       <arg>      : SCM information:scm.info")
    print(" --output         <arg>       : dir output with SCM information file")
    sys.exit(exit_code)


def parse_command():
    global SCM_INFO_FILE, SCM_UT_INFO_FILE
    try:
        options, args = getopt.getopt(sys.argv[1:], "h",
                                      ["help", "scm-info=", "output="])
    except getopt.GetoptError as ex:
        log.error(ex, exc_info=True)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in "--scm-info":
            SCM_INFO_FILE = value
        elif name in "--output":
            SCM_UT_INFO_FILE = value


def getTestModule(scmSrcDir):
    log.info("scan SCM module with unit test case...")
    testModules = []
    for root, dirs, files in os.walk(scmSrcDir):
        if 'pom.xml' in files:
            testDir = os.path.join(root, 'src', 'test')
            if os.path.isdir(testDir):
                pomFile = root + os.sep + 'pom.xml'
                pomTree = ET.parse(pomFile)
                moduleName = pomTree.find('{http://maven.apache.org/POM/4.0.0}artifactId').text
                modulePath = os.path.abspath(root) + os.sep
                testModules.append({'moduleName': moduleName, 'modulePath': modulePath})
    return testModules


# 持久化 scm_ut.info 文件（哪些模块执行了单元测试，后面做覆盖率统计需要收集数据）
def generateScmUtInfoFile(scmUtInfoFile, testModules):
    with open(scmUtInfoFile, 'w') as f:
        f.write('moduleName,modulePath\n')
        for module in testModules:
            f.write('{},{}\n'.format(module['moduleName'], module['modulePath']))


def parseUnitTestParam(scmInfoFile):
    testParam = ""
    if scmInfoFile != "" and os.path.exists(scmInfoFile):
        with open(scmInfoFile) as lines:
            for line in lines:
                line = line.strip()
                if line.startswith("mainSdbUrl="):
                    testParam += ' -DMAINSDBURL=' + line.replace("mainSdbUrl=", "")
                if line.startswith("sdbuser="):
                    testParam += ' -DSDBUSER=' + line.replace("sdbuser=", "")
                if line.startswith("sdbpassword="):
                    testParam += ' -DSDBPASSWD=' + line.replace("sdbpassword=", "")
    return testParam


def runUnitTest(scmSrcDir, scmInfoFile, scmUtInfoFile):
    # 扫描需要执行单元测试的模块
    testModules = getTestModule(scmSrcDir)
    if len(testModules) <= 0:
        log.warn("No unit test module was found.")
        return
    generateScmUtInfoFile(scmUtInfoFile, testModules)

    # 基于 scm.info 文件生成测试参数 -DMAINSDBURL=XXX -D...（部分单元测试需要连上 SDB 环境执行）
    unitTestParam = parseUnitTestParam(scmInfoFile)

    # 执行单元测试
    print("start running SCM unit testcase...")
    print("======================================UNIT TEST RESULT==============================================")
    for testModule in testModules:
        moduleName = testModule['moduleName']
        modulePath = testModule['modulePath']
        pomFile = modulePath + 'pom.xml'
        targetDir = modulePath + 'target' + os.sep
        consoleOutFile = targetDir + 'result.txt'
        reportDir = targetDir + 'surefire-reports'
        testCmd = 'mvn -f ' + pomFile + ' test -Dtestngxml=src/test/resources/testng.xml ' + unitTestParam + ' > ' + consoleOutFile + ' 2>&1'
        cmdExecutor.command(testCmd, False, False)
        # 解析用例输出，打印执行结果
        with open(consoleOutFile) as f:
            for line in f:
                pattern = r'.*Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)'
                match = re.search(pattern, line)
                if match:
                    total = int(match.group(1))
                    fail = int(match.group(2)) + int(match.group(3))
                    skip = int(match.group(4))
                    success = total - fail - skip
                    print(moduleName + ' | total: {}, pass: {}, fail: {}, skip: {}, details:{}'.format(total, success, fail, skip, reportDir))
                    break


if __name__ == '__main__':
    try:
        parse_command()
        if os.path.exists(SCM_UT_INFO_FILE):
            os.remove(SCM_UT_INFO_FILE)
        if len(SCM_INFO_FILE.strip()) > 0 and not os.path.exists(SCM_INFO_FILE):
            raise Exception("scm info file not exist!")
        runUnitTest(SCM_SRC_DIR, SCM_INFO_FILE, SCM_UT_INFO_FILE)
    except Exception as e:
        log.error(e, exc_info=True)
        raise e
