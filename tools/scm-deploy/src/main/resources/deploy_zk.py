#!/usr/bin/python
import json
import os
import sys
import getopt
import socket

rootDir = sys.path[0] 

ZK_HOME = rootDir + os.sep
ZK_SERVER_SHELL = ZK_HOME + "bin" + os.sep + "zkServer.sh"
ZK_CONF_PATH = ZK_HOME + "conf" + os.sep
ZK_DATA_PATH = ZK_HOME + "data" + os.sep
#ZK_DATALOG_PATH = ZK_HOME + os.sep + "logs"
START_ZK = False
node_has_create = False
servers = {}


def display_info(msg):
    print("========================================================================")
    print(msg)
    print("========================================================================")

def execCMD(cmd):
    print("[INFO] " + cmd)
    ret = os.system(cmd)
    if ret != 0:
        raise Exception("Failed to exec cmd:" + cmd)

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

def create_zoo_cfg(myid):
    sample_cfg = ZK_CONF_PATH + "zoo_sample.cfg"
    copy_cfg = ""
    if os.path.exists(ZK_CONF_PATH + "zoo.cfg"):
        copy_cfg = ZK_CONF_PATH + "zoo" + str(myid) + ".cfg"
    else:
        copy_cfg = ZK_CONF_PATH + "zoo.cfg"
    execCMD("cp %s %s" % (sample_cfg, copy_cfg))
    return copy_cfg

def modify_config(myid, config, zk_cfg):
    server = config["server"]
    servers["server.%d" % myid] = convert_localhost(server)
    hostname = server.split(":")[0]
    if hostAdaptor(hostname) == False:
        return False

    if config["deploy"] == False:
        return False

    copy_cfg = create_zoo_cfg(myid)
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

    execCMD("echo autopurge.snapRetainCount=3 >> " + copy_cfg)
    execCMD("echo autopurge.purgeInterval=1 >> " + copy_cfg)
    execCMD("echo snapCount=200000 >> " + copy_cfg)
    return True

def deploy_zk(config):
    global node_has_create
    if 'zookeeper-server' in config:
        zk_cfg = []
        for index, item in enumerate(config['zookeeper-server']):
            myid = item['myid']
            if modify_config(myid, item, zk_cfg):
                node_has_create = True

        server_params = dict2Str(servers)

        
        for cfg in zk_cfg:
            execCMD("echo \"%s\" >> %s" % (server_params, cfg))
    if not node_has_create:
        print("no node was created!")
        sys.exit(-2)

def get_zoo_cfg(myid):
    if os.path.exists(ZK_CONF_PATH + "zoo" + str(myid) + ".cfg"):
        return ZK_CONF_PATH + "zoo" + str(myid) + ".cfg"
    return ZK_CONF_PATH + "zoo.cfg"

def start_zk(config):
    display_info("Begin to start zookeeper...")
    if 'zookeeper-server' in config:
        zk_cfg = []
        for index, item in enumerate(config['zookeeper-server']):
            myid = item['myid']
            server = item["server"]
            hostname = server.split(":")[0]
            if item["deploy"] and hostAdaptor(hostname):
                zk_cfg.append(get_zoo_cfg(myid))
        for cfg in zk_cfg:
            execCMD("%s start %s" % (ZK_SERVER_SHELL, cfg))

def stop_zk(count):
    display_info("Begin to stop zookeeper...")
    if os.path.exists(ZK_SERVER_SHELL):
        for id in range(count):
            execCMD("%s stop zoo%d.cfg" % (ZK_SERVER_SHELL, id+1))
    else:
        print("[INFO] %s is not exist, ignore to stop"  % ZK_SERVER_SHELL)

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
    print("\t--zkhome       zookeeper install path(will be cleared),default:")
    print("\t-s             start zookeeper")
    print("\t               %s" % ZK_HOME)

def main(argv):
    global ZK_HOME, ZK_SERVER_SHELL, ZK_CONF_PATH, ZK_DATA_PATH
    config = rootDir + os.sep + "deploy_zk.json"

    try:
        opts, args = getopt.getopt(argv[1:], "dshc:", ["help", "conf=", "zkhome="])
    except getopt.GetoptError:
        print_help(argv[0])
        sys.exit(1)
    isStart = False
    isDeploy = False
    for opt, arg in opts:
        if opt in ("-h", "--help"):
            print_help(argv[0])
            sys.exit(0)
        elif opt in ("-c", "--conf"):
            config = arg
        elif opt in ("-s"):
            isStart = True
        elif opt in ("-d"):
            isDeploy = True
        elif opt == "--zkhome":
            ZK_HOME = os.path.abspath(arg) + os.sep
            ZK_SERVER_SHELL = ZK_HOME + "bin" + os.sep + "zkServer.sh"
            ZK_CONF_PATH = ZK_HOME + "conf" + os.sep
            ZK_DATA_PATH = ZK_HOME + "data" + os.sep

    os.chdir(rootDir)
    conf = load_config(config)
    if isDeploy:
        deploy_zk(conf)
    if isStart:
        start_zk(conf)

if __name__ == '__main__':
    main(sys.argv)

