#!/usr/bin/python

import os
import shutil
import sys

GIT_ROOT_PATH = sys.path[0] + os.sep + ".." + os.sep + ".."
CONTENTSERVER_PROJECT_DIR = GIT_ROOT_PATH + os.sep + "src" + os.sep + "content-server" + os.sep + "project"
SUPER_PARENT_POM = GIT_ROOT_PATH + os.sep + "src" + os.sep + "pom.xml"
VERSION_FILE = sys.path[0] + os.sep + 'version'

OLDVERSIONPATTERN = 'OldVersion='
NEWVERSIONPATTERN = 'NewVersion='


def modifyFile(file, oldValue, newValue):
    isModified = False

    tmpfile = file + ".tmp"
    with open(file, "r") as f:
        lines = f.readlines()

    with open(tmpfile, "wb") as f_w:
        for line in lines:
            if oldValue in line:
                line = line.replace(oldValue, newValue)
                isModified = True

            f_w.write(line)

    if True == isModified:
        oldMode = os.stat(file).st_mode
        shutil.copy(tmpfile, file)
        os.chmod(file, oldMode)

    os.remove(tmpfile)


def getVersion(file, versionPattern):
    with open(file, "r") as f:
        lines = f.readlines()

    for line in lines:
        if line.find(versionPattern) != -1:
            return line[len(versionPattern):].replace("\n", "")

    print "get version from file failed:versionPattern=" + versionPattern + ",file=" + file
    sys.exit(1)

def parseVersion(version):
    lastPos = -1
    for char in version:
        if char == '.' or char.isdigit():
            lastPos += 1
        else:
            break
    return [version[0:lastPos + 1], version[lastPos + 1:]]

def parseMainVersion(versionNumber):
    firstPointPos = versionNumber.index(".")
    major = versionNumber[0:firstPointPos]
    minor = versionNumber[firstPointPos + 1:]
    firstPointPos = minor.index(".")
    minor = minor[0:firstPointPos]
    return [major, minor]

oldVersion = getVersion(VERSION_FILE, OLDVERSIONPATTERN)
newVersion = getVersion(VERSION_FILE, NEWVERSIONPATTERN)

print "Old version:" + oldVersion
print "New version:" + newVersion

# change scm project version
ret = os.system("mvn versions:set -DnewVersion=" + newVersion + " -f " + SUPER_PARENT_POM)
if ret != 0:
    print "modify Scm porject version failed"
    sys.exit(-1)
# change scm project version
ret = os.system("mvn versions:commit -f " + SUPER_PARENT_POM)
if ret != 0:
    print "commit Scm porject version failed"
    sys.exit(-1)

# doc/config/version.json
docVersion = GIT_ROOT_PATH + os.sep + "doc" + os.sep + "config" + os.sep + "version.json"
# version format like 2.3.1 ,we need major version 2,minor version 3

# newExtraInfo may be an empty string
[newVersionNumber, newExtraInfo] = parseVersion(newVersion)
[newMajor, newMinor] = parseMainVersion(newVersionNumber)

# oldExtraInfo may be an empty string
[oldVersionNumber, oldExtraInfo] = parseVersion(oldVersion)
[oldMajor, oldMinor] = parseMainVersion(oldVersionNumber)

oldMajorStr = '"major": ' + oldMajor
oldMinorStr = '"minor": ' + '"' + oldMinor + oldExtraInfo + '"'
newMajorStr = '"major": ' + newMajor
newMinorStr = '"minor": ' + '"' + newMinor + newExtraInfo + '"'

modifyFile(docVersion, oldMajorStr, newMajorStr)
modifyFile(docVersion, oldMinorStr, newMinorStr)

print "changing testcase version..."
ret = os.system("python " + GIT_ROOT_PATH + os.sep + "testcases" + os.sep + "v2.0" + os.sep + "testcase-base" + os.sep + "version.py -v " + newVersion)
if ret != 0:
    print "change testcase version failed"
    sys.exit(-1)
print "change testcase version successed"