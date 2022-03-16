#!/usr/bin/python

import commands
import os
import sys
import shutil
import fnmatch

TOOLS_PATH = sys.path[0]

GIT_ROOT_PATH = TOOLS_PATH + os.sep + ".." + os.sep + ".." 
superParentPom = GIT_ROOT_PATH + os.sep + "src" + os.sep +"pom.xml"
infrastructureParentPom = GIT_ROOT_PATH + os.sep + "src" + os.sep + "infrastructure" + os.sep + "project" + os.sep + "pom.xml"
genVerScriptPath = GIT_ROOT_PATH + os.sep + 'script' + os.sep +  'dev' + os.sep + "genVer.py"
javaDriverPath = GIT_ROOT_PATH + os.sep + 'driver' + os.sep + 'java'

def modifyFile(file, oldValue, newValue) :
   with open(file, "r") as f:
      lines = f.readlines()
     
   with open(file, "w") as f_w:
      for line in lines:
         if oldValue in line:
            line=line.replace(oldValue, newValue)
         f_w.write(line)

def execCMD(cmd):
    ret = os.system(cmd)
    if ret != 0:
        print 'exec cmd failed:' + cmd
        sys.exit(-1)

execCMD('mvn clean -f ' + superParentPom)
execCMD('python ' + genVerScriptPath)

#install project before javadoc
execCMD("mvn install -Dmaven.test.skip=true -DskipWebCompile=true -f " + superParentPom)

os.chdir(TOOLS_PATH)

execCMD('mvn javadoc:javadoc -f ' + javaDriverPath + os.sep + 'pom.xml')

apidocs=javaDriverPath + os.sep + 'target' + os.sep + 'site' + os.sep + 'apidocs'
apiDir = TOOLS_PATH + os.sep + '..' + os.sep + 'build' + os.sep + 'output' + os.sep + 'api' + os.sep + "java"
html= apiDir + os.sep + 'html'

# copy apidocs
if os.path.exists(apiDir):
    shutil.rmtree(apiDir)
os.makedirs(apiDir)
shutil.copytree(apidocs, html)

if os.path.exists(apiDir) == False :
   print 'path ' + apiDir + ' is not exist'
   sys.exit(-1);

oldValue='content="text/html" charset="UTF-8"'
newValue='content="text/html;charset=UTF-8"'

print 'start to replace charset'
print 'apiDir=' + apiDir
print 'oldValue=' + oldValue
print 'newValue=' + newValue

for root, dirs, files in os.walk(apiDir) :
   for filename in files:
      if fnmatch.fnmatch(filename, "*html"):
         fullFile = os.path.join(root, filename)
         modifyFile(fullFile, oldValue, newValue);
