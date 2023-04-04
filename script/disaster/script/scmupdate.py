#!/usr/bin/python

import platform
import commands
import sys
import os
import json
import getopt
import socket
import tarfile
import glob
import fnmatch


rootDir = sys.path[0]


def display(name):
    print('Usage: %s [option] ...' %name)
    print ""
    print "Options:"
    print " --help | -h"
    print " [--updatemeta] [--updateconf]"
    print " [--dryrun] "
    print ""
    print " --help | -h       : print help message"
    print " --updatemeta      : update the root site meta data"
    print " --updateconf      : update the SCM node configuration file"
    print " --dryrun          : output command but not execute"


def main(argv):
    global NEED_UPDATEMETA,NEDD_UPDATECONF,conf_path,dry_run
    NEED_UPDATEMETA = False
    NEDD_UPDATECONF = False
    conf_path = rootDir + os.sep +  "scmupdate.json"
    dry_run = False
    try:
        opts, args = getopt.getopt(argv[1:],"h:",["help", "updatemeta", "updateconf", "dryrun"])
    except getopt.GetoptError:
        display(argv[0])
        sys.exit(2)
    for opt,arg in opts:
        if opt in ("-h","--help"):
            display(argv[0])
            sys.exit(0)
        elif opt == "--updatemeta":
            NEED_UPDATEMETA = True
        elif opt == "--updateconf":
            NEDD_UPDATECONF = True
        elif opt == "--dryrun":
            dry_run = True
    conf = load_config(conf_path)
    global sdbpath,coordhost,coordport,scmpath,sdbuser,sdbpasswd
    sdbpath=conf["sdbpath"]
    coordhost=conf["coordhost"]
    coordport=conf["coordport"]
    sdbuser=conf["sdbuser"]
    sdbpasswd=conf["sdbpassword"]
    scmpath=conf["scmpath"]
    if NEED_UPDATEMETA:
        updatemeta(conf["rootsite"])
    if NEDD_UPDATECONF:
        updateconf(conf["config"])
        
def command(cmd,is_print):
    if is_print:
        print cmd
    if dry_run:
        return
    (status, output) = commands.getstatusoutput(cmd)
    if not len(output) == 0:
        print output
    if status != 0:
        raise Exception("Failed to execute cmd:"  +cmd+ '\r\n')

def exe_sdb_cmd(cmd_str):
    cmd=sdbpath+os.sep+"bin"+os.sep+"sdb -s \"\n" 
    sdbconnect_str="var db=new Sdb('"+coordhost+"',"+ coordport+",'"+sdbuser+"','"+sdbpasswd+"');\n"
    cmd+=sdbconnect_str
    cmd+=cmd_str
    cmd+="\""
    command(cmd, True);

def load_config(conf_file):
    f = open(conf_file,'r')
    try:
        j = f.read()
        config = json.loads(j)
        return config
    finally:
        f.close()
    
def updatemeta(rootsite_conf):
    print "update"
    cmd=""
    #db.SCMSYSTEM.SITE.update({"$set":{"meta.url":["r520-8:18100"]}},{"root_site_flag":true})
    metaurls=transconfig_set(rootsite_conf.get("meta.url"))
    update_meta_cmd = "db.SCMSYSTEM.SITE.update({'\$set':{'meta.url':"+metaurls+"}},{'root_site_flag':true});\n"
    cmd += update_meta_cmd
    dataurls=transconfig_set(rootsite_conf.get("data.url"))
    update_data_cmd = "db.SCMSYSTEM.SITE.update({'\$set':{'data.url':"+dataurls+"}},{'root_site_flag':true});\n"
    cmd += update_data_cmd
    exe_sdb_cmd(cmd)
    print "update success"

def transconfig_set(config):
    conf_str="["
    item_arr=config.split(",");
    for index in range(0,len(item_arr)):
        item=item_arr[index]
        if index==0:
            conf_str+="'"+item+"'"
        else:
            conf_str+=",'"+item+"'"
    conf_str+="]"
    return conf_str
    
def updateconf(conf):
    print "update"
    print ""
    conf_file_list=scan_files(scmpath,all="application.properties");
    for index in range(len(conf_file_list)):
        filepath=conf_file_list[index];
        update_conf_file(filepath,conf)
       
    log_file_list=scan_files(scmpath,all="logback.xml");
    for index in range(len(log_file_list)):
        filepath=log_file_list[index];
        update_log_file(filepath,conf)
    print "update success"

def scan_files(directory,prefix=None,postfix=None,all=None):
    files_list=[]
    for root, sub_dirs, files in os.walk(directory):
        for special_file in files:
            if postfix:
                if special_file.endswith(postfix):
                    files_list.append(os.path.join(root,special_file))
            elif prefix:
                if special_file.startswith(prefix):
                    files_list.append(os.path.join(root,special_file))
            elif all:
                if special_file == all:
                    files_list.append(os.path.join(root,special_file))
            else:
                files_list.append(os.path.join(root,special_file))
    return files_list        

def update_conf_file(filepath,conf):
    confItems={"scm.store.sequoiadb.urls": conf.get("sdburls"),
        "scm.rootsite.meta.url":conf.get("sdburls"),
        "scm.zookeeper.urls":conf.get("zkurls")};
    keySet=confItems.keys();
    if dry_run:
        update_flag=False
        readfile = open(filepath,'r')
        for line in readfile.readlines():
            key=line.split("=")[0].strip();
            if key in keySet:
                if update_flag == False:
                    print filepath+" update config"
                    update_flag=True;
        if update_flag:
            print ""
        return;
    try:
        readfile = open(filepath,'r')
        writefile = open(filepath+'_new','w')
        update_flag=False
        for line in readfile.readlines():
            key=line.split("=")[0].strip();
            if key in keySet:
                if update_flag==False:
                    print filepath+" update config"
                    update_flag=True;
                line=key+"="+confItems.get(key)+'\n'
            writefile.write(line)
        readfile.close()
        writefile.close()
        if update_flag:
            overwrite(filepath,True)
            print ""
        else:
            deletenew(filepath,False)
    except Exception as e:
        readfile.close()
        writefile.close()
        deletenew(filepath,True)
        raise e;
        
        
def overwrite(filpath,is_print):
    command('mv -f '+filpath+'_new '+filpath,is_print);

def deletenew(filpath,is_print):
    command('rm -f '+filpath+'_new ',is_print);

def update_log_file(filepath,conf):
    audit_url_key="AUDIT_SDB_URL";
    audit_url_value=conf.get("sdburls");
    if dry_run:
        update_flag=False
        readfile = open(filepath,'r')
        for line in readfile.readlines():
            if line.find(audit_url_key) >= 0 and line.find('property') >= 0 :
                if update_flag==False:
                    print filepath+" update config"
                    update_flag=True;
        if update_flag:
            print ""
        return;

    try:
        readfile = open(filepath,'r')
        writefile = open(filepath+'_new','w')
        update_flag=False
        for line in readfile.readlines():
            if line.find(audit_url_key) >= 0 and line.find('property') >=0 :
                if update_flag==False:
                    print filepath+" update config"
                    update_flag=True;
                tmpline="    <property name=\""+audit_url_key+"\" value=\""+audit_url_value+"\" />"
                line=tmpline+'\n'
            writefile.write(line)
        readfile.close()
        writefile.close()
        if update_flag:
            overwrite(filepath,True)
            print ""
        else:
            deletenew(filepath,False)
    except Exception as e:
        readfile.close()
        writefile.close()
        deletenew(filepath,True)
        raise e;

if __name__=='__main__':
    main(sys.argv)

