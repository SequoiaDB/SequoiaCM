#!/usr/bin/python
# -*- coding:utf-8 -*-
import getopt
import glob
import os
import shutil
import sys
from logUtil import Logging
from scmCmdExecutor import ScmCmdExecutor
rootDir = sys.path[0] + os.sep
sys.path.append(rootDir)
cmdExecutor = ScmCmdExecutor(False)
LOG_PATH = rootDir + '..' + os.sep + 'tmp' + os.sep + 'compile.log'
log = Logging(LOG_PATH).get_logger()
PACKAGE_PATH = ""


def display(exit_code):
    print("")
    print(" --help | -h                  : print help message")
    print(" --package-path  <arg>        : SCM installation package path")
    sys.exit(exit_code)


def parse_command():
    global PACKAGE_PATH
    try:
        options, args = getopt.getopt(sys.argv[1:], "h", ["help", "package-path="])
    except getopt.GetoptError, e:
        log.error(e, exc_info=True)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--package-path"):
            PACKAGE_PATH = value


def compileScm():
    tarStr = rootDir + ".." + os.sep + ".." + os.sep + "archive-target" + os.sep + "sequoiacm-*-release.tar.gz"
    installStr = PACKAGE_PATH + os.sep + "sequoiacm-*-release.tar.gz"
    cmdStr = "python " + rootDir + ".." + os.sep + ".." + os.sep + "dev.py --compile all --archive"
    try:
        # 删除项目archive-target下tar.gz安装包
        filesTarget = glob.glob(tarStr)
        for file1 in filesTarget:
            os.remove(file1)
            log.info("delete file:" + file1)
        # 删除安装安装目录下的tar.gz安装包
        filesTar = glob.glob(installStr)
        for file2 in filesTar:
            os.remove(file2)
            log.info("delete file:" + file2)
        # 执行编译
        log.info("exec cmd:" + cmdStr)
        res = cmdExecutor.command(cmdStr)
        if res != 0:
            raise Exception("Failed to Compile")
        # 复制安装包到指定位置
        files = glob.glob(tarStr)
        if int(len(files)) != 1:
            raise Exception("SCM installation package is not exists !")
        shutil.copy(files[0], PACKAGE_PATH)
        log.info("copy file:" + PACKAGE_PATH)
    except Exception as ex:
        raise ex


if __name__ == '__main__':
    try:
        parse_command()
        log.info("start compile")
        if not os.path.exists(PACKAGE_PATH) or len(PACKAGE_PATH.strip()) == 0:
            raise Exception("Package path is not exists, path=" + PACKAGE_PATH)
        compileScm()
    except Exception as e:
        log.error(e, exc_info=True)
        raise e
