#!/usr/bin/python
import sys, getopt
import os
import json
rootDir = sys.path[0] + os.sep
          
def run():   
    args = " ".join(sys.argv[1:])
    
    commandPath = rootDir + "sequoiacm-deploy" + os.sep + "command"
    logbackPath = rootDir + "sequoiacm-deploy" + os.sep + "bindata" + os.sep + "logback.xml"
    # logback will throw exception if logbackPath is windows abspath
    logbackPath = os.path.relpath(logbackPath)
    
    appPropertiesPath = rootDir + "sequoiacm-deploy" + os.sep + "bindata" + os.sep + "application.properties"
    jvmOption = "-Dvisible.commands=" + commandPath
    jvmOption += " -Dlogback.configurationFile=" + logbackPath
    jvmOption += " -Dapplication.properties=" + appPropertiesPath
    jvmOption += " -Dcommon.basePath=" + rootDir
    
    jarPath = rootDir + "sequoiacm-deploy" + os.sep + "bin" + os.sep + "sequoiacm-deploy-3.0.0.jar"
    cmd = "java " + jvmOption + " -jar " + jarPath + " " + args
    
    command(cmd)

def command(cmd):
    ret = os.system(cmd)
    if ret != 0:
        print("failed to run cmd")
        sys.exit(1)
    
if __name__ == '__main__':
    run()
