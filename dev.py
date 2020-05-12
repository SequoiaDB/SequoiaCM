#!/usr/bin/python
import sys, getopt
import os
import tarfile
import shutil
rootDir = sys.path[0]+os.sep
sys.path.append(rootDir + "script" + os.sep + "dev")
import  scmProjectUtil 
import  scmCmdExecutor
from scmCmdExecutor import ScmCmdExecutor
from scmProjectUtil import ScmProjectUtil

cmdExecutor = ScmCmdExecutor(False)
scmProjectUtil = ScmProjectUtil(rootDir + "script" + os.sep + "dev" + os.sep + "projectInfo.json", cmdExecutor)

compileScmModule = None
needArchiveRelease = False


def getScmVersion():
    versionFile = rootDir + "script" + os.sep + "dev" + os.sep + "version"
    with open(versionFile) as lines:
        for line in lines:
            line = line.strip()
            if line.startswith("NewVersion="):
                return line.replace("NewVersion=", "")
    print ("Error:failed to get scm version from " + versionFile)
    sys.exit(-1)

archiveReleasePath = rootDir + "archive-target" + os.sep + 'sequoiacm-' + getScmVersion() + '-release.tar.gz'

compileTestModule = None
runTestModule = None
compileRelease = False


def display(exit_code):
    print("config file: " + rootDir + "script" + os.sep + "dev" + os.sep + "projectInfo.json")
    print("")
    print(" --help | -h       : print help message")
    print(" --compile | -c    : option value: all " + " ".join(scmProjectUtil.getScmModules()))
    print(" --release         : compile release sequoiacm")
    print(" --compileTest     : option value: all " + " ".join(scmProjectUtil.getTestModules()))
    print(" --runTest         : run test by testng.xml, test module: all " + " ".join(scmProjectUtil.getTestModules()))
    print(" --archive | -a    : archive scm project, package a tar.gz file")
    print(" --tar-path | -t   : specify package scm target file path, default "+ archiveReleasePath)
    sys.exit(exit_code)

def parse_command():
    global compileScmModule, needArchiveRelease, archiveReleasePath, compileTestModule, runTestModule, compileRelease
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc:at:", ["help", "compile=", "release", "archive", "tar-path=", "compileTest=", "runTest="])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(-1)
        
    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("-c","--compile"):
            compileScmModule = value
        elif name in ("-a","--archive"):
            needArchiveRelease = True;
        elif name in ("-t","--tar-path"):
            archiveReleasePath = value 
        elif name in ("--compileTest"):
            compileTestModule = value
        elif name in ("--runTest"):
            runTestModule = value
        elif name in ("--release"):
            compileRelease = True

            
def compileScm():
    compileArgs = ""
    if compileRelease:
        compileArgs = "-DpackageType=release"
    
    if compileScmModule == "all":
        scmProjectUtil.compileScm(compileArgs)
    else:
        scmProjectUtil.compileScmModule(compileScmModule, compileArgs)
        
def compileTest():
    if compileTestModule == "all":
        scmProjectUtil.compileTest()
    else:
        scmProjectUtil.compileTestModule(compileTestModule)

def archive():
    scmProjectUtil.packageScm(archiveReleasePath)

def untar(filePath,dir):
    try:
        t = tarfile.open(filePath)
        t.extractall(dir)
    except Exception as e:
        print(e)

def runTest():
    if runTestModule == "all":
        scmProjectUtil.runAllTest()
    else:
        scmProjectUtil.runTest(runTestModule)

if __name__ == '__main__':
    parse_command()
    doSomething = False
    if compileScmModule != None:
        compileScm()
        doSomething = True
        
    if compileTestModule != None:
        compileTest()
        doSomething = True
        
    if runTestModule != None:
        runTest()
        doSomething = True
        
    if needArchiveRelease:
        archive()
        doSomething = True
    
    if doSomething == False:
        display(0)


