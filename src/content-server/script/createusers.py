#!/usr/bin/python
import commands
import json
import os
import sys
import getopt

SCM_ADMIN = "scmadmin.sh"
dry_run = 0
rootDir = sys.path[0]
BIN_PATH = rootDir + os.sep + "bin"

def display(name):
    print('usage: %s [options]...' % name)
    print("")
    print("options:")
    print("\t-h, --help         print help information")
    print("\t--bin              specify scm bin path, default is '%s'" % BIN_PATH)
    print("\t--dryrun           output command but not execute")
    sys.exit(0)

def command(cmd):
    print(cmd)
    if dry_run == 1:
        return
    
    (status, output) = commands.getstatusoutput(cmd)
    if not len(output) == 0:
        print(output)
    if status != 0:
        raise Exception("Failed to execute command: %s" % cmd)

def scm_admin(cmd):
    command("%s%s%s %s" % (BIN_PATH, os.sep, SCM_ADMIN, cmd))

def create_user(url, adminUser, adminPasswd, newUser):
    cmd = 'createuser --new-user ' + newUser['name'] + ' --new-password ' + newUser['password']
    cmd += ' --url "' + url + '" --user ' + adminUser + ' --password ' + adminPasswd
    scm_admin(cmd)

def create_role(url, adminUser, adminPasswd, newRole):
    cmd = 'createrole --role ' + newRole['name']
    cmd += ' --url "' + url + '" --user ' + adminUser + ' --password ' + adminPasswd
    scm_admin(cmd)
    
def grant_role(url, adminUser, adminPasswd, roleName, resource):
    cmd = 'grantrole --role ' + roleName + ' --type ' + resource['resourceType'] + ' --resource ' + resource['resource'] + ' --privilege ' + resource['privilege']
    cmd += ' --url "' + url + '" --user ' + adminUser + ' --password ' + adminPasswd
    scm_admin(cmd)
    
def attach_role(url, adminUser, adminPasswd, userName, roleName):
    cmd = 'attachrole --attached-user ' + userName + ' --role ' + roleName
    cmd += ' --url "' + url + '" --user ' + adminUser + ' --password ' + adminPasswd
    scm_admin(cmd)

def load_config(conf_file):
    f = open(conf_file, 'r')
    try:
        j = f.read()
        config = json.loads(j)
        return config
    finally:
        f.close()
        
def create_all_users(conf):
    url = conf['url']
    adminUser = conf['adminUser']
    adminPasswd = conf['adminPassword']
    
    for oneRole in conf['roles']:
        create_role(url, adminUser, adminPasswd, oneRole)
        for oneResource in oneRole['resources']:
            grant_role(url, adminUser, adminPasswd, oneRole['name'], oneResource)
    
    for oneUser in conf['newUsers']:
        create_user(url, adminUser, adminPasswd, oneUser)
        for roleName in oneUser['roles']:
            attach_role(url, adminUser, adminPasswd, oneUser['name'], roleName)
    
    for oneUser in conf['oldUsers']:
        for roleName in oneUser['roles']:
            attach_role(url, adminUser, adminPasswd, oneUser['name'], roleName)

def main(argv):
    global BIN_PATH
    global dry_run
    
    try:
        options, args = getopt.getopt(sys.argv[1:], "h", ["help", "bin=", "dryrun"])
    except getopt.GetoptError, e:
        print "Error:", e
        sys.exit(-1)
        
    for name, value in options:
        if name in ("-h", "--help"):
            display(argv[0])
        elif name == "--bin":
            BIN_PATH = value
        elif name == "--dryrun":
            dry_run = 1

    config = rootDir + os.sep + "users.json"
    conf = load_config(config)
    create_all_users(conf)

if __name__ == '__main__':
    main(sys.argv)




