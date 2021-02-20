#!/usr/bin/python
import commands
import json
import os
import sys
import getopt
import socket

SCM_ADMIN = "scmcloudadmin.sh"
SCM_CTL = "scmcloudctl.sh"
root_dir = sys.path[0]
bin_path = root_dir + os.sep + "bin"
dry_run = False
clean_systable = False
node_has_create = False

def command(cmd):
    print(cmd)
    if dry_run:
        return
    (status, output) = commands.getstatusoutput(cmd)
    if not len(output) == 0:
        print(output)
    if status != 0:
        raise Exception("Failed to execute command: " % cmd)


def scm_admin(cmd):
    command("%s%s%s %s" % (bin_path, os.sep, SCM_ADMIN, cmd))


def scm_ctl(cmd):
    command("%s%s%s %s" % (bin_path, os.sep, SCM_CTL, cmd))


def load_config(conf_file):
    f = open(conf_file, 'r')
    try:
        j = f.read()
        config = json.loads(j)
        return config
    finally:
        f.close()


def clean_system_table(node_type, config):
    if node_type == 'admin-server' or node_type == 'auth-server':
        clear_systable(node_type, config['scm.store.sequoiadb.urls'], config['scm.store.sequoiadb.username'], config['scm.store.sequoiadb.password'])


def clear_systable(node_type, sdb_url, sdb_user, sdb_password_file_path):
    cmd = 'cleansystable --type ' + node_type + ' --url ' + sdb_url
    if sdb_user != None and len(sdb_user.strip()) != 0:
        cmd = cmd + ' --user ' + sdb_user
    if sdb_password_file_path != None and len(sdb_password_file_path.strip()) != 0:
        cmd = cmd + ' --password-file ' + sdb_password_file_path
    scm_admin(cmd)
    

def create_node(node_type, auditconf, config):
    cmd = 'createnode --type ' + node_type
    # audit
    adurl = auditconf['auditurl']
    aduser = auditconf['audituser']
    adpasswd = auditconf['auditpassword']
    cmd += ' --adurl ' + adurl + " --aduser " + aduser + " --adpasswd " + adpasswd

    for key in config:
        cmd += ' -D'+key+'='+config[key]
    scm_admin(cmd)

def hostAdaptor(hostname):
    local_hostname = socket.gethostname()
    local_hostip = socket.gethostbyname(local_hostname)
    if hostname == "localhost" or hostname == "127.0.0.1" or hostname == local_hostname or hostname == local_hostip:
        return True
    else:
        return False
        
def create_nodes(node_type, auditconf, config):
    global node_has_create
    if not isinstance(config, list):
        raise Exception('Invalid node config: %s', str(config))
    if len(config) > 0 and clean_systable:
        clean_system_table(node_type, config[0])
    for ele in config:
       if "hostname" in ele and hostAdaptor(ele.pop("hostname")) or "hostname" not in ele:
           node_has_create = True
           create_node(node_type, auditconf, ele)

def deploy_scm(config):
    auditconf = config['audit']
    if 'serviceCenter' in config:
        create_nodes('service-center', auditconf, config['serviceCenter'])
    if 'authServer' in config:
        create_nodes('auth-server', auditconf, config['authServer'])
    if 'gateway' in config:
        create_nodes('gateway', auditconf, config['gateway'])
    if 'serviceTrace' in config:
        create_nodes('service-trace', auditconf, config['serviceTrace'])
    if 'adminServer' in config:
        create_nodes('admin-server', auditconf, config['adminServer'])

def print_help(name):
    print('usage: %s [option]...' % name)
    print("")
    print("Options:")
    print("\t-h, --help         print help information")
    print("\t-c, --conf         specify config file path, default is 'deploy.json'")
    print("\t-b, --bin          specify scm bin path, default is '%s'" % bin_path)
    print("\t-s, --start        start all nodes")
    print("\t--cleansystable    clean system table")
    print("\t--dryrun           output command but not execute")


def start_node(port=0):
    cmd = "start"
    if port == 0:
        cmd += " --type all"
    else:
        cmd += " --port " + str(port)
    scm_ctl(cmd)


def main(argv):
    global bin_path
    global dry_run
    global clean_systable

    config = root_dir + os.sep + "deploy.json"
    start = False
    try:
        opts, args = getopt.getopt(argv[1:], "hc:b:s",
                                   ["help", "conf=", "bin=", "start", "cleansystable", "dryrun"])
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
            bin_path = arg
        elif opt in ("-s", "--start"):
            start = True
        elif opt == "--dryrun":
            dry_run = True
        elif opt == "--cleansystable":
            clean_systable = True
    conf = load_config(config)

    
    deploy_scm(conf)
    if not node_has_create:
        print("no node was created!")
        sys.exit(-2)
    if start:
        start_node()


if __name__ == '__main__':
    main(sys.argv)
