#!/usr/bin/python
import sys, getopt
import os
import shutil
import glob
rootDir = sys.path[0]+os.sep
sys.path.append(rootDir)
import scmCmdExecutor
from scmCmdExecutor import ScmCmdExecutor
cmdExecutor = ScmCmdExecutor(False)

PACKAGE_PATH = ""

def display(exit_code):
    print("")
    print(" --help | -h                  : print help message")
    print(" --package-path  <arg>        : SCM installation package path")
    sys.exit(exit_code)

def parse_command():
    global PACKAGE_PATH
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc:at:", ["help", "package-path="])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--package-path"):
            PACKAGE_PATH = value

def compileScm(packagePath):
    try:
        tarStr = rootDir + ".." + os.sep  + ".." + os.sep + "archive-target" + os.sep + "sequoiacm-*-release.tar.gz"
        filesTarget = glob.glob(tarStr)
        for file in filesTarget:
            os.remove(file)
        filesTar = glob.glob(packagePath + os.sep + "sequoiacm-*-release.tar.gz")
        for file in filesTar:
            os.remove(file)
        res = cmdExecutor.command("python " + rootDir + ".." + os.sep  + ".." + os.sep + "dev.py --compile all --archive")
        if res != 0:
             raise Exception("*************************Fail to Compile*************************************")
        files = glob.glob(tarStr)
        if int(len(files)) != 1:
            raise Exception("SCM installation package is not exists !")
        shutil.copy(files[0], packagePath)
    except Exception as e:
        print(e)
        raise e


if __name__ == '__main__':
    try:
        parse_command()
        print("------------------------------start compile---------------------------------------------------------")
        if not os.path.exists(PACKAGE_PATH) or len(PACKAGE_PATH.strip())== 0 :
            raise Exception("Package path is not exists !")
        compileScm(PACKAGE_PATH)
    except Exception as e:
        print(e)
        raise e









