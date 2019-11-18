#!/usr/bin/python
import sys, getopt
import os
import platform
import commands

NEED_COMPILE = 1
NEED_INSTALL = 0

rootDir = sys.path[0]
deployDir = rootDir + os.sep + ".." + os.sep + "deploy"
CONF_CTL = "confctl.sh"
CONF_ADMIN = "confadmin.sh"

def display_info(msg):
    print("========================================================================")
    print(msg)
    print("========================================================================")


def display(exit_code):
    print(" --help | -h       : print help message")
    print(" --nocompile       : do not compile all")
    print(" --install         : install sequoiacm-config")
    sys.exit(exit_code)


def parse_command():
    global NEED_COMPILE, NEED_INSTALL
    try:
        options, args = getopt.getopt(sys.argv[1:], "h", ["help", "nocompile", "install"])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name == '--nocompile':
            NEED_COMPILE = 0
        elif name == '--install':
            NEED_INSTALL = 1


def exec_cmd(cmd):
    print ("INFO:" + cmd)
    ret = os.system(cmd)
    if ret != 0:
        sys.exit(ret)
            

def compile_all():
    ret = os.system("mvn clean install -f " + rootDir + "/../project/pom.xml")
    if ret != 0:
        display_info("compile failed")
        sys.exit(1)

def install():
    if os.path.exists(deployDir + "/bin/" + CONF_ADMIN):
        exec_cmd(deployDir + "/bin/" + CONF_CTL + " stop --type all")
    else:
        print("INFO: " + deployDir + "/bin/" + CONF_CTL + " notexist, ignore to stop node" )

    if os.path.exists(deployDir):
        exec_cmd("rm -rf " + deployDir + "/*")
    else:
        print("INFO: " + deployDir +  "notexist, ignore to clean " + deployDir)

    exec_cmd("mkdir -p " + deployDir)
        
    exec_cmd("cp -r " + rootDir + "/../project/common/assembly/target/sequoiacm-config-*-release/sequoiacm-config/* " + deployDir)
        
    exec_cmd("python " + rootDir + "/deploy.py --conf " + rootDir + "/deploy.json --bin "+deployDir+"/bin")
    
    #stop residul process
    exec_cmd(deployDir+"/bin/" + CONF_CTL + " stop --type all")
    
    exec_cmd(deployDir+"/bin/" + CONF_CTL + " start --type all")

if __name__ == '__main__':
    parse_command()
    if NEED_COMPILE == 1:
        compile_all()
    if NEED_INSTALL == 1:
        install()

