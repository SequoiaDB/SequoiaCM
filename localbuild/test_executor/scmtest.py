#!/usr/bin/python
import sys, getopt
import os
rootDir = sys.path[0] + os.sep

def run():
    args = " ".join(sys.argv[1:])

    jvmOption = "-Dcommon.basePath=" + rootDir

    jarParent = rootDir + "jars" + os.sep
    jarFile = None
    files = os.listdir(jarParent)
    for file in files:
        if os.path.isfile(jarParent + file):
            if file.startswith("sequoiacm-test-executor-tool") and file.endswith(".jar"):
                jarFile = file
    if jarFile == None:
        print("failed to find sequoiacm-test-executor-tool jar in " + jarParent)
        sys.exit(1)

    jarPath = jarParent + jarFile

    cmd = "java " + jvmOption + " -jar " + jarPath + " " + args

    command(cmd)

def command(cmd):
    ret = os.system(cmd)
    if ret != 0:
        print("failed to run cmd")
        sys.exit(1)

if __name__ == '__main__':
    run()