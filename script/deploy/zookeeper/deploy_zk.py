#!/usr/bin/python
import commands
import json
import os
import sys
import getopt
import socket

rootDir = sys.path[0]

ZK_PACKAGE = rootDir + os.sep + "thirdparty" + os.sep + "zookeeper" + os.sep + "zookeeper-3.4.12.tar.gz"
ZK_HOME = rootDir + os.sep + "deploy" + os.sep + "zookeeper" + os.sep
ZK_SERVER_SHELL = ZK_HOME + "bin" + os.sep + "zkServer.sh"
ZK_CONF_PATH = ZK_HOME + "conf" + os.sep
ZK_DATA_PATH = ZK_HOME + "data" + os.sep
#ZK_DATALOG_PATH = ZK_HOME + os.sep + "logs"

servers = {}
zk_cfg = []


def display_info(msg):
    print("========================================================================")
    print(msg)
    print("========================================================================")

def execCMD(cmd):
    print "[INFO] " + cmd
    ret = os.system(cmd)
    if ret != 0:
        sys.exit(ret)

def dict2Str(dict):
    str = ""
    for key in dict:
        str += key + "=" + dict[key] + "\n"
    return str

def convert_localhost(url):
    u = url.split(':')
    if u[0] == "localhost" or u[0] == "127.0.0.1":
        local = socket.gethostname()
        return local + ":" + u[1] + ":" + u[2]
    return url
    
def update_param(key, newVal, file):
    execCMD("sed -i 's/%s=.*/%s=%s/g' %s" % (key, key, newVal, file))

def touch_myid(data_dir, myid):
    execCMD("echo %d > %s" % (myid, data_dir + os.sep + "myid"))

def hostAdaptor(hostname):
    local_hostname = socket.gethostname()
    local_hostip = socket.gethostbyname(local_hostname)
    if hostname == "localhost" or hostname == "127.0.0.1" or hostname == local_hostname or hostname == local_hostip:
        return True
    else:
        return False
    
def modify_config(myid, config):
    server = config["server"]
    servers["server.%d" % myid] = convert_localhost(server)
    hostname = server.split(":")[0];
    if hostAdaptor(hostname) == False:
        return
        
    sample_cfg = ZK_CONF_PATH + "zoo_sample.cfg"
    copy_cfg = ZK_CONF_PATH + "zoo" + str(myid) + ".cfg"
    execCMD("cp %s %s" % (sample_cfg, copy_cfg))
    zk_cfg.append(copy_cfg)
    for key in config:
        update_param(key, config[key], copy_cfg)
    # dataDir
    data_dir = ZK_DATA_PATH + str(myid)
    execCMD("mkdir -p %s" % data_dir)
    update_param("dataDir", data_dir.replace(os.sep, "\\" + os.sep), copy_cfg)
    # myid file
    touch_myid(data_dir, myid)
    # dataLogDir
    #datalog_dir = ZK_DATALOG_PATH + os.sep + config['clientPort']
    #execCMD("mkdir %s" % datalog_dir)
    #update_param("dataLogDir", data_dir, "zoo%d.cfg" % myid)

def install_zk(config):
    stop_zk(len(config['zookeeper-server']))

    display_info("Begin to install zookeeper...")
    if os.path.exists(ZK_HOME):
        execCMD("rm -rf %s/*" % ZK_HOME)
    else:
        print("[INFO] %s is not exist, ignore to clean" % ZK_HOME)
        execCMD("mkdir -p %s" % ZK_HOME)

    os.chdir(rootDir)

    # unzip
    execCMD("tar -zxf %s -C %s" % (ZK_PACKAGE, os.path.dirname(ZK_PACKAGE)))
    zk_tmpdir = ZK_PACKAGE.split(".tar.gz")[0]

    # move zookeeper to ZK_HOME
    execCMD("mv %s/* %s" % (zk_tmpdir, ZK_HOME))
    execCMD("rm -rf %s" % zk_tmpdir)

    if 'zookeeper-server' in config:
        for index, item in enumerate(config['zookeeper-server']):
            myid = item['myid']
            modify_config(myid, item)

        server_params = dict2Str(servers)

        for cfg in zk_cfg:
            execCMD("echo \"%s\" >> %s" % (server_params, cfg))


def start_zk():
    display_info("Begin to start zookeeper...")
    for cfg in zk_cfg:
        execCMD("%s start %s" % (ZK_SERVER_SHELL, cfg))

def stop_zk(count):
    display_info("Begin to stop zookeeper...")
    if os.path.exists(ZK_SERVER_SHELL):
        for id in range(count):
            execCMD("%s stop zoo%d.cfg" % (ZK_SERVER_SHELL, id+1))
    else:
        print("[INFO] %s is not exist, ignore to stop"  % ZK_SERVER_SHELL)

def status_zk():
    display_info("Begin to show zookeeper status...")
    for cfg in zk_cfg:
        execCMD("%s status %s" % (ZK_SERVER_SHELL, cfg))

def load_config(conf_file):
    f = open(conf_file, 'r')
    try:
        j = f.read()
        config = json.loads(j)
        return config
    finally:
        f.close()

def print_help(name):
    print('usage: %s [option]...' % name)
    print("")
    print("Options:")
    print("\t-h, --help     print help information")
    print("\t-c, --conf     specify config file path, default is '%sdeploy_zk.json'" % (rootDir + os.sep))
    #print("\t--install      install zookeeper (install path: %s)" % ZK_HOME)
    #print("\t--start        start all nodes")
    #print("\t--stop         stop all nodes")
    #print("\t--status       show nodes status")
    print("\t--zkhome       zookeeper install path(will be cleared),default:")
    print("\t               %s" % ZK_HOME)
    print("\t--zkpk         zookeeper package(.tar.gz) path for install,default:")
    print("\t               %s" % ZK_PACKAGE)

def main(argv):
    global ZK_PACKAGE, ZK_HOME, ZK_SERVER_SHELL, ZK_CONF_PATH, ZK_DATA_PATH
    config = rootDir + os.sep + "deploy_zk.json"
    #install = False
    #start = False
    #stop = False
    #status = False

    try:
        opts, args = getopt.getopt(argv[1:], "hc:", ["help", "conf=", "zkhome=","zkpk="])
    except getopt.GetoptError:
        print_help(argv[0])
        sys.exit(1)

    for opt, arg in opts:
        if opt in ("-h", "--help"):
            print_help(argv[0])
            sys.exit(0)
        elif opt in ("-c", "--conf"):
            config = arg
        elif opt == "--zkhome":
            ZK_HOME = os.path.abspath(arg) + os.sep
            ZK_SERVER_SHELL = ZK_HOME + "bin" + os.sep + "zkServer.sh"
            ZK_CONF_PATH = ZK_HOME + "conf" + os.sep
            ZK_DATA_PATH = ZK_HOME + "data" + os.sep
        elif opt == "--zkpk":
            ZK_PACKAGE = arg
        '''
        elif opt == "--install":
            install = True
        elif opt == "--start":
            start =True
        elif opt == "--stop":
            stop = True
        elif opt == "--status":
            status = True
        '''

    conf = load_config(config)
    install_zk(conf)
    start_zk()

if __name__ == '__main__':
    main(sys.argv)

