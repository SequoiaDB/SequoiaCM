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

##modify test pom.xml
oldDriverValue = "<scmdriver.version>" + oldVersion + "</scmdriver.version>"
newDriverValue = "<scmdriver.version>" + newVersion + "</scmdriver.version>"
# modify test pom.xml
testRootDir = GIT_ROOT_PATH + os.sep + "testcases" + os.sep + "v2.0"
# base
basePom = testRootDir + os.sep + "testcase-base" + os.sep + "pom.xml"
modifyFile(basePom, oldDriverValue, newDriverValue)

# doc/config/version.json
docVersion = GIT_ROOT_PATH + os.sep + "doc" + os.sep + "config" + os.sep + "version.json"
# version format like 2.3.1 ,we need major version 2,minor version 3
firstPointPos = newVersion.index(".")
newMajor = newVersion[0:firstPointPos]
newMinor = newVersion[firstPointPos + 1:]
firstPointPos = newMinor.index(".")
newMinor = newMinor[0:firstPointPos]

firstPointPos = oldVersion.index(".")
oldMajor = oldVersion[0:firstPointPos]
oldMinor = oldVersion[firstPointPos + 1:]
firstPointPos = oldMinor.index(".")
oldMinor = oldMinor[0:firstPointPos]

oldMajorStr = '"major": ' + oldMajor
oldMinorStr = '"minor": ' + oldMinor
newMajorStr = '"major": ' + newMajor
newMinorStr = '"minor": ' + newMinor

modifyFile(docVersion, oldMajorStr, newMajorStr)
modifyFile(docVersion, oldMinorStr, newMinorStr)
