# 迁移清理工具

使用迁移工具可以将源站点的文件内容数据迁移至目标站点的对象存储数据源，再通过清理工具清理源站点上的内容数据。

>  **Note:**
> * 迁移工具和清理工具可以单独使用，也可以结合使用
>
> * 单独使用时，迁移和清理对应的脚本为 scmTransfer.sh 和 scmClean.sh，需要指定工作区名称 wsName 和文件匹配条件中的创建月份 create_month
> 
> * 可以通过迁移清理脚本 start.sh 完成迁移和清理操作，需要指定 workspace.list 文件的路径
> 
> * 指定 --version / -v 参数执行脚本 scmTransfer.sh 或 scmClean.sh 可以获取迁移或清理工具的版本信息。

## 工具介绍

工具目录结构

```shell
/sequoiacm-transfer-clean-file
    |-- bin/                
        |-- scmTransfer.sh  # 迁移工具执行脚本
        |-- scmClean.sh     # 清理工具执行脚本
        |-- start.sh        # 迁移清理工具执行脚本
    |-- conf/               
        |-- scm_env.sh      # 工具配置文件 
        |-- workspace.list  # 需要迁移清理的工作区条件列表
    |-- lib/                # 工具执行所需 jar 包
    |-- log/                # 工具的日志文件
    |-- README.md           # 工具的使用手册文档
```
配置文件参数说明

| 参数                | 类型                | 是否必填           | 描述                                                                                                           |                     
|---------------------|-------------------|------------------|--------------------------------------------------------------------------------------------------------------|
| srcSiteName         | 字符串               | 是               | 源站点名称，在清理工具中是指需要清理的站点名称                                                                                      |
| targetSiteName      | 字符串               | 是               | 目标站点名称，在迁移工具中是指迁移的目标站点名称                                                                                     |
| metaSdbCoord        | 字符串               | 是               | 元数据 sdb 地址，多个地址使用逗号分隔                                                                                        |
| metaSdbUser         | 字符串               | 是               | 元数据 sdb 用户名                                                                                                  |
| metaSdbPassword     | 字符串               | 是               | 元数据 sdb 密码                                                                                                   |
| connectTimeout      | 数字                | 否               | 连接超时时间，单位：毫秒，默认值为 1000                                                                                       |
| socketTimeout       | 数字                | 否               | socket 连接超时时间，默认值为 0                                                                                         |
| maxConnectionNum    | 数字                | 否               | 连接池可管理的最大连接数量，即连接池大小，默认值为 500                                                                                |
| keepAliveTimeout    | 数字                | 否               | 连接池内闲置连接的最大闲置时间，默认值为 0                                                                                       |
| url                 | 字符串               | 是                | scm 网关地址和迁移目标站点名，如迁移至 branchSite1，则填写为 gatewayhost:port/branchSite1                                          |
| scmUser             | 字符串               | 是                | scm 系统用户名                                                                                                    |
| scmPassword         | 字符串               | 是                | scm 系统密码                                                                                                     |
| queueSize           | 数字                | 否                | 线程池队列大小，默认值为 1000                                                                                            |
| thread              | 数字                | 否                | 线程池线程数，默认值为 20                                                                                               |
| datasourceConf      | 字符串               | 否               | 一般不需要填写，支持填写数据源连接相关配置，如：scm.sftp.xxx=xxx、scm.cephs3.xxx=xxx，格式为：k1=v1,k2=v2，具体配置项可参考用户手册中运维指南节点配置章节的内容服务节点配置 |
| srcSitePasswordFile | 字符串               | 否               | 需要清理的站点对应的数据源的密码文件路径，srcSitePasswordFile 和 srcSitePassword 两者可选填其一                                           |
| srcSitePassword     | 字符串               | 否               | 需要清理的站点对应的数据源的密码，srcSitePasswordFile 和 srcSitePassword 两者可选填其一                                               |
| targetSiteInstances | 字符串               | 否               | 目标站点实例名称，多个名称使用逗号分隔，格式为 hostname:port                                                                        |
| zkUrls              | 字符串               | 是               | zookeeper 地址                                                                                                 |
| maxBuffer           | 数字                | 是               | zookeeper client buffer 大小，单位 mb                                                                             |
| maxResidualTime     | 数字                | 是               | zookeeper 文件夹存在超过多长时间后，可以被清理，单位 ms                                                                           |
| maxChildNum         | 数字                | 否               | zookeeper 客户端最大数量                                                                                            |
| cron               | 字符串               | 是               | zookeeper 节点清理定时任务执行规则                                                                                       |
| maxFileNumber      | 数字                 | 是               | 日志文件的最大数量                                                                                                    |
| maxFileSize        | 数字                 | 是               | 日志文件的最大大小                                                                                                    |

## 执行迁移

将工作区 ws_default 中满足 create_month=202209 的文件迁移至目标站点

```shell
./scmTransfer.sh ws_default 202209
```

>  **Note:**
> * 迁移工具在单独使用时，只需要指定 wsName 和 create_month，其他参数需要在 scm_env.sh 配置文件中指定

## 执行清理

将 ws_default 工作区中满足 create_month=202209 的文件进行清理

```shell
./scmClean.sh ws_default 202209
```

>  **Note:**
> * 清理工具在单独使用时，只需要指定 wsName 和 create_month，其他参数需要在 scm_env.sh 配置文件中指定

## 执行迁移和清理

将源站点上指定工作区中满足匹配条件的文件迁移至目标站点，并清理源站点满足匹配条件的文件

```shell
./start.sh ./conf/workspace.list
```

>  **Note:**
> * 迁移清理工具在使用时，需要在 workspace.list 配置文件中指定 wsName 和 create_month，可支持配置多组 wsName 和 create_month，其他参数需要在 scm_env.sh 配置文件中指定


## 执行Zookeeper日志清理
需要在每台 zk 节点所在机器执行本脚本，修改脚本如下变量定义：
- 填写 zk 安装目录的 bin 目录：
zkBinDir=/opt/sequoiacm/zookeeper-3.4.12/bin

- 查看当前安装目录 zk 节点的配置文件 conf/zoo.cfg 或者是 conf/zoo1（2、3）.cfg  ，观察配置项 dataDir，这个变量填写 dataDir 的值：
zkDataDir=/opt/sequoiacm/zookeeper-3.4.12/data/1

```shell
./zkLogClean.sh
```

>  **Note:**
> * 请在执行迁移清理工具前，在每台 zk 节点机器上执行该脚本，迁移清理工具执行结束后，停止该脚本