#!/usr/bin/python
import commands
import getopt
import os
import glob
import platform
import sys
import time
import datetime

service = None
service_install_path = None
is_rollback = False
need_start_node = False
need_verify_path = True
dry_run = False

ROOT_DIR = sys.path[0]
PACKAGE_DIR = ROOT_DIR + os.sep + "package"
SERVICE_TMP_DIR = ROOT_DIR + os.sep + "service_tmp"
backup_dir = SERVICE_TMP_DIR
UPGRADE_TMP_DIR = ROOT_DIR + os.sep + "upgrade_tmp"
ROLLBACK_TMP_DIR = ROOT_DIR + os.sep + "rollback_tmp"
BACKUP_DIR_NAME = "backup"
BACKUP_FLAG_FILE_NAME = ".backup_completed"
DICT_SVC = {
    "service-center"    : { "dir" : "sequoiacm-cloud",      "backup_dir":"sequoiacm-cloud/service-center",                  "jars_lib" : "jars/sequoiacm-cloud-servicecenter-*.jar,jars/sequoiacm-cloud-tools-*.jar",  "script" : "scmcloudctl.sh",   "script_opt" : " -t service-center" },
    "gateway"           : { "dir" : "sequoiacm-cloud",      "backup_dir":"sequoiacm-cloud/gateway",                         "jars_lib" : "jars/sequoiacm-cloud-gateway-*.jar,jars/sequoiacm-cloud-tools-*.jar",         "script" : "scmcloudctl.sh",   "script_opt" : " -t gateway" },
    "admin-server"      : { "dir" : "sequoiacm-cloud",      "backup_dir":"sequoiacm-cloud/admin-server",                    "jars_lib" : "jars/sequoiacm-cloud-adminserver-*.jar,jars/sequoiacm-cloud-tools-*.jar",     "script" : "scmcloudctl.sh",   "script_opt" : " -t admin-server" },
    "auth-server"       : { "dir" : "sequoiacm-cloud",      "backup_dir":"sequoiacm-cloud/auth-server",                     "jars_lib" : "jars/sequoiacm-cloud-authserver-*.jar,jars/sequoiacm-cloud-tools-*.jar",      "script" : "scmcloudctl.sh",   "script_opt" : " -t auth-server" },
    "service-trace"     : { "dir" : "sequoiacm-cloud",      "backup_dir":"sequoiacm-cloud/service-trace",                   "jars_lib" : "jars/sequoiacm-cloud-servicetrace-*.jar,jars/sequoiacm-cloud-tools-*.jar",    "script" : "scmcloudctl.sh",   "script_opt" : " -t service-trace"},
    "cloud"             : { "dir" : "sequoiacm-cloud",      "backup_dir":"sequoiacm-cloud",                                 "jars_lib" : "jars/*",                                                                       "script" : "scmcloudctl.sh",   "script_opt" : " -t all" },
    "content-server"    : { "dir" : "sequoiacm-content",    "backup_dir":"sequoiacm-content",                               "jars_lib" : "lib/*",                                                                        "script" : "scmctl.sh",        "script_opt" : " --all"  },
    "config-server"     : { "dir" : "sequoiacm-config",     "backup_dir":"sequoiacm-config",                                "jars_lib" : "jars/*",                                                                       "script" : "confctl.sh",       "script_opt" : " -t all" },
    "schedule-server"   : { "dir" : "sequoiacm-schedule",   "backup_dir":"sequoiacm-schedule",                              "jars_lib" : "jars/*",                                                                       "script" : "schctl.sh",        "script_opt" : " -t all" },
    "fulltext-server"   : { "dir" : "sequoiacm-fulltext",   "backup_dir":"sequoiacm-fulltext",                              "jars_lib" : "jars/*",                                                                       "script" : "ftctl.sh",         "script_opt" : " -t all" },
    "mq-server"         : { "dir" : "sequoiacm-mq",         "backup_dir":"sequoiacm-mq",                                    "jars_lib" : "jars/*",                                                                       "script" : "mqctl.sh",         "script_opt" : " -t all" },
    "om-server"         : { "dir" : "sequoiacm-om",         "backup_dir":"sequoiacm-om",                                    "jars_lib" : "jars/*",                                                                       "script" : "omctl.sh",         "script_opt" : " -t all" },
    "s3-server"         : { "dir" : "sequoiacm-s3",         "backup_dir":"sequoiacm-s3",                                    "jars_lib" : "jars/*",                                                                       "script" : "s3ctl.sh",         "script_opt" : " -t all" },
    "daemon"            : { "dir" : "daemon",               "backup_dir":"daemon",                                          "jars_lib" : "jars/*",                                                                       "script" : "scmd.sh",          "script_opt" : ""        },
    "non-service"       : { "dir" : "sequoiacm",            "backup_dir":"non-service",                                     "jars_lib" : "",                                                                             "script" : "",                 "script_opt" : ""        },
}
def print_help():
    print(" --help | -h       : print help message")
    print(" --service         : update which service: %s" % DICT_SVC.keys())
    print(" --install-path    : service install path")
    print(" --nocheck        : do not check the matching between --service and --install-path")
    print(" --rollback        : rollback upgrade")
    print(" --backup-path     : service backup path")
    print(" --start           : or restart the node after upgrade and rollback")
    print(" --dryrun          : dryrun mode")

def untar_service_package(service, service_dir_new):
    service_package = None
    for file in os.listdir(PACKAGE_DIR):
        if file.startswith(DICT_SVC[service]['dir']) and file.endswith(".tar.gz"):
            service_package = PACKAGE_DIR + os.sep + file
            break
    check_notnull(service_package, "can not find a new version of the service installation package")

    exec_cmd("mkdir -p " + SERVICE_TMP_DIR)
    exec_cmd("tar -xf " + service_package + " -C " + SERVICE_TMP_DIR)
    exec_cmd("chmod u+x " + service_dir_new + os.sep + "bin/*.sh")

def mv_resource(service, source_path, target_path):
    operate_resource(service, source_path, target_path, "move")

def cp_resource(service, source_path, target_path):
    operate_resource(service, source_path, target_path, "copy")

def operate_resource(service, source_path, target_path, operate_type):
    exec_cmd("mkdir -p " + target_path)
    current_py = source_path + os.sep + "*.py"
    current_bin = source_path + os.sep + "bin"
    if operate_type == "copy":
        # service may not contain .py (e.g. daemon)
        if is_source_exist(current_py):
            exec_cmd("cp -rf " + current_py + " " + target_path)
        exec_cmd("cp -rf " + current_bin + " " + target_path)
        for jar in DICT_SVC[service]['jars_lib'].split(","):
            target_jar_dir = target_path + os.sep + jar.split("/")[0]
            exec_cmd("mkdir -p " + target_jar_dir)
            exec_cmd("cp -rf " + source_path + os.sep + jar + " " + target_jar_dir)
    else:
        mv_if_source_exist(current_py, target_path)
        mv_if_source_exist(current_bin, target_path)
        for jar in DICT_SVC[service]['jars_lib'].split(","):
            target_jar_dir = target_path + os.sep + jar.split("/")[0]
            exec_cmd("mkdir -p " + target_jar_dir)
            mv_if_source_exist(source_path + os.sep + jar, target_jar_dir)


def mv_if_source_exist(source_path, target_path):
    if is_source_exist(source_path):
        exec_cmd("mv " + source_path + " " + target_path)

def is_source_exist(source_path):
    return len(glob.glob(source_path)) != 0

def copy_old_transwarp_jars(service_backup_dir, service_install_path):
    #cp old transwarp jars to install path
    backup_lib = service_backup_dir + os.sep + "lib"
    new_lib = service_install_path + os.sep + "lib"
    if os.path.exists(backup_lib + os.sep + "hbase_transwarp"):
        exec_cmd("cp -rf " + backup_lib + os.sep + "hbase_transwarp " + new_lib)
    if os.path.exists(backup_lib + os.sep + "hdfs_transwarp"):
        exec_cmd("cp -rf " + backup_lib + os.sep + "hdfs_transwarp " + new_lib)

def upgrade_non_service(service_backup_dir, backup_completed, service_install_path):
    print("[INFO] backup")
    exec_cmd("mkdir -p " + service_backup_dir)
    old_doc = service_install_path + os.sep + "doc"
    old_driver = service_install_path + os.sep + "driver"
    old_tools = service_install_path + os.sep + "tools"
    new_doc = ROOT_DIR + os.sep + "doc"
    new_driver = ROOT_DIR + os.sep + "driver"
    new_tools = ROOT_DIR + os.sep + "tools"

    if os.path.exists(old_doc) and os.path.exists(new_doc):
        exec_cmd("cp -rf " + old_doc + " " + service_backup_dir)
    if os.path.exists(old_driver) and os.path.exists(new_driver):
        exec_cmd("cp -rf " + old_driver + " " + service_backup_dir)
    if os.path.exists(old_tools) and os.path.exists(new_tools):
        exec_cmd("cp -rf " + old_tools + " " + service_backup_dir)
    exec_cmd("touch " + backup_completed)
    print("backup success,backup location:" + service_backup_dir)

    print("[INFO] begin replace the new version of the resource package")
    if os.path.exists(old_doc) and os.path.exists(new_doc):
        exec_cmd("rm -rf " + old_doc)
        exec_cmd("cp -rf " + new_doc + " " + service_install_path)
    if os.path.exists(old_driver) and os.path.exists(new_driver):
        exec_cmd("rm -rf " + old_driver)
        exec_cmd("cp -rf " + new_driver + " " + service_install_path)
    if os.path.exists(old_tools) and os.path.exists(new_tools):
        exec_cmd("rm -rf " + old_tools)
        exec_cmd("cp -rf " + new_tools  + " " + service_install_path)

def upgrade(service, service_install_path, backup_dir):
    service_dir_new = SERVICE_TMP_DIR + os.sep + DICT_SVC[service]['dir']
    service_backup_dir = backup_dir + os.sep + DICT_SVC[service]['backup_dir'] + os.sep + BACKUP_DIR_NAME
    backup_completed = service_backup_dir + os.sep + BACKUP_FLAG_FILE_NAME
    if os.path.exists(backup_completed):
        print("[ERROR] a backup exists in the specified backup directory:" + service_backup_dir)
        sys.exit(2)

    if service == "non-service":
        upgrade_non_service(service_backup_dir, backup_completed, service_install_path)
        sys.exit(0)

    print("[INFO] untar the service installation package")
    untar_service_package(service, service_dir_new)

    print("[INFO] backup")
    cp_resource(service, service_install_path, service_backup_dir)
    exec_cmd("touch " + backup_completed)
    print("backup success, backup location:" + service_backup_dir)

    print("[INFO] stop node")
    if operate_node(service, service_install_path, "stop") != 0:
        if operate_node(service, service_install_path, "list") == 0:
            print("[ERROR] stop node failed, check and re-execute the upgrade")
            sys.exit(1)
        print("[DEBUG] no running nodes or missing files")

    print("[INFO] replace the new version of the resource package")
    exec_cmd("rm -rf " + UPGRADE_TMP_DIR)
    mv_resource(service, service_install_path, UPGRADE_TMP_DIR)
    cp_resource(service, service_dir_new, service_install_path)
    if service == "content-server":
        copy_old_transwarp_jars(service_backup_dir, service_install_path)

    if need_start_node:
        print("[INFO] start node")
        if operate_node(service, service_install_path, "start") != 0:
            print("[ERROR] start node failed! check and restart node")
            sys.exit(1)



def rollback_non_service(service_backup_dir, backup_completed, service_install_path):
    print("[INFO ]check backup")
    install_doc = service_install_path + os.sep + "doc"
    install_driver = service_install_path + os.sep + "driver"
    install_tools  = service_install_path + os.sep + "tools"

    backup_doc = service_backup_dir + os.sep + "doc"
    backup_driver = service_backup_dir + os.sep + "driver"
    backup_tools  = service_backup_dir + os.sep + "tools"
    print("[INFO ]rollback backup")
    if os.path.exists(backup_doc):
        exec_cmd("rm -rf " + install_doc)
        exec_cmd("cp -rf " + backup_doc + " " + service_install_path)
    if os.path.exists(backup_driver):
        exec_cmd("rm -rf " + install_driver)
        exec_cmd("cp -rf " + backup_driver + " " + service_install_path)
    if os.path.exists(backup_tools):
        exec_cmd("rm -rf " + install_tools)
        exec_cmd("cp -rf " + backup_tools + " " + service_install_path)
    exec_cmd("rm -rf " + backup_completed)

def rollback(service, service_install_path, backup_dir):
    service_dir_new = SERVICE_TMP_DIR + os.sep + DICT_SVC[service]['dir']
    service_backup_dir = backup_dir + os.sep + DICT_SVC[service]['backup_dir'] + os.sep + BACKUP_DIR_NAME
    backup_completed = service_backup_dir + os.sep + BACKUP_FLAG_FILE_NAME

    print("[INFO] check backup")
    if not os.path.exists(backup_completed):
        print("[ERROR] backup of service is incomplete: " + service_backup_dir + ", please confirm whether it has been upgraded")
        sys.exit(1)

    if service == "non-service":
        rollback_non_service(service_backup_dir, backup_completed, service_install_path)
        return

    print("[INFO] stop node")
    if operate_node(service, service_install_path, "stop") != 0:
        if operate_node(service, service_install_path, "list") == 0:
            print("[ERROR] stop node failed, check and re-execute the rollback")
            sys.exit(1)
        print("[DEBUG] no running nodes or missing files")

    print("[INFO] rollback backup")
    exec_cmd("rm -rf " + ROLLBACK_TMP_DIR)
    mv_resource(service, service_install_path, ROLLBACK_TMP_DIR)
    cp_resource(service, service_backup_dir, service_install_path)

    if need_start_node:
        print("[INFO] start node")
        if operate_node(service, service_install_path, "start") != 0:
            print("[ERROR] start node failed! check and restart node")
            sys.exit(1)
    exec_cmd("rm -rf " + backup_completed)

def operate_node(service, service_install_path, operate_type):
    operate_cmd = service_install_path + os.sep + "bin" + os.sep + DICT_SVC[service]['script'] + " "
    operate_cmd += operate_type
    if operate_type != "list":
        operate_cmd += DICT_SVC[service]['script_opt']
    return exec_cmd_with_return(operate_cmd)

def exec_cmd(cmd):
    res = exec_cmd_with_return(cmd)
    if res != 0:
        sys.exit(res)

def exec_cmd_with_return(cmd):
    print(cmd)
    if dry_run:
        return 0
    return os.system(cmd)

def check_notnull(obj, error_message):
    if obj == None:
        print("[ERROR] " + error_message)
        sys.exit(2)

def parse_command():
    global service, service_install_path, backup_dir, is_rollback, need_start_node, need_verify_path, dry_run
    try:
        options, args = getopt.getopt(sys.argv[1:], "h", ["help", "service=", "install-path=","backup-path=", "nocheck", "rollback", "start", "dryrun"])
    except getopt.GetoptError, e:
        print ("[ERROR] ", e)
        sys.exit(2)

    for name, value in options:
        if name in ("-h", "--help"):
            print_help()
            sys.exit(0)
        elif name == '--service':
            service = value.strip().lower()

        elif name == '--install-path':
            service_install_path = value.strip()
            if service_install_path.endswith(os.sep):
                service_install_path = service_install_path[:-1]
        elif name == '--backup-path':
            backup_dir = value.strip()
            if backup_dir.endswith(os.sep):
                            backup_dir = backup_dir[:-1]
        elif name == '--nocheck':
            need_verify_path = False

        elif name == "--rollback":
            is_rollback = True

        elif name == "--start":
            need_start_node = True

        elif name == "--dryrun":
            print("[INFO] dry run mode!")
            dry_run = True

def check_arg(service, service_install_path):
    check_notnull(service, "please set --service")
    check_notnull(DICT_SVC.get(service), "unsupported service:" + service)

    check_notnull(service_install_path, "please set --install-path")
    if need_verify_path:
        if os.path.basename(service_install_path) != DICT_SVC[service]['dir']:
            print("[ERROR] check whether the service and install-path match")
            sys.exit(2)


if __name__ == '__main__':
    parse_command()
    check_arg(service, service_install_path)

    if is_rollback:
        rollback(service, service_install_path, backup_dir)
        print("[INFO] rollback success!")
        sys.exit(0)

    upgrade(service, service_install_path, backup_dir)
    print("[INFO] upgrade success!")
