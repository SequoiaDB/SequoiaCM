from xml.dom.minidom import parse
import xml.dom.minidom
import platform
import commands
import sys
import os
import shutil
import glob
import json
import tarfile

from scmCmdExecutor import ScmCmdExecutor


class ScmProjectUtil:
    # projectInfo.json
    def __init__(self, projectInfoPath, cmdExecutor):
        self.__projectInfoPath = projectInfoPath
        self.__cmdExecutor = cmdExecutor;
        self.__parseProjectInfo(projectInfoPath)
        
    
    def __refactorPath(self, path):
        return os.path.dirname(self.__projectInfoPath) + os.sep + path.replace("/", os.sep)

    def __parseProjectInfo(self, projectInfoPath): 
        f = open(projectInfoPath,'r')
        try:
            j = f.read()
            projectInfo = json.loads(j) 
        finally:
            f.close()
        
        scmProjectInfo = projectInfo['scmProject']
        self.__parentPomPath = self.__refactorPath(scmProjectInfo['parentPomPath'])
        self.__scmModulePomPaths = {}
        for scmModule in scmProjectInfo['modules']:
            self.__scmModulePomPaths[scmModule['name']] = self.__refactorPath(scmModule['modulePomPath'])
        
        self.__packageInfo = projectInfo['releasePackage']
        
        testProjectInfo = projectInfo['scmTestProject']
        self.__testLibPath = self.__refactorPath(testProjectInfo['libPath'])
        self.__testDependencies = []
        for dependency in testProjectInfo['copyDependencies']:
            self.__testDependencies.append(self.__refactorPath(dependency))
        self.__testModulePomPaths = {}
        self.__testModuleTestNgXmlPath = {}
        for testModule in testProjectInfo['modules']:
            self.__testModulePomPaths[testModule['name']] = self.__refactorPath(testModule['modulePomPath'])
            self.__testModuleTestNgXmlPath[testModule['name']] = self.__refactorPath(testModule['testNgXmlPath'])
    
    def compileScmModule(self, moduleName, customArg = ""):
        modulePath = self.__scmModulePomPaths[moduleName];
        if modulePath is None:
            raise Exception("unknown moduleName:" + moduleName + ", all modules:" + self.__scmModulePomPaths.keys())
        
        self.__cmdExecutor.command("mvn clean install -Dmaven.test.skip=true -f " + modulePath + " " + customArg)
            
    def compileScm(self, customArg = ""):
        self.__cmdExecutor.command("mvn clean install -Dmaven.test.skip=true -f " + self.__parentPomPath + " " + customArg)

    def compileDoc(self, customArg = ""):
        print("Start create the pdf")
        self.__cmdExecutor.command("python " + customArg + "build.py --pdf")

    def getScmModules(self):
        return self.__scmModulePomPaths.keys()
        
    def getTestModules(self):
        return self.__testModulePomPaths.keys()
        
    def __copyTestDependencies(self):
        for dependencyPath in self.__testDependencies:
            for file in glob.glob(dependencyPath):
                print("copy test dependency:" + file)
                shutil.copyfile(file, self.__testLibPath + os.sep + os.path.basename(file))  
        
    def compileTest(self):
        self.__copyTestDependencies()
        for testModuleName in self.__testModulePomPaths.keys():
            self.__cmdExecutor.command("mvn clean package -Dmaven.test.skip=true -f " + self.__testModulePomPaths[testModuleName])
    
    def compileTestModule(self, *moduleNames):
        self.__copyTestDependencies()
        for moduleName in moduleNames:
            modulePath = self.__testModulePomPaths[moduleName];
            if modulePath is None:
                raise Exception("unknown moduleName:" + moduleName + ", all modules:" + self.__testModulePomPaths.keys())
            self.__cmdExecutor.command("mvn clean package -Dmaven.test.skip=true -f " + modulePath)
    
    def runAllTest(self):
        for module in self.getTestModules():
            self.runTest(module)
    
    def runTest(self, *testModuleNames):
        for testModuleName in testModuleNames:
            testReport = os.path.dirname(self.__testModulePomPaths[testModuleName]) + "target" + os.sep + "test-ouput"
            self.__cmdExecutor.command('mvn surefire:test -DreportDir='+testReport+' -DxmlFileName=' + self.__testModuleTestNgXmlPath[testModuleName] + ' -f ' + self.__testModulePomPaths[testModuleName])
    
    def __makePackageTree(self, output, node):
        type = node["type"]
        if type == "new_dir":
            newDirPath = os.path.join(output, node["name"])
            os.makedirs(newDirPath)
            for child in node["childs"]:
                self.__makePackageTree(newDirPath, child)
        elif type == "copy":
            srcPath = self.__refactorPath(node["src"])
            if os.path.isdir(srcPath):
                shutil.copytree(srcPath, os.path.join(output, os.path.basename(srcPath)))
            else:
                for file in glob.glob(srcPath):
                    shutil.copy(file, os.path.join(output, os.path.basename(file)))  
        else:
            raise Exception("unknown type:" + type)
            
    
    def packageScm(self, outputFilePath):
        print("packing...")
        if os.path.exists(outputFilePath):
            os.remove(outputFilePath)
        elif os.path.exists(os.path.dirname(outputFilePath)) == False:
            os.makedirs(os.path.dirname(outputFilePath))
            
        tmpSave = sys.path[0] + os.sep + "tmp" + os.sep;
        if os.path.exists(tmpSave): 
            shutil.rmtree(tmpSave)
        os.makedirs(tmpSave)
        
        for ele in self.__packageInfo:
            self.__makePackageTree(tmpSave, ele)
        
        with tarfile.open(outputFilePath,"w:gz") as tar:
            for filename in os.listdir(tmpSave):
                filePath = os.path.join(tmpSave, filename)
                tar.add(filePath, os.path.basename(filePath))
                
        print("success:" + outputFilePath)
        
        shutil.rmtree(tmpSave)




