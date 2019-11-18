#!/usr/bin/python
import sys, getopt
import os
import platform
import commands

NEED_COMPILE = 1
NEED_INSTALL = 0

rootDir = sys.path[0] + os.sep + ".."
SCH_CTL = "schctl.sh"
SCH_ADMIN = "schadmin.sh"

def display_info(msg):
    print("========================================================================")
    print(msg)
    print("========================================================================")


def display(exit_code):
    print(" --help | -h       : print help message")
    print(" --nocompile       : do not compile all")
    print(" --install         : install sequoiacm-schedule")
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
    ret = os.system("mvn clean package -f " + rootDir + "/project/pom.xml")
    if ret != 0:
        display_info("compile failed")
        sys.exit(1)


def install():
    if os.path.exists(rootDir+"/deploy/bin/" + SCH_ADMIN):
        exec_cmd(rootDir+"/deploy/bin/" + SCH_CTL + " stop --type all")
    else:
        print("INFO: " + rootDir+"/deploy/bin/" + SCH_CTL + " notexist, ignore to stop node" )

    if os.path.exists(rootDir+"/deploy"):
        exec_cmd("rm -rf "+rootDir+"/deploy/*")
    else:
        print("INFO: " + rootDir+"/deploy notexist, ignore to clean " + rootDir+ "/deploy")

    exec_cmd("mkdir -p " + rootDir + "/deploy")
        
    exec_cmd("cp -r " + rootDir + "/project/common/assembly/target/sequoiacm-schedule-*-release/sequoiacm-schedule/* " + rootDir + "/deploy/")
        
    exec_cmd("python " + rootDir + "/deploy/deploy.py --conf " + rootDir + "/deploy/deploy.json --bin "+rootDir+"/deploy/bin")
    
    #stop residul process
    exec_cmd(rootDir+"/deploy/bin/" + SCH_CTL + " stop --type all")
    
    exec_cmd(rootDir+"/deploy/bin/" + SCH_CTL + " start --type all")

if __name__ == '__main__':
    parse_command()
    if NEED_COMPILE == 1:
        compile_all()
    if NEED_INSTALL == 1:
        install()

