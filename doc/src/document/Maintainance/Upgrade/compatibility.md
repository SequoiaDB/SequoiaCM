## 兼容性列表##

| 升级前版本 | 升级到2.* | 升级到3.0 | 升级到3.1  | 升级到3.2  |
|------------|-----------|-----------|------------|------------|
| 2.*        | ●         | ×         | ×          | ×          |
| 3.0        | ×         | ●         | ●          | ●          |
| 3.1        | ×         | ×         | ●          | ●          |

> **Note：**
>
>  * SequoiaCM 不支持版本降级。
>
>  * × 表示不支持升级
>
>  * ○ 表示仅支持集群所有服务同时离线升级
>
>  * ● 表示支持在线滚动升级，集群离线升级

## 兼容风险##
| 版本号 | 兼容风险                                                                          |
|------------|-------------------------------------------------------------------------------|
| 3.2.0      | 本版本默认关闭工作区的目录功能，如需使用该功能，需要在创建工作区时显示指定开启目录功能。对于已有工作区，请参考下文升级注意事项章节，补全工作区目录开关状态 |

## 升级注意事项 ##

### SequoiaCM version 3.2.2 ###
- ***升级顺序要求***
<br>
升级至 3.2.2 版本过程中，如果有修改工作区配置的操作，需先升级配置服务。
<br>
如果整个升级过程中不涉及修改工作区配置的操作，对升级顺序没有要求。


### SequoiaCM version 3.2.0 ###
- ***补全工作区目录开关状态***
<br>
升级至 3.2 版本前，请检查并补全元数据表 SCMSYSTEM.WORKSPACE 中工作区的目录开关状态，以确保原有工作区升级后目录功能不受影响。连接 SCM 元数据服务，执行如下步骤：

    1. 检查工作区的目录开关状态是否均已记录

        ```lang-javascript
        db.SCMSYSTEM.WORKSPACE.find({ enable_directory: { $exists: 0 } })
        ```

    2. 若步骤 1 返回不为空，则执行以下命令补全工作区目录开关状态

        ```lang-javascript
        db.SCMSYSTEM.WORKSPACE.update({ $set: { enable_directory: true } }, { enable_directory: { $exists: 0 } })
        ```

- ***service-center节点需引入依赖配置项***
<br>
3.2 版本引入了系统状态监控功能，需要依赖 service-center 存储节点列表，因此在升级到该版本前，请在 service-center 节点的配置文件中加入元数据服务的相关配置，配置示例如下（配置项的具体含义请参考 [cloud 节点配置][cloud_config]）：
   
  ```lang-ini
    # sequoiadb connection configure
    # connect timeout(ms)
    scm.store.sequoiadb.connectTimeout=10000
    # max auto conntect retry time(ms)
    scm.store.sequoiadb.maxAutoConnectRetryTime=15000
    # set socket timeout(ms)
    scm.store.sequoiadb.socketTimeout=0
    # use nagle or not(true|false)
    scm.store.sequoiadb.useNagle=false
    # use ssl or not(true|false)
    scm.store.sequoiadb.useSSL=false
    
    # sequoiadb datasource configure
    # max connection number
    scm.store.sequoiadb.maxConnectionNum=500
    # increase connection count
    scm.store.sequoiadb.deltaIncCount=10
    # max idle connection number
    scm.store.sequoiadb.maxIdleNum=2
    # keep alive time(ms)
    scm.store.sequoiadb.keepAliveTime=60000
    # check connections period time(ms).
    scm.store.sequoiadb.recheckCyclePeriod=30000
    # check whether the connection is usable(true|false)
    scm.store.sequoiadb.validateConnection=true
    
    # 请根据实际情况调整下列元数据服务连接信息
    scm.store.sequoiadb.username=sdbadmin
    scm.store.sequoiadb.password=/opt/sequoiacm/secret/metasource.pwd
    scm.store.sequoiadb.urls=sdbServer:11810

   ```
  
    > **Note:**
    >
    > - 节点配置文件路径：\< cloud 节点安装目录\>/conf/service-center/\<节点端口号\>/application.properties
    > 



- ***确保节点管理端口不会冲突***
<br>
 为支撑系统状态监控，3.2 版本的微服务节点会额外占用一个管理端口号，默认为 server.port + 1，在升级前请确保同一台机器上，各节点的管理端口不会与已有端口产生冲突。如遇冲突，请通过 management.port 配置项显示指定节点的管理端口号。

- ***调度任务支持同名限制***
<br>
3.2 版本支持限制同一工作区下调度任务不能重名，若升级后需要引入这一限制，可以手工为调度任务表创建唯一索引

  ```lang-javascript
    db.SCMSYSTEM.SCHEDULE.createIndex('idx_workspace_schedule_name', { workspace: 1, name: 1 }, { 'Unique': true })
   ```

    > **Note:**
    >
    > - 若创建索引时，已存在重名任务冲突。在不影响原有业务的前提下，排查并修改冲突的调度任务名后，重新创建索引。
    > 

[cloud_config]:Maintainance/Node_Config/cloud.md