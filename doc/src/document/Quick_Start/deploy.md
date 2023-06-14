本文档主要介绍通过命令行方式在本地主机安装 SequoiaCM 企业内容管理软件。

##下载安装包##

当前版本暂不提供 SequoiaCM 企业内容管理软件的下载途径，如果需要下载安装包可联系 SequoiaCM 技术支持。

##安装部署##

###部署规划###

安装部署前，用户需根据业务需求进行部署规划，便于后续进行集群配置。目前 SequoiaCM 提供以下服务：

|服务名|说明| 是否必选 |
|------|------------|----|
| ZooKeeper | 提供选举及分布式锁服务 | 是 |
| 内容管理服务 | 提供内容管理核心功能，负责文件、批次和目录的增删查改，后台任务的创建与执行 | 是 |
| 配置服务 | 提供配置管理功能，负责管理 SequoiaCM 的业务配置和节点配置 | 是 |
| 调度服务 | 提供调度管理功能，负责调度文件的内容迁移和清理 | 否 |
| 注册中心 | 提供服务治理功能，负责 SequoiaCM 集群中各微服务间的治理 | 是 |
| 网关服务 | 提供路由和负载均衡功能，负责对驱动的请求进行路由和分发 | 是 |
| 认证服务 | 提供认证和权限控制功能 | 是 |
| 监控服务 | 提供监控功能，负责业务统计和集群状态监控的功能 | 否 |
| 消息队列服务 | 提供集群内消息队列功能 | 否 |
| 全文检索服务 | 提供文件的全文检索功能，具体说明如下：<br>● 使用该服务前，用户需提前部署与 SCM 版本匹配的 Elasticsearch，并安装 IK 中文分词器<br>● 如果需要全文检索图片文件，用户还需安装 Tessract 图片识别引擎 | 否 |
| OM 管理服务 | 提供 SequoiaCM 集群的可视化管理功能 | 否 |
| S3 服务 | 提供 S3 协议处理能力 | 否 |
| 链路追踪服务 | 提供分布式链路追踪能力 | 否 |

###安装部署前准备###

- 安装部署过程需使用 root 用户权限。
- 需参照[软硬件配置要求][install_requirement]调整系统配置。
- 需提前部署 SequoiaDB 集群，同时保证已创建数据域、已设置鉴权且已开启事务功能。

###安装部署步骤###

下述安装过程将使用名称为 `sequoiacm-3.2.0-release.tar.gz` 的产品包为示例。

1. 将产品包解压缩至当前主机

    ```lang-bash
    # tar -zxvf sequoiacm-3.2.0-release.tar.gz -C /opt/data/
    ```

2. 切换至解压后的目录

    ```lang-bash
    # cd /opt/data/sequoiacm/
    ```

3. 编辑集群部署配置文件

    ```lang-bash
    # vi sequoiacm-deploy/conf/deploy.cfg
    ```

    >**Note:**
    >
    > 用户需根据部署规划编辑配置文件，参数说明可参考[安装部署配置参数][deploy]。

3. 执行安装部署命令

    ```lang-bash 
    # ./scm.py cluster --deploy --conf sequoiacm-deploy/conf/deploy.cfg
    ```

4. 检查节点

    使用浏览器登陆服务注册中心（http://scmServer:8800），确保规划的节点都已经注册到服务注册中心。

###创建工作区###

工作区是承载具体业务的逻辑单元，所有内容服务相关的元素必须隶属于某个工作区。因此，在 SequoiaCM 安装部署完成后需创建工作区，具体操作步骤如下：

1. 切换至 SequoiaCM 安装用户

    ```lang-bash
    $ su scmadmin
    ```

2. 编辑工作区配置文件 `workspaces.json`

    ```lang-bash
    $ vi sequoiacm-deploy/conf/workspaces.json
    {
        "url":"scmServer:8080/rootsite",
        "userName": "admin",
        "password": "admin", 
        "workspaces":[
            {
                "name":"test_ws",
                "description":"''",
                "batch_sharding_type": "none",
                "batch_file_name_unique": false,
                "site_cache_strategy": "always",
                "meta":{
                    "site":"rootSite",
                    "domain":"meta_domain",
                    "meta_sharding_type":"year"
                },
                "data":[
                    {
                        "site":"rootSite",
                        "domain":"data_domain"
                    }
                ],
                "enable_directory": false,
                "preferred": "rootSite"
            }
        ]
    }
    ```

    >**Note**
    >
    > 工作区配置文件的参数说明可参考[工作区配置参数][deploy]。

3. 创建工作区

    ```lang-bash
    $ ./scm.py workspace --create --conf sequoiacm-deploy/conf/workspaces.json
    ```

###权限配置###

工作区创建完成后，需要将工作区的权限赋予某个用户，才能实现工作区的访问。具体操作步骤如下：

1. 编辑权限配置文件 `users.json`

    ```lang-bash
    $ vi sequoiacm-content/users.json
    {
        "url":"scmServer:8080/rootsite",
        "adminUser":"admin",
        "adminPassword":"admin",
        "roles":[
            {
                "name":"test_role",
                "resources":[
                    {
                        "resourceType":"workspace",
                        "resource":"test_ws",
                        "privilege":"ALL"
                    }
                ]
            }
        ],
        "oldUsers": [
            {
                "name": "admin",
                "password": "admin",
                "roles": ["test_role"]
            }
        ],
        "newUsers": [
            {
                "name": "user",
                "password": "user_password",
                "roles": ["test_role"]
            }
        ]
    }
    ```

    >**Note:**
    >
    > 权限配置文件的参数说明可参考[权限配置参数][deploy]。

2. 执行赋权操作，将工作区权限赋给 admin 和 user 用户

    ```lang-bash
    $ python sequoiacm-content/createusers.py
    ```

至此，SequoiaCM 集群安装部署完毕，用户可使用 java 驱动或 S3 客户端连接 SequoiaCM 进行数据操作，具体示例可参考[开发指南][driver_operation]。

##配置参数##

###安装部署配置参数###

**installconfig**

该配置段用于配置 SequoiaCM 安装路径，以及用于启动 SequoiaCM 的用户名、用户组和密码。参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| InstallPath | SequoiaCM 安装路径 | 是 |
| InstallUser | 用户名<br>如果指定的用户不存在，安装部署时将自动创建 | 是 |
| InstallUserPassword | 用户密码<br>如果字段 InstallUser 指定的用户已存在，则无需指定该参数 | 否 |
| InstallUserGroup | 用户组<br>如果字段 InstallUser 指定的用户已存在，则无需指定该参数 | 否 |

**host**

该配置段用于配置待部署机器的信息，配置后将在对应主机上部署 SequoiaCM，参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| HostName | 主机地址 | 是 | 
| SshPort  | 端口号 | 是 |
| User     | 用户名，需具备 sudo 权限 | 是 |
| Password | 用户密码<br>如果机器已配置互信，则无需填写该参数 | 否 |
| JavaHome | jdk 安装路径<br>该字段为空时，默认在 `/etc/profile` 下搜索 JAVA_HOME 环境变量 | 否 |

**zookeeper**

该配置段用于配置 zookeeper 节点的数量，建议配置为奇数。参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| HostName   | 主机地址| 是
| ServerPort | 服务端口号，格式为 `<端口号1>:<端口号2>`<br>端口号1用于选举 leader，端口号2用于集群内通讯 | 是 |
| ClientPort | 客户端口号 | 是 |

**metasource**

该配置段用于配置元数据存储信息，目前元数据信息仅支持存储在 SequoiaDB 集群，参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| Url      | 协调节点地址，格式为 `<sdbserver>:<svcname>` | 是 |
| Domain   | 数据域 | 是 |
| User     | SequoiaDB 用户名 | 是 | 
| Password | SequoiaDB 用户名密码<br>当指定参数 PasswordFile 时，该参数应填写密文文件的 token；如果密文文件未配置 token，该参数为空即可 | 否 |
| PasswordFile | 密文文件 | 否 |

**datasource**

该配置段用于配置数据存储信息，参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| Name | 数据源名称 | 是 |
| Type | 数据库类型，取值下如下：<br>1：sequoiadb<br>2：hbase<br>3：ceph_s3<br>4：ceph_swift<br>5：hdfs<br>8：sftp | 是 |
| Url  | Url 地址<br>字段 Type 取值为 1 时，该字段取值为协调节点地址，格式为 `<sdbserver>:<svcname>`<br>字段 Type 取值为 3 时，支持配置两个同名数据源作为[主备数据源][primary_standby_cephs3]（第一个配置的为主数据源） | 是 | 
| User | SequoiaDB 用户名 | 是 |
| Password | SequoiaDB 用户名密码<br>当指定参数 PasswordFile 时，该参数应填写密文文件的 token；如果密文文件未配置 token，该参数为空即可 | |
| PasswordFile | 密文文件 | 否 |
| ConnectionConf | 数据源参数配置，仅字段 Type 取值为 hdfs 或 hbase 时有效<br>格式为 `{fs.defaultFS":"hdfs://hostName1:port",...}` | 否 |

**site**

该配置段用于配置站点信息，参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| Name | 站点名 | 是 |
| DatesourceName | 数据源名称 | 是 |
| IsRootSite | 是否为主站点，取值如下：<br>true：主站点<br>false：分站点 | 是 |

**sitestrategy**

该配置段用于配置[站点网络模型][sitestrategy]，参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| Strategy | 站点网络模型，取值如下：<br>network：网状模型<br>start：星型模型 | 是 |

**zone**

该配置段用于配置机房信息，参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| Name   | 机房名 | 是   |

**sitenode**

该配置段用于配置内容管理服务节点，参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| ZoneName | 节点所属机房的名称 | 是 |
| SiteName | 节点所属站点的名称 | 是 |
| HostName | SequoiaCM 主机地址 | 是 |
| Port     | 端口号 | 是 |
| CustomNodeConf | 自定义配置，例如配置 JVM 参数、限制堆内存大小等 | 否 |

**s3node**

该配置段用于配置 S3 服务节点，用户需根据部署规划选择是否配置该部分内容。参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| ZoneName | 节点所属机房的名称 | 是 |
| BindingSite | 节点所属站点的名称 | 是 |
| HostName | SequoiaCM 主机地址 | 是 |
| Port | 端口号 | 是 |
| ServiceName | 节点服务名 | 否 |
| ManagementPort |管理端口号，用于监控节点状态 | 否 |
| CustomNodeConf | 自定义配置，例如配置 JVM 参数、限制堆内存大小等 | 否 |

**elasticsearch**

该配置段用于配置 Elasticsearch 连接信息，如果不需要全文检索服务，这段配置可以填空。参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| Url    | elasticsearch 服务地址，如 'https://192.168.31.20:9200,https://192.168.31.21:9200' | 是 |
| User   | elasticsearch 服务用户名，若 elasticsearch 未设置用户名密码则填空| 否 |
| Password   | elasticsearch 服务密码，若 elasticsearch 未设置用户名密码则填空| 否 |
| CertPath   | elasticsearch 服务 https 证书在当前部署工具所在机器上的路径，该证书文件在 elasticsearch 安装目录的 config/certs/http_ca.crt，当不使用 https 访问时，可以不填写该字段 | 否 |

>**Note:**
>
> 若配置的 elasticsearch 为 6.3 版本而非 8.2，需要在全文检索服务节点配置手动指定配置项 scm.fulltext.es.adapterPath 为 ./jars/es-client-6.3

**servicenode**

该配置段用于配置基础服务节点，用户需根据部署规划进行配置。参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| Zone   | 节点所属机房的名称 | 是 |
| ServiceType | 节点服务类型，取值如下：<br>service-center：注册中心服务节点<br>auth-server：认证服务节点<br>gateway：网关服务节点<br>config-server：配置服务节点<br>schedule-server：调度服务节点<br>admin-server：监控服务节点<br>mq-server：消息队列服务节点<br>om-server：OM 管理服务节点<br>service-trace：链路追踪服务节点<br>fulltext-server：全文索引服务节点 | 是 |
| HostName | SequoiaCM 主机地址 | 是 |
| Port | 端口号 | 是 |
| CustomNodeConf | 自定义配置，例如配置 JVM 参数、限制堆内存大小等 | 否 |

>**Note:**
>
> 在同一个 SequoiaCM 集群中仅支持部署一个 service-trace 节点

**auditsource**

该配置段用于配置审计日志的存储信息。如果未配置该部分内容，默认将审计日志存储至元数据配置段所指定的数据域下。参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| Url      | 协调节点地址，格式为 `<sdbserver>:<svcname>` | 是 |
| Domain   | 数据域 | 是 |
| User     | SequoiaDB 用户名 | 是 | 
| Password | SequoiaDB 用户名密码<br>当指定参数 PasswordFile 时，该参数应填写密文文件的 token；如果密文文件未配置 token，该参数为空即可 | 否 |
| PasswordFile | 密文文件 | 否 |

**daemon**

该配置段用于配置守护进程工具，参数说明如下：

| 参数名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| EnableDaemon | 是否在所有待部署的 SequoiaCM 机器上安装守护进程工具，默认值为 true，表示安装守护进程 | 否 |

###工作区配置参数###

| 字段名 | 说明 | 是否必填 |
| ------ | ---- | ------ |
| url | 网关地址，格式为 `<scmserver>:<port>/<sitename>` | 是 |
| userName | SequoiaCM 管理员用户名，默认为 admin | 是 |
| password | SequoiaCM 管理员用户密码，默认为 admin，与 passwordFile 字段二选一 | 否 |
| passwordFile | SequoiaCM 管理员用户[密码文件][encrypt_password]路径，与 password 字段二选一 | 否 |
| workspace.name | 工作区名 | 是 |
| workspace.description | 工作区描述 | 否 |
| workspace.batch_sharding_type | 指定批次分区类型，默认值为 none，取值如下：<br>month：按月分区<br>quarter：按季分区<br>year：按年分区<br>none：不分区 | 否 |
| workspace.batch_file_name_unique | 指定批次内文件名是否唯一，默认值为 false，表示文件名不唯一 | 否 |
| workspace.site_cache_strategy | 站点缓存策略，取值如下<br>always：文件数据在站点间流动时总是缓存数据到途经的站点上<br>never：数据不会缓存到途经的站点上 | 是 |
| workspace.enable_directory | 是否开启目录功能，默认值为 false，表示不开启目录功能 | 否 |
| workspace.preferred | 网关优先选择处理的站点，默认为主站点，具体说明如下：<br>● 该字段需指定当前工作区内的站点名<br>● 仅对 S3 请求生效，当无法处理 S3 请求时，将直接报错 | 否 |
| workspace.meta.site | 主站点名 | 是 |
| workspace.meta.domain | 数据域，用于存储工作区的元数据 | 是 |
| workspace.meta.meta_sharding_type | 元数据分区类型，默认值为 year，取值如下：<br>month：按月分区<br>quarter：按季分区<br>year：按年分区<br>none：不分区 | 否 |
| workspace.data.site | 主站点名 | 是 |
| workspace.data.domain | 数据域，用于存储工作区的数据 | 是 |

###权限配置参数###

| 字段名 | 说明 | 是否必填 |
| ------ | ---- | -------- |
| url | 网关地址，格式为 `<scmServer>:<port>/<sitename>` | 是 |
| adminUser | SequoiaCM 管理员用户名，默认为 admin | 是 |
| adminPassword | SequoiaCM 管理员用户密码，默认为 admin，与 adminPasswordFile 字段二选一| 否 |
| adminPasswordFile | SequoiaCM 管理员用户[密码文件][encrypt_password]路径，与 adminPassword 字段二选一 | 否 |
| roles.name | 角色名 | 是 |
| roles.resources.resourceType | 角色关联的资源类型，取值如下：<br>workspace：工作区<br>directory：目录 | 是 | 
| roles.resources.resource | 资源名，用户需根据资源类型，指定工作区名或目录名 | 是 |
| roles.resources.privilege | 权限类型，取值如下：<br>CREATE：支持创建操作<br>READ：支持读操作<br>UPDATE：支持更新操作<br>DELETE：支持删除操作<br>ALL：支持所有操作 | 否 |
| oldUsers.name | 旧用户名，即 SequoiaCM 已创建的用户 | 否 |
| oldUsers.password | 旧用户名密码 | 否 |
| oldUsers.roles | 分配给旧用户的角色 | 否 |
| newUsers.name | 新用户名，即 SequoiaCM 未创建的用户<br>执行赋权操作后将自动生成对应的用户 | 否 |
| newUsers.password | 新用户名密码 | 否 |
| newUsers.roles | 分配给新用户的角色 | 否 |

[deploy]:Quick_Start/deploy.md#配置参数
[install_requirement]:Quick_Start/install_requirement.md
[driver_operation]:Development/overview.md
[primary_standby_cephs3]:Architecture/data_storage.md
[node_group]:Architecture/node_group.md
[encrypt_password]:Maintainance/Tools/Scmadmin/encrypt.md
[sitestrategy]:Architecture/site_network.md