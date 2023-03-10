#!/usr/bin/python
# -*- coding:utf-8 -*-
import ConfigParser
import getopt
import os
import re
import sys

rootDir = sys.path[0] + os.sep
sys.path.append(rootDir)
from scmCmdExecutor import ScmCmdExecutor
from SSHConnection import SSHConnection
from logUtil import Logging

LOG_PATH = rootDir + '..' + os.sep + 'tmp' + os.sep + 'deploysdb.log'
log = Logging(LOG_PATH).get_logger()
BIN_DIR = sys.path[0] + os.sep + '..' + os.sep + 'bin' + os.sep

cmdExecutor = ScmCmdExecutor(False)

PACKAGE_FILE = ""
HOST = ""
SDB_FILE = ""
TEMPLATE = ""
SSH_FILE = ""
IS_FORCE = False


def display(exit_code):
    print("")
    print(" --help | -h                  : print help message")
    print(" --package-file <arg>         : SDB installation package path ")
    print(" --host        <arg>          : hostname--at least one, ',' separated ")
    print(" --output      <arg>          : output SDB information:sdb.info")
    print(" --template    <arg>          : deploy sdb template")
    print(" --ssh-info    <arg>          : ssh information : filled in localbuild.conf")
    print(" --force                      : force to install SDB cluster")
    sys.exit(exit_code)


def parse_command():
    global PACKAGE_FILE, HOST, SDB_FILE, IS_FORCE, SSH_FILE, TEMPLATE
    try:
        options, args = getopt.getopt(sys.argv[1:], "h",
                                      ["help", "package-file=", "host=", "output=", "ssh-file=", "template=", "force"])
    except getopt.GetoptError, e:
        log.error(e, exc_info=True)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--package-file"):
            PACKAGE_FILE = value
        elif name in ("--host"):
            HOST = value
        elif name in ("--output"):
            SDB_FILE = value
        elif name in ("--ssh-file"):
            SSH_FILE = value
        elif name in ("--template"):
            TEMPLATE = value
        elif name in ("--force"):
            IS_FORCE = True


def generateSdbInfo(sdbInfoArr, sdbFile):
    # port fixed temporarily
    cf = ConfigParser.ConfigParser()
    cf.read(sdbFile)
    count = 0
    while count < len(sdbInfoArr):
        cf.add_section('cluster' + str(count + 1))
        cf.set('cluster' + str(count + 1), 'coord', sdbInfoArr[count + 1])
        count += 1
    cf.write(open(sdbFile, 'w'))


def getCoordInfo(deployInfoArr):
    sdbInfoArr = {}
    count = 1
    while count < len(deployInfoArr):
        tempStr = ""
        deployArr = deployInfoArr[count].split('\n')
        temp = 0
        while temp < len(deployArr):
            if str(deployArr[temp]).startswith("coord"):
                coordArr = str(deployArr[temp]).split(",")
                tempStr += str(coordArr[2]).strip() + ":" + str(coordArr[3]).strip() + ","
            temp += 1
        sdbInfoArr[count] = tempStr
        count += 1
    return sdbInfoArr


def execDeploySdb(host, deployInfoArr, sshInfo, sdbFile):
    info = sshInfo.split(",")
    try:
        ssh = SSHConnection(host=host, user=info[0], pwd=info[1])
        sdbInfoArr = getCoordInfo(deployInfoArr)
        count = 1
        while count < len(deployInfoArr):
            deployArr = deployInfoArr[count].split('\n')
            temp = 0
            while temp < len(deployArr):
                if str(deployArr[temp]).startswith("coord"):
                    coordArr = str(deployArr[temp]).split(",")
                    break
                temp += 1
            ssh.cmd("echo '" + str(deployInfoArr[count]) + "' >/opt/sequoiadb/tools/deploy/sequoiadb.conf ")
            deployRes = ssh.cmd("su - sdbadmin -c 'bash /opt/sequoiadb/tools/deploy/quickDeploy.sh --sdb'")
            if deployRes[0] != 0:
                raise Exception("Failed to deploy SDB cluster!")
            ssh.cmd("/opt/sequoiadb/bin/sdb -s \"var db = new Sdb(\\\"" + str(coordArr[2]).strip() + "\\\", \\\"" + str(
                coordArr[3]).strip() + "\\\"); db.createDomain(\\\"domain1\\\",[\\\"group1\\\"])\"")
            ssh.cmd("/opt/sequoiadb/bin/sdb -s \"var db = new Sdb(\\\"" + str(coordArr[2]).strip() + "\\\", \\\"" + str(
                coordArr[3]).strip() + "\\\"); db.createDomain(\\\"domain2\\\",[\\\"group1\\\"])\"")
            count += 1
        generateSdbInfo(sdbInfoArr, sdbFile)
    except Exception as e:
        raise e


def installSdb(localRunPath, host, sshInfo, isForce):
    info = sshInfo.split(",")
    try:
        ssh = SSHConnection(host=host, user=info[0], pwd=info[1])
        ssh.mkdir("/tmp/localbuild")
        ssh.upload(localRunPath, "/tmp/localbuild/" + localRunPath.split(os.sep)[-1])
        ssh.cmd("chmod +x /tmp/localbuild/" + localRunPath.split(os.sep)[-1])
        sdbRes = ssh.cmd("su - sdbadmin -c  sdblist")
        if sdbRes[0] != 0:
            ssh.cmd("/tmp/localbuild/" + localRunPath.split(os.sep)[-1] + " --mode unattended")
        else:
            if isForce:
                print("Forcing uninstall and reinstall ,please wait")
                res = ssh.cmd("su - sdbadmin -c 'sdbstop -a'")
                if res[0] != 0:
                    raise Exception("Failed to stop SDB cluster!")
                ssh.cmd("/opt/sequoiadb/uninstall --mode unattended")
                ssh.cmd("rm -rf /opt/sequoiadb/")
                ssh.cmd("/tmp/localbuild/" + localRunPath.split(os.sep)[-1] + " --mode unattended")
            else:
                print("SDB cluster is exists !")
                while (True):
                    print("Do you want uninstall and reinstall it ?(y/n):\n")
                    res = raw_input("Please enter your choice:")
                    if res == "Y" or res == "y":
                        print("uninstalling and reinstalling ,please wait")
                        res = ssh.cmd("su - sdbadmin -c 'sdbstop -a'")
                        if res[0] != 0:
                            raise Exception("Failed to stop SDB cluster!")
                        ssh.cmd("/opt/sequoiadb/uninstall --mode unattended")
                        ssh.cmd("rm -rf /opt/sequoiadb/")
                        ssh.cmd("/tmp/localbuild/" + localRunPath.split(os.sep)[-1] + " --mode unattended")
                        break
                    elif res == "N" or res == "n":
                        print("know your choice ,exiting!")
                        sys.exit(0)
                    else:
                        print("I don't know your choice,please enter again")
                        continue
    except Exception as e:
        raise e
    finally:
        res = ssh.cmd("find /tmp/localbuild")
        if res[0] == 0:
            ssh.cmd("rm -rf /tmp/localbuild")
        ssh.close()


def getSdbTemplate(template, hostList):
    try:
        pattern = r'\[cluster[0-9]\]\n'
        with open(template, 'r') as file:
            data = file.read()
            count = 1
            while count <= int(len(hostList)):
                sum = count - 1
                data = data.replace("hostname" + str(count), str(hostList[sum])).replace(" ", "").replace('\r', "")
                count += 1
            deployInfoArr = re.split(pattern, data)
        if len(deployInfoArr) <= 1:
            raise Exception("The information you filled in have insufficient in deploySdbTemplate!")
        return deployInfoArr
    except Exception as e:
        raise e


def getSSHInfo(confFile):
    try:
        sshArr = {}
        cf = ConfigParser.ConfigParser()
        cf.read(confFile)
        arr = cf.sections()
        count = 0
        val = 0
        while count < len(arr):
            user = cf.get(arr[count], "user")
            pwd = cf.get(arr[count], "password")
            if str(arr[count]) != "host":
                val = int(str(arr[count]).replace('host', ''))
            sshArr[val] = str(user) + "," + str(pwd)
            count += 1
        return sshArr
    except Exception as ex:
        log.error('The information you filled in have insufficient in localbuild.conf!')
        raise ex


if __name__ == '__main__':
    try:
        parse_command()
        log.info("start install SDB cluster")
        if os.path.exists(SDB_FILE):
            os.remove(SDB_FILE)
        if not os.path.exists(PACKAGE_FILE) or len(PACKAGE_FILE.strip()) == 0:
            raise Exception("Missing SDB installation package or package file is not exists!")
        if not os.path.exists(TEMPLATE) or len(TEMPLATE.strip()) == 0:
            raise Exception("Missing deploy SDB Template or SDB Template file is not exists!")
        if not os.path.exists(SSH_FILE) or len(SSH_FILE.strip()) == 0:
            raise Exception("Missing SSH info file or SSH info file is not exists!")
        if len(SDB_FILE.strip()) == 0:
            raise Exception("Missing SDB info file path !")
        hostArr = HOST.split(",")
        if len(hostArr) == 0:
            raise Exception("Missing hostname!")
        deployInfoArr = getSdbTemplate(TEMPLATE, hostArr)
        sshDict = getSSHInfo(SSH_FILE)
        count = 0
        while count < len(hostArr):
            if sshDict.has_key(count + 1):
                installSdb(PACKAGE_FILE, hostArr[count], sshDict[count + 1], IS_FORCE)
            else:
                installSdb(PACKAGE_FILE, hostArr[count], sshDict[0], IS_FORCE)
            log.info('install sdb , host=' + str(hostArr[count]))
            count += 1
        if sshDict.has_key(1):
            execDeploySdb(hostArr[0], deployInfoArr, sshDict[1], SDB_FILE)
        else:
            execDeploySdb(hostArr[0], deployInfoArr, sshDict[0], SDB_FILE)
    except Exception as e:
        log.error(e)
        raise e
