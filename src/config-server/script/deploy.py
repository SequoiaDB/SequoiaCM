#!/usr/bin/python
import commands
import json
import os
import sys
import getopt
import socket

CONF_ADMIN = "confadmin.sh"
CONF_CTL = "confctl.sh"
dry_run = False
rootDir = sys.path[0]
BIN_PATH = rootDir + os.sep + "bin"
node_has_create = False


def command(cmd):
    print(cmd)
    if dry_run:
        return
    (status, output) = commands.getstatusoutput(cmd)
    if not len(output) == 0:
        print(output)
    if status != 0:
        raise Exception("Failed to execute command: %s" % cmd)


def scm_admin(cmd):
    command("%s%s%s %s" % (BIN_PATH, os.sep, CONF_ADMIN, cmd))


def scm_ctl(cmd):
    command("%s%s%s %s" % (BIN_PATH, os.sep, CONF_CTL, cmd))


def load_config(conf_file):
    f = open(conf_file, 'r')
    try:
        j = f.read()
        config = json.loads(j)
        return config
    finally:
        f.close()


def create_node(type, config):
    cmd = 'createnode --type ' + type
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

def create_nodes(type, config):
    global node_has_create
    for ele in config:
        if "hostname" in ele and hostAdaptor(ele.pop("hostname")) or "hostname" not in ele:
           node_has_create = True
           create_node(type, ele)


def deploy_scm(config, bin_path="." + os.sep + "bin", dryrun=False):
    global BIN_PATH
    global dry_run
    dry_run = dryrun
    if bin_path is not None:
        BIN_PATH = bin_path
    if 'config-server' in config:
        create_nodes('config-server', config['config-server'])


def print_help(name):
    print('usage: %s [option]...' % name)
    print("")
    print("Options:")
    print("\t-h, --help     print help information")
    print("\t-c, --conf     specify config file path, default is 'deploy.json'")
    print("\t-b, --bin      specify scm bin path, default is '%s'" % BIN_PATH)
    print("\t-s, --start    start all nodes")
    print("\t--dryrun       output command but not execute")


def start_node(port=0):
    cmd = "start"
    if port == 0:
        cmd += " --type all"
    else:
        cmd += " --port " + str(port)
    scm_ctl(cmd)


def main(argv):
    config = rootDir + os.sep + "deploy.json"
    bin_path = None
    start = False
    dryrun = False
    try:
        opts, args = getopt.getopt(argv[1:], "hc:b:s",
                                   ["help", "conf=", "bin=", "start", "dryrun"])
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
            dryrun = True
    conf = load_config(config)
    deploy_scm(conf, bin_path, dryrun)
    if not node_has_create:
        print("no node was created!")
        sys.exit(-2)
    if start:
        start_node()


if __name__ == '__main__':
    main(sys.argv)

