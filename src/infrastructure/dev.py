#!/usr/bin/python
import json
import sys, getopt
import os
import platform
import commands

NEED_COMPILE = 1
NEED_INSTALLZK = 0

rootDir = sys.path[0]
PROJECT_PATH = rootDir + os.sep + "project"

def display_info(msg):
    print("========================================================================")
    print(msg)
    print("========================================================================")


def displayAndExit(exit_code):
    print(" --help | -h       : print help message")
    print(" --nocompile       : do not compile all")
    print(" --installzk       : install zookeeper")
    sys.exit(exit_code)

def parse_command():
    global NEED_COMPILE, NEED_INSTALLZK
    try:
        options, args = getopt.getopt(sys.argv[1:], "h", ["help", "nocompile", "installzk"])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            displayAndExit(0)
        elif name == '--nocompile':
            NEED_COMPILE = 0
        elif name == '--installzk':
            NEED_INSTALLZK = 1

def execCMD(cmd):
    print "INFO:" + cmd
    ret = os.system(cmd)
    if ret != 0:
        sys.exit(ret)

def compile_all():
    ret = os.system("mvn clean install -f " + rootDir + "/project/pom.xml -Dmaven.test.skip=true")
    if ret != 0:
        display_info("compile failed")
        sys.exit(1)

def installzk():
    display_info("Begin install zookeeper")
    ret = 0
    ret = os.system("python deploy_zk.py")

    if ret == 0:
        display_info("install zookeeper Succeed")
    else:
        display_info("install zookeeper Failed")
        sys.exit(1)

if __name__ == '__main__':
    parse_command()
    if NEED_COMPILE == 1:
        compile_all()
    if NEED_INSTALLZK == 1:
        installzk()

