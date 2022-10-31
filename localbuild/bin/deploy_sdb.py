#!/usr/bin/python
import sys, getopt
import os
import shutil
import glob
import paramiko
import ConfigParser
rootDir = sys.path[0]+os.sep
sys.path.append(rootDir)
import scmCmdExecutor
from scmCmdExecutor import ScmCmdExecutor
from SSHConnection import SSHConnection

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
    print(" --help | -h            : print help message")
    print(" --package-file         : SDB installation package path ")
    print(" --host                 : hostname--at least one, ',' separated ")
    print(" --output               : output SDB information:sdb.info")
    print(" --ssh-info             : ssh information : filled in localbuild.conf")
    print(" --force                : force to install SDB cluster")
    sys.exit(exit_code)

def parse_command():
    global PACKAGE_FILE, HOST, SDB_FILE, IS_FORCE, SSH_FILE, TEMPLATE
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc:at:", ["help", "package-file=", "host=", "output=", "ssh-file=", "template=", "force"])
    except getopt.GetoptError, e:
        print ("Error:", e)
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



def generateSdbInfo(sdbFile, sdbHost, cluster, count):
    # port fixed temporarily
    port = 11810
    temp = 1
    cf = ConfigParser.ConfigParser()
    cf.read(sdbFile)
    cf.add_section('cluster'+str(count+1))
    while temp <= int(cluster):
        cf.set('cluster'+str(count+1), 'coord'+str(temp), str(sdbHost) + ":" + str(port))
        port += 30
        temp += 1
    cf.write(open(sdbFile, 'w'))


def execDeploySdb(ssh, localRunPath, cluster, sdbFile, host, count):
    try:
        res = ssh.cmd("su - sdbadmin -c 'sdbstop -a'")
        if res[0] != 0:
            raise Exception("Failed to stop SDB cluster!")
        ssh.cmd("chmod +x /tmp/localbuild/" + localRunPath.split(os.sep)[-1])
        installRes = ssh.cmd("python /tmp/localbuild/installsdb/installSdb.py  --installsdb --runpath /tmp/localbuild/"  + localRunPath.split(os.sep)[-1] + " --cluster " + str(cluster))
        if installRes[0] == 0:
            print("-----------------------------------Succeed to install SDB cluster---------------------------")
            generateSdbInfo(sdbFile, host, cluster, count)
        else:
            raise Exception("Failed to install SDB cluster")
    except Exception as e:
        raise e

def installSdb(localRunPath, host, cluster, sdbFile, sshInfo, isForce, count):
     info = sshInfo.split(",")
     try:
        ssh = SSHConnection(host = host, user = info[0], pwd = info[1])
        ssh.mkdir("/tmp/localbuild")
        ssh.mkdir("/tmp/localbuild/installsdb")
        files = glob.glob(BIN_DIR + 'installsdb' + os.sep + '*')
        for file in files:
            ssh.upload(file, "/tmp/localbuild/installsdb/" + file.split(os.sep)[-1])
        ssh.upload(localRunPath, "/tmp/localbuild/" + localRunPath.split(os.sep)[-1] )
        sdbRes = ssh.cmd("su - sdbadmin -c  sdblist")
        if  sdbRes[0] != 0:
            execDeploySdb(ssh, localRunPath, cluster, sdbFile, host, count)
        else:
            if isForce:
                print("Forcing uninstall and reinstall ,please wait")
                execDeploySdb(ssh, localRunPath, cluster, sdbFile, host, count)
            else:
                print("SDB cluster is exists !")
                while (True):
                    print("Do you want uninstall and reinstall it ?(y/n):\n")
                    res = raw_input("Please enter your choice:")
                    if res == "Y" or res == "y":
                        print("uninstalling and reinstalling ,please wait")
                        execDeploySdb(ssh, localRunPath, cluster, sdbFile, host, count)
                        break
                    elif res == "N" or res == "n":
                        print("know your choice ,exiting!")
                        sys.exit(0)
                    else:
                        print("I don't know your choice,please enter again")
                        continue
     except Exception as e:
        print("*************************Fail to install SDB cluster*************************************")
        raise e
     finally:
        res = ssh.cmd("find /tmp/localbuild")
        if  res[0] == 0:
            ssh.cmd("rm -rf /tmp/localbuild" )
        ssh.close()


def getSdbTemplate(template):
    try:
        clusterArr = []
        cf = ConfigParser.ConfigParser()
        cf.read(template)
        arr = cf.sections()
        count = 0
        while count < len(arr):
            num = cf.get(arr[count],"cluster")
            if num != "":
                clusterArr.append(str(num))
            count += 1
        if len(clusterArr) == 0:
            raise Exception("The information you filled in have insufficient in deploySdbTemplate!")
        return clusterArr
    except Exception as e:
        raise e

def getSSHInfo(confFile):
    try:
        sshArr= []
        cf = ConfigParser.ConfigParser()
        cf.read(confFile)
        arr = cf.sections()
        count = 0
        val = 0
        while count < len(arr):
            user = cf.get(arr[count],"user")
            pwd = cf.get(arr[count],"password")
            if str(arr[count]) != "host":
                val = str(arr[count]).replace('host', '')
            sshArr.append(str(user) + "," + str(pwd) + ","+ str(val))
            count += 1
        return sshArr
    except Exception as e:
        print("The information you filled in have insufficient in localbuild.conf!")
        raise e

if __name__ == '__main__':
    try:
        parse_command()
        print("------------------------------start install SDB cluster:---------------------------------------------")
        if os.path.exists(SDB_FILE):
            os.remove(SDB_FILE)
        if not os.path.exists(PACKAGE_FILE) or len(PACKAGE_FILE.strip()) == 0:
            raise Exception("Missing SDB installation package or package file is not exists!")
        if not os.path.exists(TEMPLATE) or len(TEMPLATE.strip()) == 0 :
            raise Exception("Missing deploy SDB Template or SDB Template file is not exists!")
        if not os.path.exists(SSH_FILE) or len(SSH_FILE.strip()) == 0 :
            raise Exception("Missing SSH info file or SSH info file is not exists!")
        if len(SDB_FILE.strip()) == 0 :
            raise Exception("Missing SDB info file path !")
        hostArr = HOST.split(",")
        if len(hostArr) == 0:
            raise Exception("Missing hostname!")
        clusterArr = getSdbTemplate(TEMPLATE)
        sshArr = getSSHInfo(SSH_FILE)
        count = 0
        sshItemArr = []
        sshInfoArr = []
        for ch in sshArr:
            arr1 = str(ch).split(",")
            sshItemArr.append(arr1[2])
            sshInfoArr.append(str(arr1[0]) + "," + arr1[1])
        value = 0
        if len(sshItemArr) > 1:
            i = 1
            while i < len(sshItemArr):
                if str(sshItemArr[i]) == str(count+1):
                    value = i
                    break
                i += 1
        while count < len(clusterArr):
            if int(clusterArr[count]) > 10:
                raise Exception("The number of cluster lss than MAX_CLUSTER:10!")
            installSdb(PACKAGE_FILE, hostArr[count], clusterArr[count], SDB_FILE, sshInfoArr[value], IS_FORCE, count)
            count += 1
    except Exception as e:
        print(e)
        raise e









