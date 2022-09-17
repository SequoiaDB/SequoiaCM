#!/usr/bin/python

import json
import os
import platform
import sys
import getopt
import socket
import time

rootDir = sys.path[0]
BIN_PATH = rootDir + os.sep + "bin"
SCM_ADMIN = "scmadmin.sh"
SCM_CTL = "scmctl.sh"

if platform.system() == "Windows":
    SCM_ADMIN = "scmadmin.bat"
    SCM_CTL = "scmctl.bat"

dry_run = False


def command(cmd):
    print(cmd)
    if dry_run:
        return
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("Failed to exec cmd:" + cmd)


def scm_admin(cmd):
    command("%s%s%s %s" % (BIN_PATH, os.sep, SCM_ADMIN, cmd))


def load_config(conf_file):
    f = open(conf_file, 'r')
    try:
        j = f.read()
        config = json.loads(j)
        return config
    finally:
        f.close()

def create_workspace(ws_conf, url, user, password):
    name = ws_conf['name']
    cmd = "createws --name " + name
    if 'meta' in ws_conf:
        meta = ws_conf['meta']
        cmd += " --meta '%s'" % json.dumps(meta)
    if 'data' in ws_conf:
        data = ws_conf['data']
        cmd += " --data '%s'" % json.dumps(data)
    if 'description' in ws_conf:
        desc = ws_conf['description']
        cmd += " --description " + desc
    if 'batch_sharding_type' in ws_conf:
        batch_sharding_type = ws_conf['batch_sharding_type']
        cmd += " --batch-sharding-type " + batch_sharding_type
    if 'batch_id_time_regex' in ws_conf:
        batch_id_time_regex = ws_conf['batch_id_time_regex']
        cmd += " --batch-id-time-regex '" + batch_id_time_regex + "'"
    if 'batch_id_time_pattern' in ws_conf:
        batch_id_time_pattern = ws_conf['batch_id_time_pattern']
        cmd += " --batch-id-time-pattern " + batch_id_time_pattern
    if 'batch_file_name_unique' in ws_conf:
        batch_file_name_unique = ws_conf['batch_file_name_unique']
        if batch_file_name_unique:
            cmd += " --batch-file-name-unique"
    if 'enable_directory' in ws_conf:
        enable_directory = ws_conf['enable_directory']
        if not enable_directory:
            cmd += " --disable-directory"
    if 'preferred' in ws_conf:
        preferred = ws_conf['preferred']
        cmd += " --preferred " + preferred
    if 'site_cache_strategy' in ws_conf:
        site_cache_strategy = ws_conf['site_cache_strategy']
        cmd += " --site-cache-strategy " + site_cache_strategy
    
    cmd += ' --user ' + user
    cmd += ' --password ' + password
    cmd += ' --url ' + url
    scm_admin(cmd)

def create_workspaces(wss_conf, url='localhost:8080/rootsite', user='admin', password='admin'):
    for ws_conf in wss_conf:
        create_workspace(ws_conf, url, user, password)

def deploy(config):
    url = config['url']
    userName = config['userName']
    password = config ['password']
    if 'workspaces' in config:
        create_workspaces(config['workspaces'], url, userName, password)
        
def print_help(name):
    print('usage: %s [option]...' % name)
    print("")
    print("Options:")
    print("\t-h, --help     print help information")
    print("\t-c, --conf     specify config file path, default is 'workspaces.json'")
    print("\t-b, --bin      specify scm bin path, default is '%s'" % BIN_PATH)
    print("\t--dryrun       output command but not execute")


def main(argv):
    global BIN_PATH
    global dry_run

    config = rootDir + os.sep +  "workspaces.json"
    try:
        opts, args = getopt.getopt(argv[1:], "hc:b:s",
                                   ["help", "conf=", "bin=", "dryrun"])
    except getopt.GetoptError:
        print_help(argv[0])
        sys.exit(2)
    for opt, arg in opts:
        if opt in ("-h", "--help"):
            print_help(argv[0])
            sys.exit(0)
        elif opt in ("-c", "--conf"):
            config = arg
        elif opt in ("-b", "--bin"):
            BIN_PATH = arg
        elif opt == "--dryrun":
            dry_run = True
    conf = load_config(config)
    deploy(conf)

if __name__ == '__main__':
    main(sys.argv)
