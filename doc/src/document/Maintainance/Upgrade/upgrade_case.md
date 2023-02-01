本节将介绍一个小规模的 SequoiaCM 集群从版本 3.1.2 滚动升级至 3.2.2 的过程。

##滚动升级##

滚动升级是一种在线升级方式，SequoiaCM 的滚动升级流程需要注意以下几点：

- 选择在业务量最小的时间段内进行升级。

- 如果用户划分了主备机房，可选择同时升级主备机房中所有的服务节点，也可以按先备后主的顺序进行分批升级。

- 服务升级涉及到节点重启，升级过程中可能会出现短暂的业务失败。

##案例##

###环境介绍###

![升级案例集群环境][env_upgrade_case]

SequoiaCM 集群为两中心部署，每个服务均有一个节点实例落于主、备中心。

###升级步骤###

本节假设 SequoiaCM 的安装用户、用户组分别为 scmadmin、scmadmin_group，服务的安装路径为 /opt/sequoiacm/。

1. 将 SequoiaCM 3.2.2 版本的安装包上传至调度机（可以是集群中的任意一台主机，也可以是能与集群内所有主机通信的其他机器）

2. 创建新版本安装包的解压目录

    ```lang-bash
    # mkdir -p /opt/scm_upgrade_package
    ```

3. 安装包解压缩

    ```lang-bash
    # tar -zxvf sequoiacm-3.2.2-release.tar.gz -C /opt/scm_upgrade_package
    ```

4. 切换至解压目录

    ```lang-bash
    # cd /opt/scm_upgrade_package/sequoiacm/
    ```

5. 编辑升级配置文件

    ```lang-bash
    # vi sequoiacm-deploy/conf/upgrade.cfg
    ```

    指定备机房（主机 C、D）的信息

    ```lang-bash
    [installconfig]
    InstallPath,          InstallUser,  InstallUserPassword, InstallUserGroup
    /opt/,                scmadmin,     admin,               scmadmin_group
    
    # 配置备机房主机 C、D 的信息
    [host]
    HostName,      SshPort, User,        Password,  JavaHome
    scmServer-c, 22,      root,   sequoiadb,
    scmServer-d, 22,      root,   sequoiadb,
    
    [config]
    upgradePackPath=/opt/upgrade/sequoiacm
    backupPath=/opt/backup/sequoiacm
    ```
   
6. 执行升级脚本，升级备机房的节点

    ```lang-bash
    # ./scm.py cluster --upgrade --upgrade-conf sequoiacm-deploy/conf/upgrade.cfg
    ```
   
7. 再次修改升级配置文件

    ```lang-bash
    # vi sequoiacm-deploy/conf/upgrade.cfg
    ```

    指定主机房（主机 A、B ）的信息

    ```lang-bash
    [installconfig]
    InstallPath,          InstallUser,  InstallUserPassword, InstallUserGroup
    /opt/,                scmadmin,     admin,               scmadmin_group
    
    # 配置备机房主机 A、B 的信息
    [host]
    HostName,      SshPort, User,        Password,  JavaHome
    scmServer-a, 22,      root,   sequoiadb,
    scmServer-b, 22,      root,   sequoiadb,
    
    [config]
    upgradePackPath=/opt/upgrade/sequoiacm
    backupPath=/opt/backup/sequoiacm
    ```

8. 执行升级脚本，升级备机房的节点

    ```lang-bash
    # ./scm.py cluster --upgrade --upgrade-conf sequoiacm-deploy/conf/upgrade.cfg
    ```


[env_upgrade_case]:Maintainance/Upgrade/env_upgrade_case.png