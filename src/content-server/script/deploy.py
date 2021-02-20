#!/usr/bin/python

import platform
import commands
import sys
import os
import json
import getopt
import socket

NEED_CREATESITE = 0
NEED_CREATEWS = 0
NEDD_CREATNODE = 0
NEED_CREATEUSER = 0

rootDir = sys.path[0]

SCM_BIN_PATH = rootDir + os.sep + "bin";
SCM_ADMIN = "scmadmin.sh"
SCM_CTL = "scmctl.sh"

mdsurlSetCMD = ""

dry_run = False
node_has_create = False

if platform.system() == "Windows":
    SCM_ADMIN = "scmadmin.bat"
    SCM_CTL = "scmctl.bat"

def command(cmd):
    print(cmd)
    if dry_run:
        return
    (status, output) = commands.getstatusoutput(cmd)
    if not len(output) == 0:
        print(output)
    if status != 0:
        raise Exception("Failed to execute cmd: %s" %cmd + '\r\n'+'errorMsg:' + output)

def scm_admin(cmd):
    command("%s%s%s %s"%(SCM_BIN_PATH, os.sep, SCM_ADMIN, cmd))

def scm_ctl(cmd):
    command("%s%s%s %s"%(SCM_BIN_PATH, os.sep, SCM_CTL, cmd))

def convert_localhost(url):
    splitUrls = url.split(',')
    hostUrl = ""
    for splitUrl in splitUrls:
        u = splitUrl.split(':')
        if u[0] == "localhost" or u[0] == "127.0.0.1":
            local = socket.gethostname()
            if hostUrl != "":
                hostUrl = hostUrl + ","
            hostUrl = hostUrl + local + ":" + u[1]
        else:
            if hostUrl != '':
                hostUrl = hostUrl + ","
            hostUrl = hostUrl + splitUrl
    return hostUrl

def hostAdaptor(url):
    splitUrl = url.split(',')
    u = splitUrl[0].split(':')
    local_hostname = socket.gethostname()
    local_hostip = socket.gethostbyname(local_hostname)
    if u[0] == "localhost" or u[0] == "127.0.0.1" or u[0] == local_hostname or u[0] == local_hostip:
        return True
    else:
        return False

def load_config(conf_file):
    f = open(conf_file,'r')
    try:
        j = f.read()
        config = json.loads(j)
        return config
    finally:
        f.close()

def get_ds_type(s):
    m = {
        'sequoiadb': 1,
        'hbase': 2,
        'ceph_s3': 3,
        'ceph_swift': 4,
        'hdfs': 5,
        'hbase_transwarp' :6,
        'hdfs_transwarp' :7
    }
    return m.get(s, 1)

def scm_global_mdsurl(sites_conf):
    global mdsurlSetCMD
    #set mdsurl from root site
    for site_conf in sites_conf:
        if site_conf.get('isRoot', False):
            if 'meta' not in site_conf:
                raise Exception("missing 'meta' in root site config")
            else:
                meta = site_conf['meta']
                meta_url = meta['url']
                mdsurlSetCMD += " --mdsurl " + convert_localhost(meta_url)
                if 'user' in meta:
                    meta_user = meta['user']
                    if meta_user != '':
                        mdsurlSetCMD += " --mdsuser " + meta_user
                        meta_passwd = meta.get('password', '')
                        if meta_passwd != '':
                            mdsurlSetCMD += " --mdspasswd " + meta_passwd
                break


def create_sites(sites_conf, gateway_conf):
    # create root site if exists
    for site_conf in sites_conf:
        if site_conf.get('isRoot', False):
            create_site(site_conf, gateway_conf)
            break
    # create branch sites
    for site_conf in sites_conf:
        if not site_conf.get('isRoot', False):
            create_site(site_conf, gateway_conf)

def create_site(site_conf, gateway_conf):
    global mdsurlSetCMD
    cmd = "createsite"

    name = site_conf['name']
    cmd += " --name " + name

    is_root = site_conf.get('isRoot', False)
    if is_root:
        cmd += "  --continue --root"

    # data
    data = site_conf['data']
    ds_type = data.get('type', 'sequoiadb')
    cmd += " --dstype " + str(get_ds_type(ds_type))
    ds_url = data['url']
    cmd += " --dsurl " + convert_localhost(ds_url)
    if 'user' in data:
        ds_user = data['user']
        if ds_user != '':
            cmd += " --dsuser " + ds_user
            ds_passwd = data.get('password', '')
            if ds_passwd != '':
                cmd += " --dspasswd " + ds_passwd
    if (ds_type == 'hbase' or ds_type == 'hdfs'):
        ds_conf = data['configuration']
        cmd += " --dsconf '%s'" % json.dumps(ds_conf)
    if (ds_type == 'hbase_transwarp' or ds_type == 'hdfs_transwarp'):
        ds_conf = data['configuration']
        cmd += " --dsconf '%s'" % json.dumps(ds_conf)

    # meta
    cmd += mdsurlSetCMD

    # gateway
    gateway = gateway_conf['url']
    user = gateway_conf['user']
    passwd = gateway_conf['password']
    cmd += ' --gateway ' +gateway +' --user '+ user +' --passwd ' + passwd
    scm_admin(cmd)


def create_nodes(nodes_conf, audit_conf, gateway_conf):
    global node_has_create
    for node_conf in nodes_conf:
        if 'node' in node_conf:
            conf = node_conf['node']
            for p in conf:
                if hostAdaptor(p['url']):
                    node_has_create = True
                    create_node(p, audit_conf, gateway_conf)

def create_node(conf, audit_conf, gateway_conf):
    global mdsurlSetCMD
    cmd = "createnode"

    name = conf['name']
    url = conf['url']
    site_name = conf['siteName']
    cmd += " --name %s --serverurl %s --sitename %s" %(name, convert_localhost(url), site_name)

    cmd += mdsurlSetCMD

    if 'customProperties' in conf and len(conf['customProperties']) > 0:
        customProps = conf['customProperties']
        for customPropKey in customProps:
            cmd += ' -D' + customPropKey + '=' + customProps[customPropKey]
    # audit
    adurl = audit_conf['auditurl']
    aduser = audit_conf['audituser']
    adpasswd = audit_conf['auditpassword']
    cmd += ' --adurl ' + adurl + " --aduser " + aduser + " --adpasswd " + adpasswd
    
    # gateway
    gateway = gateway_conf['url']
    user = gateway_conf['user']
    passwd = gateway_conf['password']
    cmd += ' --gateway ' +gateway +' --user '+ user +' --passwd ' + passwd
    
    scm_admin(cmd)

def start_node(port=0):
    cmd = "start"
    if port ==0:
        cmd += " --all -t 100"
    else:
        cmd += " --port " + str(port)
    scm_ctl(cmd)

def deploy_scm(config):
    global NEED_CREATESITE,NEED_CREATEWS,NEDD_CREATNODE
    
    if NEED_CREATESITE == 1:
        if 'sites' in config:
            create_sites(config['sites'], config['gateway'])
    if NEDD_CREATNODE == 1:
        if 'nodes' in config:
            create_nodes(config['nodes'], config['audit'], config['gateway'])
    if not node_has_create:
        print("no node was created!")
        sys.exit(-2)
    

def displayHelp(name):
    print('Usage: %s [option] ...' % name)
    print("")
    print("Options:")
    print("\t-h, --help    print help information")
    print("\t--createsite  createsite, default is False")
    print("\t--createnode  createnode, default is False")
    print("\t--createws    createws, default is False, default execute script './createworkspaces.py'")
    print("\t--createuser  createusers, default is False, default execute script './createusers.py'")
    print("\t-c, --conf    specify config file path for create nodes or sites, default is './deploy.json'")
    print("\t-b, --bin     specify scm bin path, default is '%s'" % SCM_BIN_PATH)
    print("\t-s, --start   start nodes")
    print("\t--dryrun      output command but not execute")


def main(argv):
    global NEED_CREATESITE,NEED_CREATEWS,NEDD_CREATNODE,SCM_BIN_PATH,dry_run,NEED_CREATEUSER
    config = rootDir + os.sep +  "deploy.json"
    start = False
    try:
        opts, args = getopt.getopt(argv[1:],"h:c:b:s",["help", "createsite", "createws", "createnode", "createuser", "start", "conf=", "bin=", "dryrun"])
    except getopt.GetoptError:
        displayHelp(argv[0])
        sys.exit(2)
    for opt,arg in opts:
        if opt in ("-h","--help"):
            displayHelp(argv[0])
            sys.exit(0)
        elif opt  == "--createsite":
            NEED_CREATESITE = 1
        elif opt  == "--createws":
            NEED_CREATEWS = 1
        elif opt  == "--createnode":
            NEDD_CREATNODE = 1
        elif opt in ("-c","--conf"):
            config = arg
        elif opt in ("-b","--bin"):
            SCM_BIN_PATH = arg
        elif opt == "--dryrun":
            dry_run = True
        elif opt == "--createuser":
            NEED_CREATEUSER = 1
        elif opt in ("-s", "--start"):
            start = True
    conf = load_config(config)
    scm_global_mdsurl(conf['sites'])
    deploy_scm(conf)
    if start:
        start_node()
    if NEED_CREATEWS == 1:
        command("python " + rootDir + os.sep +  "createworkspaces.py")
    if NEED_CREATEUSER == 1:
        command("python " + rootDir + os.sep + "createusers.py")

if __name__=='__main__':
    main(sys.argv)
