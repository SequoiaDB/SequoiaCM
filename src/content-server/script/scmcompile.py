#!/usr/bin/python

import os
import sys
import shutil
import fnmatch
import tarfile
import platform
import commands

g_deployTarget=" -DpomFile=pom.xml -DrepositoryId=SequoiaRele -Durl=http://admin:admin123@192.168.31.204:8081/nexus/content/repositories/SequoiaRele/ "

def copyDir(src, dest):
    if platform.system() == "Linux" :
        ret = os.system("mkdir -p "+dest + os.sep)
        if ret != 0:
            print "mkdir failed:"+dest + os.sep
            sys.exit(1)
        ret = os.system("cp -r %s/* %s" % (src, dest + os.sep))
    else :
        ret = os.system("xcopy /s %s %s /y" % (src, dest + os.sep))
     
    if ret != 0:
        print 'copy failed'
        sys.exit(1)

def untarFile(tarFile, untarDir):
   t=tarfile.open(tarFile)
   t.extractall(path=untarDir)
   
def findOneFile(findDir, fileMatcher):
   for root, dirs, files in os.walk(findDir) :
     for filename in files:
       if fnmatch.fnmatch(filename, fileMatcher):
         return os.path.join(root, filename)
         
   return ""

def copyDriverToDir(truckDir,destDir):
   displayInfo("Begin to copy driver");
   
   #find tar.gz
   tarFileMatcher="sequoiacm-driver-*.tar.gz"
   tarFullFile=""
   srcDir=truckDir + os.sep + "scm" + os.sep + "driver" + os.sep + "java" + os.sep + "target"
   
   tarFullFile = findOneFile(srcDir, tarFileMatcher)
       
   if tarFullFile == "" :
       displayError("could not find driver tar file");
       print "path=" + srcDir
       print "file pattern=" + tarFileMatcher
       sys.exit(1)
   
   #untar tar.gz file
   tempDir=os.path.join(srcDir, "temp")
   shutil.rmtree(tempDir, True)
   untarFile(tarFullFile, tempDir)
   
   #copy file to target
   
   # 1. find jars
   scmDriverMatch="sequoiacm-driver*jar"
   scmCommonMatch="sequoiadb-driver*jar"
   
   scmDriverFullName = findOneFile(tempDir, scmDriverMatch)
   scmCommonFullName = findOneFile(tempDir, scmCommonMatch)
   
   imExportTarget = truckDir + os.sep + ".." + os.sep + "external-app" + os.sep + "scm-imexport" + os.sep + "project" + os.sep + "sequoiacm-imexport" + os.sep + "target";
   scmImExportFullName = findOneFile(imExportTarget, "sequoiacm-imexport-*jar")
   
   if scmDriverFullName == "" or scmCommonFullName == "" or scmImExportFullName == "" :
       displayError("could not find driver jar file")
       print "scmDriverMatch:",scmDriverMatch
       print "scmCommonMatch:",scmCommonMatch
       print "scmImExportFullName:",scmImExportFullName
       sys.exit(1)
   
   print "clear test lib:rm -rf rm -rf `ls " +  destDir + os.sep + "* | grep -v threadexecutor`"
   os.system("rm -rf `ls " +  destDir + os.sep + "* | grep -v threadexecutor`" )
   
   # 2. copy jars to target
   print "target path=" + destDir
   print "copy", scmDriverFullName
   shutil.copy(scmDriverFullName, destDir)
   
   driverLibDir = os.path.dirname(scmCommonFullName)
   print 'src=' + driverLibDir + ',' + 'dest=' + destDir
   copyDir(driverLibDir, destDir)
   
   print "copy " + scmImExportFullName + " to " + destDir
   shutil.copy(scmImExportFullName, destDir)

def displayInfo(msg) :
   print "========================================================================"
   print msg
   print "========================================================================"
   
def displayError(msg) :
   print "************************************************************************"
   print msg
   print "************************************************************************"

class Svn(object):
  @staticmethod
  def up():
    displayInfo("Begin to update all files")
    ret = 0
   
    if platform.system() == "Linux" :
      ret=os.system("svn up")
    else :
      print "[WARNING] no linux system, do not update svn"
   
    if ret == 0:
       displayInfo("update Succeed");
    else:
       displayError("update Failed");
       sys.exit(1)

class ScmCompiler(object):
  @staticmethod
  def docompile(compileDir):
    print "cd ", compileDir
    os.chdir(compileDir)
    return os.system("mvn clean compile")
       
  @staticmethod
  def doinstall(compileDir):
    print "cd ", compileDir
    os.chdir(compileDir)
    return os.system("mvn clean install -Dmaven.test.skip=true")
  
  @staticmethod  
  def dopackage(compileDir):
    print "cd ", compileDir
    os.chdir(compileDir)
    return os.system("mvn clean package -Dmaven.test.skip=true")
    
  @staticmethod
  def dodeploy(compileDir, filename):
    print "cd ", compileDir
    os.chdir(compileDir)
    cmd = "mvn deploy:deploy-file -Dfile=./target/" + filename + " " + g_deployTarget
    print cmd
    ret = os.system(cmd)
    if ret != 0 :
      displayError("dodeploy Failed:" + cmd);
      sys.exit(1)
  
  @staticmethod
  def compilecommon(truckDir):
    #Scmcommon
    displayInfo("Begin to compile Scmcommon");
    compileDir=truckDir + os.sep + "scm" + os.sep + "common"
    ret = ScmCompiler.doinstall(compileDir)
    if ret != 0 :
       displayError("compile Scmcommon Failed");
       sys.exit(1)
      
  @staticmethod
  def compiledriver(truckDir):
    #Driver
    displayInfo("Begin to install Driver");
    compileDir=truckDir + os.sep + "scm" + os.sep + "driver" + os.sep + "java"
    ret = ScmCompiler.doinstall(compileDir)
    if ret != 0 :
       displayError("Install Dirver Failed");
       sys.exit(1)
       
  @staticmethod
  def compiletools(truckDir):
    #Tools
    displayInfo("Begin to package Tools");
    compileDir=truckDir + os.sep + "scm" + os.sep + "tools"
    ret = ScmCompiler.dopackage(compileDir)
    if ret != 0 :
       displayError("Package Tools Failed");
       sys.exit(1)
       
  @staticmethod
  def compileBaseAndPackageDs(truckDir):
    displayInfo("Begin to install base");
    compileDir=truckDir + os.sep + "scm" + os.sep
    print "cd ", compileDir
    os.chdir(compileDir)
    ret = os.system("mvn clean -pl com.sequoiadb:sequoiacm-base install -Dmaven.test.skip=true -pl com.sequoiadb:sequoiacm-base")
    if ret != 0 :
       displayError("Install base Failed");
       sys.exit(1)
    
    #package datasources
    displayInfo("Begin to package ceph-s3");
    ret = os.system("mvn clean -pl com.sequoiadb:sequoiacm-ceph-s3 package -pl com.sequoiadb:sequoiacm-ceph-s3")
    if ret != 0 :
       displayError("package ceph-s3 Failed");
       sys.exit(1)
       
    displayInfo("Begin to package hbase");
    ret = os.system("mvn clean -pl com.sequoiadb:sequoiacm-hbase && mvn package -pl com.sequoiadb:sequoiacm-hbase")
    if ret != 0 :
       displayError("package hbase Failed");
       sys.exit(1)
       
    displayInfo("Begin to package ceph-swift");
    ret = os.system("mvn clean -pl com.sequoiadb:sequoiacm-ceph-swift && mvn package -pl com.sequoiadb:sequoiacm-swift")
    if ret != 0 :
       displayError("package swift Failed");
       sys.exit(1)
    
  @staticmethod
  def compileserver(truckDir):
    #Server
    displayInfo("Begin to package Server");
    compileDir=truckDir + os.sep + "scm" + os.sep + "contentserver"
    ret = ScmCompiler.dopackage(compileDir)
    if ret != 0 :
       displayError("Package Server Failed");
       sys.exit(1)
       
  @staticmethod
  def compiletdd(truckDir):
    destDir=truckDir + os.sep + "testcases" + os.sep + "v2.0" + os.sep + "testcase-base" + os.sep + "lib"
    copyDriverToDir(truckDir,destDir)
    #Tdd
    displayInfo("Begin to complie Tdd");
    compileDir=truckDir + os.sep + "testcases" + os.sep + "v2.0" + os.sep + "tdd" + os.sep + "java"
    ret = ScmCompiler.dopackage(compileDir)
    if ret != 0 :
       displayError("Compile Tdd Failed");
       sys.exit(1)
    
  @staticmethod
  def complieStoryOrSdv(truckDir,storyOrSdv):
    compileDir=truckDir + os.sep + "testcases" + os.sep + "v2.0" + os.sep + storyOrSdv + os.sep + "java"
    libDir = truckDir + os.sep + "testcases" + os.sep + "v2.0" + os.sep + "testcase-base" + os.sep + "lib"
    copyDriverToDir(truckDir,libDir)
    
    displayInfo("Begin to complie "+storyOrSdv);
    
    ret = ScmCompiler.dopackage(compileDir)
    if ret != 0 :
       displayError("Compile "+storyOrSdv+" Failed");
       sys.exit(1)
       
  @staticmethod
  def generateVersion(truckDir):
    versionBin = 'python ' + truckDir + os.sep + "scm" + os.sep + "genVer.py"
    displayInfo("Begin to generate Version");
    ret=os.system(versionBin)
    if ret != 0 :
       displayError("generate version Failed");
       sys.exit(1)
  
  @staticmethod
  def compileSCM(truckDir):
    displayInfo("Begin to Package SCM")
    basePom=truckDir + os.sep + "scm" + os.sep +"pom.xml"
    ret = os.system("mvn clean install -Dmaven.test.skip=true  -f "+basePom)
    if ret != 0:
        displayError("install base and common Failed:"+str(ret))
        sys.exit(1)

  @staticmethod  
  def compileall(truckDir):
#    ScmCompiler.generateVersion(truckDir)

#    ScmCompiler.compileBaseAndPackageDs(truckDir)
#    ScmCompiler.compilecommon(truckDir)
#    ScmCompiler.compiledriver(truckDir)
#    ScmCompiler.compiletools(truckDir)
#    ScmCompiler.compileserver(truckDir)
    ScmCompiler.compileSCM(truckDir)

    ScmCompiler.compiletdd(truckDir)
    ScmCompiler.complieStoryOrSdv(truckDir, "story")
    ScmCompiler.complieStoryOrSdv(truckDir, "sdv")
    
def main():
#    ScmCompiler.generateVersion(sys.path[0])
    ScmCompiler.compileSCM(sys.path[0])
    
if __name__ == '__main__':
    main()
    
  


