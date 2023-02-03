cluster-info 子命令提供集群信息收集功能。可收集的信息包括集群信息、主机信息和节点信息。

- 集群信息：集群的主机数、服务数、节点数
- 主机信息：内存、磁盘、网络、CPU、系统及运行状况
- 节点信息：每个 SequoiaCM 节点的 jstack 和 tcp 连接

##子命令选项##

| 选项          | 缩写 | 描述     | 是否必填    |
|---------------|------|----------|-------------|
| --gateway     |      | 指定 SequoiaCM 集群的网关服务节点，格式为 `<IP>:<port>`                   | 是    |
| --hosts       |      | 指定需要进行信息收集的目标主机，格式为 `<hostname>:<port>`，多个主机间使用逗号（,）分隔                                                                       | 否    |
| --output-path | -o   | 指定收集后的归档目录，默认为 `/opt/scm-diagnose`                          | 否    |
| --thread-size |      | 指定多台主机的收集并发度，默认值为 1，表示同一时刻仅对一台主机进行收集    | 否    |
| --need-zip    |      | 指定是否对所收集的信息进行压缩，默认值为 true，表示以主机为单位进行压缩   | 否    |
| --conf        |      | 指定配置文件，该文件用于配置收集行为 <br> 如果命令行与配置文件中的选项取值存在冲突，将按命令行的配置进行集群信息收集                                                                             | 否    |

> **Note:**
>
> 在配置文件中指定目标主机时，需要提供目标主机的用户名和密码。如果本机与目标机器间已配置互信，可通过选项 --hosts 进行免密登录。

###示例###

1. 切换至工具所在路径，以 SequoiaCM 安装目录 `/opt/sequoiacm` 为例，执行语句如下：

    ```lang-bash
    $ cd /opt/sequoiacm/tools/sequoiacm-scm-diagnose
    ```

2. 编辑配置文件

    ```lang-bash
    $ vi ./conf/collection.conf
    ```

   根据实际需求配置选项取值

    ```lang-ini
    [hosts]
    HostName,      SshPort, User,        Password,
    192.168.17.184, 22,      root,      /home/scmadmin/scm.passwd
    192.168.32.111, 22,      root,      sequoiadb  
    
    [collectConfig]
    services=daemon,admin-server,auth-server,gateway,service-center,service-trace,schedule-server,config-server,content-server,mq-server,fulltext-server,s3-server
    max-log-count=1
    output-path=/opt/scm-diagnose
    install-path=/opt/sequoiacm
    thread-size=1
    need-zip=false
    private-key-path=~/.ssh/id_rsa
    connect-timeout=180000
    ```

3. 执行集群信息收集

    ```lang-javascript
    $ ./bin/scmdiagnose.sh cluster-info --gateway 192.168.17.184:8880 --conf conf/collection.conf
    ```

   信息收集完成后，输出信息将返回集群信息的归档目录

    ```lang-text
    ···
    [INFO ] scm cluster collect successfully：/opt/scm-diagnose/scm-collect-cluster_2023-02-02_22-37-36
    ```

   归档目录包含如下内容：

    ```lang-text
    /opt/scm-diagnose/scm-collect-cluster_2023-02-02_22-37-36
    ├── cluster_info.txt
    └── 192.168.17.184
        ├── host_info
        │   ├── cpu.txt
        │   ├── disk.txt
        │   ├── ifconfig.txt
        │   ├── memory.txt
        │   ├── system.txt
        │   ├── top_allPid.txt
        │   └── top_all.txt
        └── node_info
            ├── branchsite1_17700
            │   ├── jstack.txt
            │   └── tcp.txt
            ├── branchsite1-s3_27770
            │   ├── jstack.txt
            │   └── tcp.txt
    ···
    ```


