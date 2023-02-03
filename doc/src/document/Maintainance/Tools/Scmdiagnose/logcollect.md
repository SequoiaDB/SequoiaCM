log-collect 子命令提供日志收集功能，所收集的日志包括 SequoiaCM 节点日志、标准错误输出日志 `error.out` 和系统错误日志 `syserror.log`。

##子命令选项##

| 选项             | 缩写 | 描述                                                                                                                                          | 是否必填 |
|------------------|------|---------------------------------------------------------------------------------------------------------------------------------------------|----------|
| --hosts          |      | 指定需要进行日志收集的目标主机，格式为 `<hostname>:<port>`，多个主机间使用逗号（,）分隔<br> 不指定该选项的情况下，默认收集本机的日志信息                                                           |  否   |
| --services       |      | 指定需要进行日志收集的服务名，多个服务间使用逗号（,）分隔 <br> 不指定该选项的情况下，默认收集所有服务的日志信息                                                                                 |  否   |
| --max-log-count |     | 指定每个节点需收集的节点日志文件个数，默认值为 1，可选取值如下：<br>● 1：仅收集当前正在写入的节点日志文件 <br> ● -1：收集所有的节点日志文件 <br> ● [2, 2^31 -1] 间的任意整数：日志文件按修改时间排序，根据取值收集最新修改的前 N 个日志文件 |  否   |
| --output-path    | -o  | 指定收集后的归档目录，默认为 `/opt/scm-diagnose`                                                                                                          |  否   |
| --install-path   |     | 指定 SequoiaCM 的安装目录，默认为 `/opt/sequoiacm`                                                                                                     |  否   |
| --thread-size    |     | 指定多台主机的收集并发度，默认值为 1，表示同一时刻仅对一台主机进行收集                                                                                                        |  否   |
| --need-zip       |     | 指定是否对所收集的日志进行压缩，默认值为 true，表示以节点为单位进行压缩                                                                                                      |  否   |
| --conf           |     | 指定配置文件，该文件用于配置日志收集行为 <br> 如果命令行与配置文件中的选项取值存在冲突，将按命令行的配置进行日志收集                                                                               |  否   |

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

3. 执行日志收集

    ```lang-bash
    $ ./bin/scmdiagnose.sh log-collect --conf conf/collection.conf 
    ```

   日志收集完成后，输出信息将返回日志的归档目录

    ```lang-text
    ···
    [INFO ] scm log collect successfully：/opt/scm-diagnose/scm-collect-logs_2023-02-03_10-05-57
    ```

   归档目录包含如下内容：

    ```lang-text
    /opt/scm-diagnose/scm-collect-logs_2023-02-03_10-05-57/
    ├── auth-server
    │   └── 192.168.31.111_8820
    │       ├── authserver.log
    │       └── error.out
    ├── config-server
    │   └── 192.168.31.111_8830
    │       ├── configserver.log
    │       └── error.out
    ├── content-server
    │   ├── 192.168.17.184_16600
    │   │   ├── contentserver.log
    │   │   ├── error.out
    │   │   └── syserror.log
    │   └── 192.168.17.184_17700
    │       ├── contentserver.log
    │       ├── error.out
    │       └── syserror.log
    ···
    ```