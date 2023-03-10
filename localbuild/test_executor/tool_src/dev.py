#!/usr/bin/python
import sys, getopt
import os

rootDir = sys.path[0] + os.sep
srcDir = rootDir + "src" + os.sep
packageCommand = "mvn clean package"

def package(path):
    ret = os.system(packageCommand)
    if ret != 0:
        print("failed to run cmd, project path:" + path)
        sys.exit(1)

if __name__ == '__main__':
    os.chdir(srcDir)
    package(srcDir)