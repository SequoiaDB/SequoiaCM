#!/usr/bin/python

import os 
import sys
import platform
import tarfile
import glob
import shutil
import ConfigParser
import xml.dom.minidom
import re
import threading
import commands
import socket
import getopt

#init
SCM_HOSTINFO = ''
DB_INSTALL_DIR = ''
SCM_INSTALL_DIR = ''
SCM_DEPLOY_MODE = ''
EXEC_MODE = ''
TEST_IS_EXEC = ''
MAIN_SITE_HOST = ''
SCM_PORT = ''
SDB_COORD_PORT = ''
SCRIPT_CONF_PATH = ''
TESTCASETYPELIST = []
CONTROL_HOSTNAME = ''
CONTROL_HOST_USER = ''
CONTROL_HOST_PASSWD = ''
HOSTLIST = []
HOSTUSERINFOS = {}
HOSTPWDINFOS = {}
TEST_CASE_DIR =''
SCMCLOUD_GATEWAYS = ''
SCM_DEPLOY_NET = ''
S3_ACCESSKEY = ''
S3_SECRETKEY = ''
   
def printHelp():
    print ('        --COMMON_SCM_HOSTINFO: all host of scm cluster(host:user:passwd,host2:user:passwd) ,require')
    print ('        --COMMON_MAIN_SITE_HOST: specify main site host name(--SCM_HOSTINFO contain this host)')
    print ('        --COMMON_SDB_INSTALL_DIR: sequoiadb install dir')
    print ('        --COMMON_SCM_INSTALL_DIR: sequoiacm install dir')
    print ('        --COMMON_SCM_DEPLOY_MODE: 1(a site,a server),2(tow site,tow server),3(four site,four sever)')
    print ('        --COMMON_SCM_PORT: scm server\'s port')
    print ('        --COMMON_SDB_COORD_PORT: sdb\'s coord port')
    print ('        --COMMON_SCRIPT_CONF_PATH: the test_py.conf for this script')
    print ('        --COMMON_S3_ACCESSKEY: s3 accesskey')
    print ('        --COMMON_S3_SECRETKEY: s3 secretkey')
    print ('        --COMMON_CI_CONTROL_HOSTINFO: control hostname, username and passwd(host:user:passwd)')
    print ('        --TEST_EXEC_MODE: p(parralel execute testcase in different host),default parallel in one host')
    print ('        --TEST_IS_EXEC: true(compile and execute testcase),false(compile testcase)')
    print ('        --TEST_CASE_DIR: testcase dir')
    print ('        --TEST_CASE_TYPE: testcase type will be execute(story,tdd)')
    print ('        --SCMCLOUD_GATEWAYS: gatewayurls(hostname1:port1,hostname2:port2)')
    print ('        --SCM_DEPLOY_NET: true/false, true is net')
    

def displayInfo(msg) :
   print ("========================================================================")
   print (msg)
   print ("========================================================================")
   
def displayError(msg) :
   print ("************************************************************************")
   print (msg)
   print ("************************************************************************")

#parse test_py.conf
def parseConfFile( scriptConfPath ):
    cf = ConfigParser.ConfigParser()
    #read config
    if not os.path.exists(scriptConfPath):
        displayError("ERROR local conf file not found:" + scriptConfPath)
        sys.exit(1)
    cf.read(scriptConfPath)
    #cf.read(os.path.abspath(os.path.join(sys.path[0], os.pardir,os.pardir))+"/misc/ci/src/conf/test_py.conf")
    global SCM_HOSTINFO, DB_INSTALL_DIR, SCM_INSTALL_DIR, HOSTLIST, SCM_DEPLOY_MODE, TEST_CASE_DIR,TESTCASETYPELIST 
    global EXEC_MODE, HOSTINFOS, HOSTUSERINFOS, HOSTPWDINFOS, TEST_IS_EXEC, MAIN_SITE_HOST, SCM_PORT, SDB_COORD_PORT
    global CONTROL_HOSTNAME, CONTROL_HOST_USER, CONTROL_HOST_PASSWD, SCMCLOUD_GATEWAYS, SCM_DEPLOY_NET
    DB_INSTALL_DIR = cf.get("common", "COMMON_SDB_INSTALL_DIR")
    SCM_INSTALL_DIR = cf.get("common", "COMMON_SCM_INSTALL_DIR")
    SCM_DEPLOY_MODE = cf.get("common", "COMMON_SCM_DEPLOY_MODE")
    EXEC_MODE = cf.get("test", "TEST_EXEC_MODE")
    TEST_IS_EXEC = cf.get("test", "TEST_IS_EXEC")
    TEST_CASE_DIR = cf.get("test", "TEST_CASE_DIR")
    CASE_TYPE = cf.get("test", "TEST_CASE_TYPE")
    MAIN_SITE_HOST = cf.get("common", "COMMON_MAIN_SITE_HOST")
    SCM_PORT = cf.get("common", "COMMON_SCM_PORT")
    SDB_COORD_PORT = cf.get("common", "COMMON_SDB_COORD_PORT")
    SCM_DEPLOY_NET = cf.get("scm", "SCM_DEPLOY_NET")
    COMMON_CI_CONTROL_HOSTINFO = cf.get("common", "COMMON_CI_CONTROL_HOSTINFO")
    controlHostInfo = COMMON_CI_CONTROL_HOSTINFO.split(':')
    CONTROL_HOSTNAME = controlHostInfo[0]
    CONTROL_HOST_USER = controlHostInfo[1]
    CONTROL_HOST_PASSWD = controlHostInfo[2]
    #get testcase type
    TESTCASETYPELIST = CASE_TYPE.split(',')
    #get host info
    SCM_HOSTINFO = cf.get("common", "COMMON_SCM_HOSTINFO")
    arrHostInfo = SCM_HOSTINFO.split(',')
    parseHostInfo(arrHostInfo)
    SCMCLOUD_GATEWAYS = cf.get("scmcloud", "SCMCLOUD_GATEWAYS")
    
def parseParameter( ):
    try:
        options,args = getopt.getopt(sys.argv[1:],"h",["help","COMMON_SCM_HOSTINFO=","COMMON_SDB_INSTALL_DIR=","COMMON_SCM_INSTALL_DIR=","COMMON_SCM_DEPLOY_MODE=","TEST_EXEC_MODE=","TEST_IS_EXEC=","COMMON_MAIN_SITE_HOST=", "COMMON_SCM_PORT=", "COMMON_SDB_COORD_PORT=", "COMMON_SCRIPT_CONF_PATH=", "TEST_CASE_DIR=", "TEST_CASE_TYPE=", "COMMON_CI_CONTROL_HOSTINFO=", "SCMCLOUD_GATEWAYS=","SCM_DEPLOY_NET=","COMMON_S3_ACCESSKEY=","COMMON_S3_SECRETKEY="])
    except getopt.GetoptError,e:
        print "Error:",e
        sys.exit(1)
    global SCM_HOSTINFO, DB_INSTALL_DIR, SCM_INSTALL_DIR, HOSTLIST, SCM_DEPLOY_MODE, SCRIPT_CONF_PATH, TESTCASETYPELIST  
    global EXEC_MODE, HOSTINFOS, HOSTUSERINFOS, HOSTPWDINFOS, TEST_IS_EXEC, MAIN_SITE_HOST, SCM_PORT, SDB_COORD_PORT
    global CONTROL_HOSTNAME, CONTROL_HOST_USER, CONTROL_HOST_PASSWD, SCMCLOUD_GATEWAYS, SCM_DEPLOY_NET, S3_ACCESSKEY, S3_SECRETKEY
    for name, value in options:
        if name=='--COMMON_SCRIPT_CONF_PATH':
            SCRIPT_CONF_PATH = value
            parseConfFile(SCRIPT_CONF_PATH)
    
    for name,value in options:
        if name in ("-h","--help"):
            printHelp()
            sys.exit(0)
        elif name=='--COMMON_SCM_HOSTINFO':
            SCM_HOSTINFO = value
            HOSTINFOS = []
            arrHostInfo = SCM_HOSTINFO.split(',')
            parseHostInfo(arrHostInfo)
        elif name=='--TEST_CASE_TYPE':
            CASE_TYPE = value
            TESTCASETYPELIST = []
            TESTCASETYPELIST = CASE_TYPE.split(',')
        elif name=='--COMMON_SDB_INSTALL_DIR':
            DB_INSTALL_DIR = value
        elif name=='--COMMON_SCM_INSTALL_DIR':
            SCM_INSTALL_DIR = value
        elif name=='--COMMON_SCM_DEPLOY_MODE':
            SCM_DEPLOY_MODE = value
        elif name=='--TEST_EXEC_MODE':
            EXEC_MODE = value
        elif name=='--TEST_IS_EXEC':
            TEST_IS_EXEC = value
        elif name=='--COMMON_MAIN_SITE_HOST':
            MAIN_SITE_HOST = value
        elif name=='--COMMON_SCM_PORT':
            SCM_PORT = value
        elif name=='--COMMON_SDB_COORD_PORT':
            SDB_COORD_PORT = value
        elif name=='--TEST_CASE_DIR':
            TEST_CASE_DIR = value
        elif name=='--COMMON_CI_CONTROL_HOSTINFO':
            COMMON_CI_CONTROL_HOSTINFO = value
            controlHostInfo = COMMON_CI_CONTROL_HOSTINFO.split(':')
            CONTROL_HOSTNAME = controlHostInfo[0]
            CONTROL_HOST_USER = controlHostInfo[1]
            CONTROL_HOST_PASSWD = controlHostInfo[2]
        elif name=='--SCMCLOUD_GATEWAYS':
            SCMCLOUD_GATEWAYS = value
        elif name=='--SCM_DEPLOY_NET':
            SCM_DEPLOY_NET=value
        elif name=='--COMMON_S3_ACCESSKEY':
            S3_ACCESSKEY=value
        elif name=='--COMMON_S3_SECRETKEY':
            S3_SECRETKEY=value
            
def parseHostInfo( arrhostinfo ):
    for i in range(len(arrhostinfo)):
        info = arrhostinfo[i].split(':')
        HOSTLIST.append(info[0])
        HOSTUSERINFOS[info[0]] = info[1]
        HOSTPWDINFOS[info[0]] = info[2]
        
        
#copy SCM_INSTALL_DIR/lib to testcase dir testcases/v2.0/story/java/lib
def copyRelyJar( scminstalldir ):
    displayInfo("Begin to copy scm related jar and testng related jar")
    os.system("rm -rf "+os.path.abspath(os.path.join(sys.path[0], os.pardir))+"/v2.0/testcase-base/lib/*driver.jar;");
    os.chdir(scminstalldir+"/driver")
    for file in glob.glob("sequoiacm-driver*.tar.gz"):
       os.system("tar -zxvf '"+file+"'")
       driverlibdir=file.split('-release')
    os.chdir(driverlibdir[0])
    for file in glob.glob("*.jar"):
       os.system("cp -rf '"+file+"' " +os.path.abspath(os.path.join(sys.path[0], os.pardir))+"/v2.0/testcase-base/lib")
    
    displayInfo("End copy scm related jar and testng related jar")
    
def divideTestngXml(scmdeploymode, controlhost, mainsitehost):
    displayInfo("Begin to divide testng.xml")
    testcaseDir = os.path.abspath(os.path.join(sys.path[0], os.pardir))+"/v2.0"
    for dir in os.listdir(testcaseDir):
        srcXmlFile="testng.xml"
        if SCM_DEPLOY_NET == "true":
            srcXmlFile="testng-net.xml"
        xmlDir = testcaseDir+"/"+dir+"/java/src/test/resources"
        if not os.path.exists(xmlDir+"/"+srcXmlFile):
            continue
        destgroup = ''
        gatewayurls=SCMCLOUD_GATEWAYS
        if int(scmdeploymode) == 4:
            hostNum = 2    
            destgroup = 'fourSite'
        elif int(scmdeploymode) == 2:
            hostNum = 2
            destgroup = 'twoSite'
        elif int(scmdeploymode) == 1:
            hostNum = 1
            destgroup = 'oneSite'
            arr = SCMCLOUD_GATEWAYS.split(',')
            gatewayurls=arr[0];
        elif controlhost == mainsitehost:
            hostNum = 1
            destgroup = 'fourSite'
        else :
            displayError("scmdeploymode is error!")
            sys.exit(1)
        shutil.copy(xmlDir+"/"+srcXmlFile,xmlDir+"/proxy.xml")        
        replaceTestngStr(xmlDir+"/proxy.xml", "XXXX", destgroup)
        replaceTestngStr(xmlDir+"/proxy.xml", "mainSiteHostName", mainsitehost)
        replaceTestngStr(xmlDir+"/proxy.xml", "GATEWAYURLS", gatewayurls)
        replaceTestngStr(xmlDir+"/proxy.xml", "S3USER", S3_ACCESSKEY)
        replaceTestngStr(xmlDir+"/proxy.xml", "S3PASSWORD", S3_SECRETKEY)
        shutil.copy(xmlDir+"/proxy.xml",xmlDir+"/testng_"+controlhost+".xml")
        shutil.copy(xmlDir+"/proxy.xml",xmlDir+"/testng_"+mainsitehost+".xml")
        replaceTestngStr(xmlDir+"/testng_"+controlhost+".xml", "localhost", controlhost)
        replaceTestngStr(xmlDir+"/testng_"+mainsitehost+".xml", "localhost", mainsitehost)
        
        #if serial testcase is exists,then  deal with testng-serial.xml
        srcXmlFile="testng-serial.xml"
        if SCM_DEPLOY_NET == "true":
            srcXmlFile="testng-net-serial.xml"
        if os.path.exists(xmlDir+"/"+srcXmlFile):
            shutil.copy(xmlDir+"/"+srcXmlFile,xmlDir+"/proxy-serial.xml")
            replaceTestngStr(xmlDir+"/proxy-serial.xml", "XXXX", destgroup)
            replaceTestngStr(xmlDir+"/proxy-serial.xml", "localhost", mainsitehost)
            replaceTestngStr(xmlDir+"/proxy-serial.xml", "mainSiteHostName", mainsitehost)
            replaceTestngStr(xmlDir+"/proxy-serial.xml", "GATEWAYURLS", gatewayurls)
            replaceTestngStr(xmlDir+"/proxy.xml", "S3USER", S3_ACCESSKEY)
            replaceTestngStr(xmlDir+"/proxy.xml", "S3PASSWORD", S3_SECRETKEY)
        if os.path.exists(xmlDir+"/testng_env.xml"):
            shutil.copy(xmlDir+"/testng_env.xml",xmlDir+"/proxy_env.xml")
            replaceTestngStr(xmlDir+"/proxy_env.xml", "XXXX", destgroup)
            replaceTestngStr(xmlDir+"/proxy_env.xml", "localhost", mainsitehost)
            replaceTestngStr(xmlDir+"/proxy_env.xml", "mainSiteHostName", mainsitehost)
            replaceTestngStr(xmlDir+"/proxy_env.xml", "GATEWAYURLS", gatewayurls)
        if os.path.exists(xmlDir+"/testng_env_before.xml"):
            replaceTestngStr(xmlDir+"/testng_env_before.xml", "localhost", mainsitehost)
            replaceTestngStr(xmlDir+"/testng_env_before.xml", "mainSiteHostName", mainsitehost)
            replaceTestngStr(xmlDir+"/testng_env_before.xml", "XXXX", destgroup)
            replaceTestngStr(xmlDir+"/testng_env_before.xml", "GATEWAYURLS", gatewayurls)
           
        
        #standalone or control site is main site ,then don't need to divide testng.xml,1 represent standalone
        if (int(scmdeploymode) <= 1) or (controlhost == mainsitehost):
            continue
        else : 
           #get all package name list
           dom = xml.dom.minidom.parse(xmlDir+"/proxy.xml")
           root = dom.documentElement
           itemlist = root.getElementsByTagName('package')
           exechostlist = [controlhost, mainsitehost]
           #divide package 
           for i in range(hostNum):
               for j in range(len(itemlist)):
                   item = itemlist[j]
                   name = item.getAttribute('name')
                   if (j%hostNum)!=i:
                       lines = []
                       file = open(xmlDir+"/testng_"+exechostlist[i]+".xml")
                       for line in file.xreadlines():
                           if not re.search("name=\""+name+"\"",line):
                               lines.append(line)
                       file.close()
                       file = open(xmlDir+"/testng_"+exechostlist[i]+".xml",'w')
                       file.writelines(lines)
                       file.close()                
    displayInfo("End divide testng.xml")
                        
def execTestcase():
    if EXEC_MODE == "p":
        displayInfo("Begin to run testcase parallel in different host")
        parallelExecTestcase();
        displayInfo("End run testcase parallel in different host")
        displayInfo("Begin to run serial testcase")
        execSerialTestng(MAIN_SITE_HOST, HOSTUSERINFOS[MAIN_SITE_HOST], HOSTPWDINFOS[MAIN_SITE_HOST])
        displayInfo("End to run serial testcase")
    else :
        displayInfo("Begin to run testcase parallel in one host")
        execTestcaseInControl()
        displayInfo("End run testcase parallel in one host")
        displayInfo("Begin to run serial testcase")
        execSerialTestng(MAIN_SITE_HOST, HOSTUSERINFOS[MAIN_SITE_HOST], HOSTPWDINFOS[MAIN_SITE_HOST])
        displayInfo("End to run serial testcase")
    
def distributeTestcase(scmdeploymode, controlhost, controlhostuser, controlhostpwd, mainsitehost, hostuserinfos, hostpwdinfos, testcasedir):
    displayInfo("Begin to distribute testcase to different host")
    srcDir = os.path.abspath(os.path.join(sys.path[0], os.pardir,os.pardir))
    cmd = "cd "+srcDir+";rm -rf testcase.tar.gz;tar -zcf "+srcDir+"/testcase.tar.gz ./testcase/"
    execCMD(controlhost, controlhostuser, controlhostpwd, cmd)

    if scmdeploymode == '1':
        needTestcaseHostNum = 1
        exechostlist = [mainsitehost]
    elif scmdeploymode == '2':
        needTestcaseHostNum = 1
        exechostlist = [mainsitehost]
    elif scmdeploymode == '4':
        needTestcaseHostNum = 1
        exechostlist = [ mainsitehost]
    else : 
        displayError("SCM_DEPLOY_MODE " + scmdeploymode + " is error!")
        sys.exit(1)
    for i in range(needTestcaseHostNum):
        cmd = "rm -rf "+testcasedir+"/testcase;rm -rf "+testcasedir+"/testcase.tar.gz"
        username = hostuserinfos[mainsitehost]
        passwd = hostpwdinfos[mainsitehost]
        execCMD(exechostlist[i], username, passwd, cmd)
        execScpFile(exechostlist[i], username, passwd, srcDir+"/testcase.tar.gz",testcasedir+"/testcase.tar.gz")
        cmd = "cd "+testcasedir+";tar -xzf ./testcase.tar.gz -C "+testcasedir+";rm -rf testcase.tar.gz;"
        execCMD(exechostlist[i], username, passwd, cmd)
    displayInfo("End to distribute testcase to different host")
    
def parallelExecTestcase():
    if SCM_DEPLOY_NET == "false":
        execBeforeTestcase()
    if (int(SCM_DEPLOY_MODE) >= 2) and (CONTROL_HOSTNAME != MAIN_SITE_HOST):
        thread1 = threading.Thread(target=execTestcaseInControl,name="execTestcaseInControl")
        thread2 = threading.Thread(target=execTestcaseInMainSite,name="execTestcaseInMainSite")
        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()
    else:
        thread1 = threading.Thread(target=execTestcaseInMainSite,name="execTestcaseInMainSite")
        thread1.start()
        thread1.join()
    
def execBeforeTestcase():
    testcaseDir = TEST_CASE_DIR+"/testcase/v2.0"
    os.chdir(testcaseDir+"/story/java")
    cmd = "cd "+testcaseDir+"/story/java; rm -rf "+TEST_CASE_DIR+"/test_clear.log; mvn surefire-report:report -DreportDir="+testcaseDir+"/story/java/test-env-before -DxmlFileName="+testcaseDir+"/story/java/src/test/resources/testng_env_before.xml"
    cmd = cmd+" >> "+TEST_CASE_DIR+"/test_clear.log 2>&1"
    displayInfo( CONTROL_HOSTNAME+" "+cmd )
    execCMD(MAIN_SITE_HOST, HOSTUSERINFOS[MAIN_SITE_HOST], HOSTPWDINFOS[MAIN_SITE_HOST], cmd)
    
def execTestcaseInControl():
    execTestngByDBSsh(CONTROL_HOSTNAME, CONTROL_HOST_USER, CONTROL_HOST_PASSWD)
    
def execTestcaseInMainSite():
    execTestngByDBSsh(MAIN_SITE_HOST, HOSTUSERINFOS[MAIN_SITE_HOST], HOSTPWDINFOS[MAIN_SITE_HOST])
    
def execTestngByDBSsh(testhost, testhostuser, testhostpasswd):
    testcaseDir = TEST_CASE_DIR+"/testcase/v2.0"
    for dir in os.listdir(testcaseDir):
        if dir == ".svn" or dir == "tdd":
            continue
        if dir not in TESTCASETYPELIST:
            continue
        os.chdir(testcaseDir+"/"+dir+"/java")
        if not os.path.exists(testcaseDir+"/"+dir+"/java/src/test/resources/testng_"+testhost+".xml"):
            continue
        cmd = "cd "+testcaseDir+"/"+dir+"/java; rm -rf "+TEST_CASE_DIR+"/test_"+dir+"_normal.log; mvn surefire-report:report -DreportDir="+testcaseDir+"/"+dir+"/java/test-output-parallel -DxmlFileName="+testcaseDir+"/"+dir+"/java/src/test/resources/testng_"+testhost+".xml"
        
        cmd = cmd+" >> "+TEST_CASE_DIR+"/test_"+dir+"_normal.log 2>&1"
        execCMD(testhost, testhostuser, testhostpasswd, cmd)
        
def execSerialTestng(testhost, testhostuser, testhostpasswd):
    testcaseDir = TEST_CASE_DIR+"/testcase/v2.0"
    for dir in os.listdir(testcaseDir):
        if dir == ".svn":
            continue
        if dir not in TESTCASETYPELIST:
            continue
        else :
           os.chdir(testcaseDir+"/"+dir+"/java")           
           cmd2= "cd "+testcaseDir+"/"+dir+"/java; rm -rf "+TEST_CASE_DIR+"/test_"+dir+"_serial.log; mvn surefire-report:report -DreportDir="+testcaseDir+"/"+dir+"/java/test-output -DxmlFileName="+testcaseDir+"/"+dir+"/java/src/test/resources/proxy-serial.xml  >> "+TEST_CASE_DIR+"/test_"+dir+"_serial.log 2>&1"
           
           if not os.path.exists(testcaseDir+"/"+dir+"/java/src/test/resources/proxy-serial.xml"):
               continue
           else : 
               execCMD(testhost, testhostuser, testhostpasswd, cmd2) 
           if SCM_DEPLOY_NET == "false":
               cmd3="cd "+testcaseDir+"/"+dir+"/java; rm -rf "+TEST_CASE_DIR+"/test_"+dir+"_env.log; mvn surefire-report:report -DreportDir="+testcaseDir+"/"+dir+"/java//test-output-env -DxmlFileName="+testcaseDir+"/"+dir+"/java/src/test/resources/proxy_env.xml  >> "+TEST_CASE_DIR+"/test_"+dir+"_env.log 2>&1"
               execCMD(testhost, testhostuser, testhostpasswd, cmd3)
               
    
#repalce testng.xml's
def replaceTestngStr(xmlfilename, srcstr, deststr):
    f = open(xmlfilename , 'r')
    lines = f.readlines()
    f.close()
    f = open(xmlfilename, 'w+')
    for eachline in lines:
        newline = re.sub(srcstr, deststr, eachline)
        f.writelines(newline)
    f.close()
    
def execCMD(testhost, user, passwd, cmd):
    dbSshCmd = DB_INSTALL_DIR+'/bin/sdb -s \'ssh = new Ssh("'+testhost+'","'+user+'","'+passwd+'");ssh.exec("'+cmd+'");print(ssh.getLastOut());println(ssh.getLastRet())\''
    displayInfo( dbSshCmd )
    (status,output) = commands.getstatusoutput(dbSshCmd)
    if status!=0:
        displayError('exec cmd:'+dbSshCmd+'\r\nerrorMsg:'+output)
        
def execScpFile(desthost, destuser, destpasswd, srcpath, destpath):
    dbSshCmd = DB_INSTALL_DIR+'/bin/sdb -s \'ssh = new Ssh("'+desthost+'","'+destuser+'","'+destpasswd+'");ssh.push("'+srcpath+'","'+destpath+'",0755);print(ssh.getLastOut());println(ssh.getLastRet())\''
    displayInfo( dbSshCmd )
    (status,output) = commands.getstatusoutput(dbSshCmd)
    if status!=0:
        displayError('exec cmd:'+dbSshCmd+'\r\nerrorMsg:'+output)
        sys.exit(1)
        
def execTdd():
    displayInfo("Begin to exec tdd testcase.")
    testcaseDir = os.path.abspath(os.path.join(sys.path[0], os.pardir))+"/v2.0"
    xmlDir = testcaseDir+"/tdd/java/src/test/resources"
    arr = SCMCLOUD_GATEWAYS.split(',')
    gatewayurls=arr[0];
    replaceTestngStr(xmlDir+"/testng.xml", "server1ip:server1port",  gatewayurls+"/rootsite")
    replaceTestngStr(xmlDir+"/testng.xml", "server2ip:server2port",  gatewayurls+"/branchsite1")
    replaceTestngStr(xmlDir+"/testng.xml", "server3ip:server3port",  gatewayurls+"/branchsite2")
    replaceTestngStr(xmlDir+"/testng.xml", "sdb1ip:sdb1port",  MAIN_SITE_HOST+":11810")
    replaceTestngStr(xmlDir+"/testng.xml", "sdb2ip:sdb2port",  MAIN_SITE_HOST+":11810")
    replaceTestngStr(xmlDir+"/testng.xml", "sdb3ip:sdb3port",  MAIN_SITE_HOST+":11810")
    os.chdir(testcaseDir+"/tdd/java")
    cmd = "cd "+testcaseDir+"/tdd/java; rm -rf "+TEST_CASE_DIR+"/test_tdd_normal.log; mvn surefire-report:report -DreportDir="+testcaseDir+"/tdd/java/test-output-parallel -DxmlFileName="+testcaseDir+"/tdd/java/src/test/resources/testng.xml"
  
    cmd = cmd+" >> "+TEST_CASE_DIR+"/test_tdd_normal.log 2>&1"
    displayInfo( CONTROL_HOSTNAME+" "+cmd )
    execCMD(CONTROL_HOSTNAME, CONTROL_HOST_USER, CONTROL_HOST_PASSWD, cmd)
    
if __name__=="__main__":
    parseParameter( )
    copyRelyJar( SCM_INSTALL_DIR )
    divideTestngXml(SCM_DEPLOY_MODE, CONTROL_HOSTNAME, MAIN_SITE_HOST)
    distributeTestcase(SCM_DEPLOY_MODE, CONTROL_HOSTNAME, CONTROL_HOST_USER, CONTROL_HOST_PASSWD, MAIN_SITE_HOST, HOSTUSERINFOS, HOSTPWDINFOS, TEST_CASE_DIR)
    if TEST_IS_EXEC=="true":
        execTestcase()
    #TEST_CASE_TYPE contains tdd
    if "tdd" in TESTCASETYPELIST:
        execTdd()
        
    