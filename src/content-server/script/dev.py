#!/usr/bin/python
import commands
import getopt
import os
import platform
import sys

SCMCLOUDCTL = "scmctl.sh"

need_compile = True
need_install = False
dry_run = False

root_dir = sys.path[0]
deploy_dir = root_dir + os.sep + ".." + os.sep + "deploy"


def display_info(msg):
    print("========================================================================")
    print(msg)
    print("========================================================================")


def print_help():
    print(" --help | -h       : print help message")
    print(" --nocompile       : do not compile all")
    print(" --install         : install sequoiacm-cloud")
    print(" --path | -p       : specify the path to install sequoiacm-cloud, default is <dev_path>/deploy")


def parse_command():
    global need_compile, need_install, deploy_dir, dry_run
    try:
        options, args = getopt.getopt(sys.argv[1:], "hp:", ["help", "nocompile", "install", "dryrun", "path="])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(1)

    for name, value in options:
        if name in ("-h", "--help"):
            print_help()
            sys.exit(0)
        elif name == '--nocompile':
            need_compile = False
        elif name == '--dryrun':
            dry_run = True
        elif name == '--install':
            need_install = True
        elif name in ("-p", "--path"):
            deploy_dir = value.strip()
            if deploy_dir.endswith(os.sep):
                deploy_dir = deploy_dir[:-1]


def exec_cmd(cmd):
    print ("CMD:" + cmd)
    print(cmd)
    if dry_run:
        return

    ret = os.system(cmd)
    if ret != 0:
        sys.exit(ret)


def compile_all():
    ret = os.system("mvn clean install -f " + root_dir + os.sep + ".." + os.sep + "project" + os.sep + "pom.xml")
    if ret != 0:
        display_info("compile failed")
        sys.exit(1)

def install():
    ctl_cmd = deploy_dir + os.sep + "bin" + os.sep + SCMCLOUDCTL
    if os.path.exists(ctl_cmd):
        tools_jar = root_dir + os.sep + ".." + os.sep + "project" \
                    + os.sep + "common" \
                    + os.sep + "tools" \
                    + os.sep + "target" \
                    + os.sep + "sequoiacm-content-tools-*.jar "
        exec_cmd("cp -r " + tools_jar + deploy_dir + os.sep + "jars")
        exec_cmd(ctl_cmd + " stop --all -f")
        
    else:
        print("INFO: " + ctl_cmd + " not exist, no need to stop node")

    if os.path.exists(deploy_dir):
        exec_cmd("rm -rf " + deploy_dir + os.sep + "*")
    else:
        print("INFO: " + deploy_dir + " is not exist")

    exec_cmd("mkdir -p " + deploy_dir)

    package_path = root_dir + os.sep + ".." + os.sep + "project" \
        + os.sep + "common" \
        + os.sep + "assembly" \
        + os.sep + "target" \
        + os.sep + "sequoiacm-content-*-release" \
        + os.sep + "sequoiacm-content" \
        + os.sep + "* "
    exec_cmd("cp -r " + package_path + deploy_dir + os.sep)

    exec_cmd("python " + deploy_dir + os.sep + "deploy.py " 
             + "--conf " + deploy_dir + os.sep + "deploy.json "
             + "--bin " + deploy_dir + os.sep + "bin "
             + " --createsite --createnode")

    # stop residual process
    exec_cmd(ctl_cmd + " stop --all -f")
    # start
    exec_cmd(ctl_cmd + " start --all --timeout 240")

if __name__ == '__main__':
    parse_command()
    if need_compile:
        compile_all()
    if need_install:
        install()
