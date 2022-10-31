#!/usr/bin/python
import sys, getopt
import os
import commands
import os
import sys
import platform
import tarfile
import glob
import shutil
import fnmatch
import socket
import re
rootDir = sys.path[0]+os.sep
isInstallSdb = False
CLUSTER = 2
SDB_NODE_PATH = "/opt/sequoiadb"
SDB_INSTALL_PATH = os.sep + "opt" + os.sep + "sequoiadb"+ os.sep
SDB_PATH = '/tmp/sequoiadb-2.8.1-linux_x86_64-enterprise-installer.run'
SDB_COORD_1 = 11810
LOCAL_HOST = socket.gethostname()


# only run in linux
def display(exit_code):
    print("")
    print(" --help | -h       : print help message")
    print(" --installsdb      : ")
    print(" --cluster         : cluster ")
    print(" --runpath         : ")
    sys.exit(exit_code)

def execCMD(cmd):
    print "INFO:" + cmd
    ret = os.system(cmd)
    if ret != 0:
        sys.exit(ret)

def dealWithJsConf(filename, coordBasePort, cataBasePort, dataBasePort, dbPath):
    file = open(filename)
    lines = []
    for line in file.xreadlines():
        if re.match("^var coordBasePortArg ", line):
            lines.append("var coordBasePortArg = " + str(coordBasePort) + ";\n")
        elif re.match("^var cataBasePortArg ", line):
            lines.append("var cataBasePortArg = " + str(cataBasePort) + ";\n")
        elif re.match("^var dataBasePortArg ", line):
            lines.append("var dataBasePortArg = " + str(dataBasePort) + ";\n")
        elif re.match("^var diskList ", line):
            lines.append("var diskList = ['" + dbPath + "'];\n")
        else :
            lines.append(line)
    file.close()
    file.close()
    file = open(filename, 'w')
    file.writelines(lines)
    file.close()

def installSdb():
    global SDB_INSTALL_PATH, SDB_PATH
    print "INFO:" + SDB_INSTALL_PATH + "uninstall --mode unattended"
    os.system(SDB_INSTALL_PATH + "uninstall --mode unattended")
    os.system("rm -rf " + SDB_INSTALL_PATH)
    if os.path.exists(SDB_PATH):
        execCMD(SDB_PATH + " --mode unattended")
    else:
        print("Your installation package is not exist !")
        sys.exit(1)

def deploySDB():
    global SDB_INSTALL_PATH
    execCMD("rm -rf " + SDB_NODE_PATH + "/database")
    execCMD("mkdir -p " + SDB_NODE_PATH + "/database");
    execCMD("chown sdbadmin " + SDB_NODE_PATH + "/database")

    deployConfFileName = rootDir + "deploy.js"
    deployCmd1 = SDB_INSTALL_PATH + "/bin/sdb -f '" + deployConfFileName + "' -e " + "\" var hostList=['" + LOCAL_HOST + "','" + LOCAL_HOST + "','" + LOCAL_HOST + "'];var diagLevel=5;var diskList=['" + SDB_NODE_PATH + "']\""
    #ready to deploy other db cluster
    #tar bin.tar.gz
    execCMD("cp -r " + SDB_INSTALL_PATH + "/bin " + SDB_INSTALL_PATH + "/bin_back")
    execCMD("cp -r " + SDB_INSTALL_PATH + "/conf " + SDB_INSTALL_PATH + "/conf_back")

    #deploy first db cluster in every host
    #deal with file
    dealWithJsConf(deployConfFileName, SDB_COORD_1, 11210, 20100, SDB_INSTALL_PATH)
    ret = os.system(deployCmd1)
    if not ret == 0:
        sys.exit(ret)
    execCMD(SDB_INSTALL_PATH + "/bin/sdb -s \"var db = new Sdb; db.createDomain(\\\"domain1\\\",[\\\"group1\\\"])\"")
    execCMD(SDB_INSTALL_PATH + "/bin/sdb -s \"var db = new Sdb; db.createDomain(\\\"domain2\\\",[\\\"group1\\\"])\"")
    TEMP_SDB_COORD = 11840
    TEMP_SDB_CATA = 11250
    TEMP_SDB_DATA = 20130
    count = 1
    while count < int(CLUSTER):
        print("***********************")
        sum = count + 1
        TEMP_PATH = SDB_INSTALL_PATH + "cluster" + str(sum)
        execCMD("mkdir " + TEMP_PATH)
        execCMD("cp -r " + SDB_INSTALL_PATH + "/bin_back " + TEMP_PATH + "/bin")
        execCMD("cp -r " + SDB_INSTALL_PATH + "/conf_back " + TEMP_PATH + "/conf")
        dealWithJsConf(deployConfFileName, TEMP_SDB_COORD, TEMP_SDB_CATA, TEMP_SDB_DATA, SDB_INSTALL_PATH)
        ret = os.system(deployCmd1)
        if not ret == 0:
            sys.exit(ret)
        execCMD(SDB_INSTALL_PATH + "/bin/sdb -s \"var db = new Sdb(\\\"localhost\\\"," + str(TEMP_SDB_COORD) + "); db.createDomain(\\\"domain2\\\",[\\\"group1\\\"])\"")
        TEMP_SDB_COORD += 30
        TEMP_SDB_CATA += 30
        TEMP_SDB_DATA += 30
        count += 1

    execCMD("rm -rf " + SDB_INSTALL_PATH + "/bin_back")
    execCMD("rm -rf " + SDB_INSTALL_PATH + "/conf_back")

def parse_command():
    global isInstallSdb, CLUSTER, SDB_PATH
    try:
        options, args = getopt.getopt(sys.argv[1:], "hc:at:", ["help", "installsdb", "runpath=", "cluster="])
    except getopt.GetoptError, e:
        print ("Error:", e)
        sys.exit(-1)

    for name, value in options:
        if name in ("-h", "--help"):
            display(0)
        elif name in ("--installsdb"):
            isInstallSdb = True
        elif name in ("--runpath"):
            SDB_PATH = value
        elif name in ("--cluster"):
            CLUSTER = value

if __name__ == '__main__':
    parse_command()
    doSomething = False
    if isInstallSdb:
        installSdb()
        deploySDB()
        doSomething = True
    if doSomething == False:
        display(0)


