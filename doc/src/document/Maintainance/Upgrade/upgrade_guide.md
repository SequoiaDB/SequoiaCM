SequoiaCM 支持通过一键升级工具完成整个集群的远程升级，本节主要介绍升级前准备、升级脚本以及升级具体流程。

##升级前准备##

- 阅读[升级注意事项][upgrade_tip]，完成升级准备，如节点配置调整等。
- 将新版本的 SequoiaCM 安装包上传至执行升级脚本的调度机。该调度机可以是集群中的任意一台主机，也可以是能与集群内所有主机通信的其他机器。如果选取其他机器执行升级脚本，可以有效避免对生产环境的资源占用。

##升级步骤##

下述以 SequoiaCM 升级至 v3.2.2 为例，介绍升级步骤。

###升级原有服务##

1. 创建新版本安装包的解压目录

    ```lang-bash
    # mkdir -p /opt/scm_upgrade_package
    ```

2. 安装包解压缩

    ```lang-bash
    # tar -zxvf sequoiacm-3.2.2-release.tar.gz -C /opt/scm_upgrade_package
    ```
3. 切换至解压目录

    ```lang-bash
    # cd /opt/scm_upgrade_package/sequoiacm/
    ```

4. 编辑升级配置文件，填写 SequoiaCM 的安装用户信息、机器信息等，并规划待升级的服务

    ```lang-bash
    # vi sequoiacm-deploy/conf/upgrade.cfg
    ```

    > **Note：**
    >
    > 配置文件的相关参数说明可参考[配置参数][upgrade_config]。

5. 执行升级脚本

    ```lang-bash
    # ./scm.py cluster --upgrade --upgrade-conf sequoiacm-deploy/conf/upgrade.cfg
    ```
   
    脚本执行后，会提示待升级的服务列表和升级顺序，检查无误后，输入 y 确认升级
    
    ```lang-bash
    [INFO ] Parsing the upgrade configuration... 
    [INFO ] Parse the upgrade configuration success
    [INFO ] Checking the upgrade configuration... (0/2)
    [INFO ] Checking the upgrade configuration: Host (1/2)
    [INFO ] Checking the upgrade configuration: Config (2/2)
    [INFO ] Check the upgrade configuration success
    [WARN ] The following specified service do not exist in the cluster:[MQ_SERVER, FULLTEXT_SERVER, SCMSYSTOOLS, NON_SERVICE[doc, driver, tools]]
    [INFO ] Prepare to upgrade services in this order
    [INFO ] [192.168.16.70]:SERVICE_CENTER:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:SERVICE_TRACE:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:AUTH_SERVER:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:CONFIG_SERVER:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:SCHEDULE_SERVER:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:GATEWAY:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:CONTENT_SERVER:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:ADMIN_SERVER:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:OM_SERVER:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:S3_SERVER:3.2.0 to 3.2.2
    [INFO ] [192.168.16.70]:DAEMON:3.2.0 to 3.2.2
    [INFO ] Whether to upgrade?  
    Please enter (y/n) confirm:
    ```
   
6. 升级完成，可通过 `scmsysctl.sh -v` 检查版本号，并通过 `scmsysctl.sh list` 检查节点是否均已正常启动

    ```lang-bash
    [INFO ] Upgrading service...(0/12)
    [INFO ] Upgrading Service: sending upgrade package to 192.168.16.70 (1/12)
    [INFO ] Upgrading Service: upgrading SERVICE_CENTER on 192.168.16.70 (2/12)
    [INFO ] Restarting SERVICE_CENTER node,port=8810
    [INFO ] Upgrading Service: upgrading SERVICE_TRACE on 192.168.16.70 (3/12)
    [INFO ] Upgrading Service: upgrading AUTH_SERVER on 192.168.16.70 (4/12)
    [INFO ] Restarting AUTH_SERVER node,port=8830
    [INFO ] Upgrading Service: upgrading CONFIG_SERVER on 192.168.16.70 (5/12)
    [INFO ] Restarting CONFIG_SERVER node,port=8840
    [INFO ] Upgrading Service: upgrading SCHEDULE_SERVER on 192.168.16.70 (6/12)
    [INFO ] Restarting SCHEDULE_SERVER node,port=8860
    [INFO ] Upgrading Service: upgrading GATEWAY on 192.168.16.70 (7/12)
    [INFO ] Restarting GATEWAY node,port=8080
    [INFO ] Upgrading Service: upgrading CONTENT_SERVER on 192.168.16.70 (8/12)
    [INFO ] Restarting CONTENT_SERVER node,port=15000
    [INFO ] Upgrading Service: upgrading ADMIN_SERVER on 192.168.16.70 (9/12)
    [INFO ] Restarting ADMIN_SERVER node,port=8870
    [INFO ] Upgrading Service: upgrading OM_SERVER on 192.168.16.70 (10/12)
    [INFO ] Restarting OM_SERVER node,port=9980
    [INFO ] Upgrading Service: upgrading S3_SERVER on 192.168.16.70 (11/12)
    [INFO ] Restarting S3_SERVER node,port=16000
    [INFO ] Upgrading Service: upgrading DAEMON on 192.168.16.70 (12/12)
    [INFO ] Restarting DAEMON node
    [INFO ] Upgrade service success, generate upgrade status file success, file location:/opt/scm_upgrade_package/sequoiacm/sequoiacm-deploy/upgrade-status/upgrade_status_2023-01-13-05:08:00
    ```

###新增服务###

SequoiaCM v3.2.2 及以上版本新增[链路追踪服务][service_trace]，用户升级到对应版本后，通过 [Cloud 管理工具][readme]启动对应的服务节点即可启用新服务。

##升级回滚##

在升级失败的情况下，输出信息会告知升级状态文件的路径，用户可依据升级状态文件将升级操作回滚。以升级状态文件 `upgrade_status_2023-02-01-10:29:37` 为例，可执行如下语句进行回滚：

```lang-bash
# python scm.py cluster --rollback --upgrade-status-path /opt/scm_upgrade_package/sequoiacm/sequoiacm-deploy/upgrade-status/upgrade_status_2023-02-01-10:29:37
```

##升级脚本说明##

`scm.py cluster` 与升级有关的参数说明如下：

| 参数名          | 参数描述                                                                                                                                      | 
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| --upgrade    | 升级 SequoiaCM 集群                                                                                                                           |
| --rollback   | 回滚 SequoiaCM 集群                                                                                                                           | 
| --upgrade-conf | 指定升级配置文件的存放路径，仅在升级时使用                                                                                                                  |
| --upgrade-status-path   | 指定升级状态文件的存放路径，仅在回滚时使用                                                                                                                  |
| --host      | 选择需要升级或回滚的主机，多个主机名间使用逗号（,）分隔  | 
| --service     | 选择需要升级或回滚的服务，多个服务名间使用逗号（,）分隔 |
| --unattended    | 非交互式升级，不显示交互流程，直接执行升级                                                                                                                   |
> **Note:**
>
> - 在执行升级操作时，如果不指定参数 --host 或 --service，默认使用升级配置文件中所有的主机或服务信息进行操作。如果通过命令行指定主机名或服务名，将根据指定的信息进行升级操作。
> - 在执行回滚操作时，如果不指定参数 --host 或 --service，默认使用升级状态文件中所有的主机或服务信息进行操作。如果通过命令行指定主机名或服务名，将根据指定的信息进行回滚操作。

##配置参数##

**installconfig**

该配置段用于声明 SequoiaCM 安装路径、用户名，需根据 SequoiaCM 集群的安装信息进行填写。参数说明如下：

| 参数名 | 说明     | 是否必填 |
| ------ |----------| -------- |
| InstallPath | 安装路径     | 是 |
| InstallUser | 安装用户     | 是 |


**host**

该配置段用于配置待升级机器的信息，配置后将在对应主机上升级 SequoiaCM，参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| HostName | 主机地址 | 是 | 
| SshPort  | 端口号 | 是 |
| User     | 用户名，需具备 sudo 权限 | 是 |
| Password | 用户密码<br>如果机器已配置互信，则无需填写该参数 | 否 |
| JavaHome | jdk 安装路径<br>该字段为空时，默认在 `/etc/profile` 下搜索 JAVA_HOME 环境变量 | 否 |


**config**

该配置段用于配置待升级的服务以及升级时中间产物存放目录，参数说明如下：

| 参数名   | 说明        | 是否必填 |
|----------|-------------| -------- |
| service  | 指定需要升级的服务名，多个服务名间使用逗号（,）分隔，不指定则升级主机上的所有服务<br>可选项如下：<br> ● gateway：网关服务<br> ● service-center：注册中心服务<br> ● auth-server：认证服务<br> ● admin-server：监控服务<br> ● service-trace：链路追踪服务<br> ● config-server：配置服务<br> ● schedule-server：调度服务<br> ● mq-server：消息队列服务<br> ● daemon：守护进程<br> ● om-server：om 服务<br> ● fulltext-server：全文检索服务<br> ● content-server：内容服务<br> ● s3-server：s3 服务<br> ● non-service：非服务目录(包括 doc、driver、tools)<br> ● scmsystools：一键启停工具 | 是 | 
| upgradePackPath  | 指定脚本和新版本软件包在远程主机上的存放位置，如果指定目录不存在将自动创建   | 是 |
| backupPath     | 指定备份目录在远程主机上的存放位置，如果指定目录不存在将自动创建     | 是 |



[upgrade_tip]:Maintainance/Upgrade/compatibility.md#升级注意事项
[upgrade_config]:Maintainance/Upgrade/upgrade_guide.md#配置参数
[service_trace]:Architecture/Microservice/service_trace.md
[readme]:Maintainance/Tools/Scmcloudadmin/Readme.md