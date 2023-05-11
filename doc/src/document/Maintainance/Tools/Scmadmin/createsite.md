createsite 子命令提供创建站点的功能。

##子命令选项##

| 选项            |缩写 | 描述                                                                                              |是否必填|
|---------------|-----|-------------------------------------------------------------------------------------------------|--------|
| --name        |-n   | 新建站点的名字，大小写不敏感                                                                                  |是      |
| --dsurl       |     | 新建站点的数据存储服务地址,仅数据源类型为非hdfs或hbase时有效，<br>格式为:'server1:11810,server2:11810'                       |否      |
| --dsuser      |     | 新建站点的数据存储服务用户名，不指定则用户名为空                                                                        |否      |
| --dspasswd    |     | 新建站点的数据存储服务密码文件绝对路径，不指定则密码为空                                                                    |否      |
| --dstype      |     | 新建站点的数据存储服务类型，可选参数:1（sequoiadb），2（hbase），<br>3（ceph_s3），4（ceph_swift），5（hdfs），8（sftp），不指定则类型为 1 |否      |
| --no-check-ds |    | 创建站点时，不对数据源做可用性检查                                                                               |否      |
| --root        |-r   | 设置新建站点为主站点                                                                                      |否      |
| --dsconf      |     | 数据源参数配置, 仅数据源类型为hdfs或hbase时有效, <br>格式为:'{"fs.defaultFS":"hdfs://hostName1:port",...}'           |否      |
| --continue    |     | 新建站点为主站点时，如果主站点数据源已经存在部分元数据，可设置此选项继续<br>完善站点元数据                                                 |否      |
| --gateway     |     | 网关地址，格式为:'host1:port,host1:port'                                                                |是      |
| --user        |     | 管理员用户名                                                                                          |是      |
| --passwd      |     | 管理员密码，指定值则使用明文输入，不指定值则命令行提示输入                                                                   |否      |
| --passwd-file |   | 管理员密码文件，与 passwd 互斥                                                                             |否      |
| --mdsurl      |     | 主站点元数据存储服务的地址，格式为:'server1:11810,server2:11810'                                                 |否      |
| --mdsuser     |     | 主站点元数据存储服务的用户名，不指定则用户名为空                                                                        |否      |
| --mdspasswd   |     | 主站点元数据存储服务的密码文件绝对路径，不指定则密码为空                                                                    |否      |
| --stagetag    |     | 指定站点的阶段标签，需保证对应的标签已存在<br>如果阶段标签列表中不存在所需标签，用户可参考[生命周期操作][lifecycle_config]进行创建  |否       |

>  **Note:**
>
>  * 数据源的密码文件需提前生成并分发至需要创建内容服务节点的机器上，请参考 [sendpassword 命令][sendpassword_tool]
>
>  * 参数 --mdsurl、--mdsuser、--mdspasswd 用于指定主站点中元数据存储服务的地址和用户名、密码。以便工具获取系统的元数据信息
>  
>  * 参数 --passwd、--passwd-file 两者填写其一
>
>  * 当本机存在 ContentServer 节点时，可以不填写参数 --mdsurl、--mdsuser、--mdspasswd，工具自动从配置文件 conf/scm/<节点目录>/application.properties 中读取主站点中元数据存储服务 SequoiaDB 的地址和用户名、密码
>
>  * 数据存储服务 Hbase 的支持版本为 1.1.12，Ceph 的支持版本为 10.2.10
>
>  * 3.0.0 版本中数据存储服务增加支持hdfs数据源(--dstype 参数为 5), 支持版本为2.5.2
>  
>  * 需在config节点和gateway节点启动后，再进行站点创建。


##数据源配置说明及示例##

###1.SequoiaDB###

1. 创建主站点，命名为 rootSite，指定元数据存储服务地址为 metaServer1:11810,metaServer2:11810，数据存储服务地址为 dataServer1:11810,dataServer2:11810，数据存储服务类型为 SequoiaDB，用户名为 sdbadmin, 密码文件绝对路径为 /home/scmadmin/sdb.passwd

```lang-javascript
   $ scmadmin.sh createsite --name rootSite --root --dstype 1 --dsurl dataServer1:11810,dataServer2:11810 --dsuser sdbadmin --dspasswd /home/scmadmin/sdb.passwd --mdsurl metaServer1:11810,metaServer2:11810 --mdsuser sdbadmin --mdspasswd /home/scmadmin/sdb.passwd --gateway server2:8080 --user admin --passwd 
```

2. 创建分站点，并命名为 site2，数据存储服务类型不指定则默认为 SequoiaDB，用户名密码均为 sdbadmin

```lang-javascript
   $ scmadmin.sh createsite --name site2 --dsurl dataServer3:11810,dataServer4:11810 --dsuser sdbadmin --dspasswd /home/scmadmin/sdb.passwd --mdsurl metaServer1:11810,metaServer2:11810 --mdsuser sdbadmin --mdspasswd /home/scmadmin/sdb.passwd --gateway server2:8080 --user admin --passwd 
```

>  **Note:**
>
>  * 创建SequoiaDB数据源类型站点时，需要指定--dsurl参数，不需要指定--dsconf参数
>
>  * 新创建的分站点 site2 的数据存储服务地址 dsurl 为 dataServer3:11810,dataServer4:11810，dsuser 为 sdbadmin，dspassword 为数据源用户密码路径
>
>  * 主站点元数据存储服务地址 mdsurl 为 metaServer1:11810，metaServer2:11810 ，mdsuser 为 sdbadmin，mdspasswd 为 /home/scmadmin/sdb.passwd

###2.Hbase###
数据存储服务类型为hbase时，不需要指定dsurl参数，但需要指定dsconf参数，dsconf参数为json格式，具体为连接hbase服务所需配置。建议参数列表如下：
####dsconf配置####
|配置项                       |描述                                                          |建议设置        |                                                                                                                      
|---------------------------|----------------------------------------------------------------|----------------|
|hbase.zookeeper.quorum     |管理hbase服务的zookeeper地址，用于客户端连接                    |zookeeper地址   |
|hbase.client.retries.number|客户端操作失败最大重试次数，默认值为15                          |建议减小,如3~5  |
|hbase.client.pause         |客户端操作重试等待时间，默认值为100ms                           |建议减小,如20~50|

>  **Note:**
>
>  * 其他Hbase相关配置参数可根据需求添加，如hbase.rpc.timeout、hbase.client.operation.timeout、hbase.client.scanner.timeout.period等
>
>  * 当Hbase服务异常时，默认的失败重试相关参数较大，容易scm服务长时间无响应，应适当减小hbase.client.retries.number 与 hbase.client.pause等相关参数

####示例####
创建分站点，并命名为 site3，数据存储服务类型指定为Hbase

```lang-javascript
   $ scmadmin.sh createsite --name site3 --dstype 2 --dsconf '{"hbase.zookeeper.quorum":"zk1,zk2,zk3", "hbase.client.retries.number":"5", "hbase.client.pause":"50"}' --mdsurl metaServer1:11810,metaServer2:11810 --mdsuser sdbadmin --mdspasswd /home/scmadmin/sdb.passwd  --gateway server2:8080 --user admin --passwd
```

>  **Note:**
>
>  * 创建Hbase类型分站点，名称为site3，数据服务的连接地址为"zk1,zk2,zk3"，客户端重试次数为5，重试等待时间为50ms
>
>  * 主站点元数据存储服务地址 mdsurl 为 metaServer1:11810,metaServer2:11810 ，mdsuser 为 sdbadmin，mdspasswd 为 /home/scmadmin/sdb.passwd

###3.CephS3###

####示例####

创建分站点，并命名为site4，数据存储服务类型指定为ceph_s3

```lang-javascript
   $ scmadmin.sh createsite --name site4 --dstype 3 --dsurl http://cephS3Server1:port,http://cephS3Server2:port --dsuser accessKey --dspasswd secretKeyFilePath --gateway server2:8080 --user admin --passwd
```

> **Note:**
>
> - 创建 CephS3 类型分站点，名称为 site4，数据服务的两个连接拥有相同的用户名和密码 dsuser 和 dspasswd 。
> - CephS3 数据服务支持[主备库][primary_standby_cephs3]，以第一个 URL 作为主库，第二个 URL 作为备库。
> - 当两个库用户名密码不一致时，可以将其拼接在 URL 上，如：accessKey:secretKeyFilePath@http://cephS3Server2:port 。连接 CephS3 时，会优先使用地址上拼接的用户名和密码，当地址上未拼接用户名和密码时，才使用 dsuser 和 dspasswd 。
> - 若通过 Nginx 代理访问 ceph_s3，为防止 Nginx 改写 Host 请求头导致 S3 签名校验失败，请加入 Nginx 配置: proxy_set_header Host $host:$server_port 来规避 Host 的修改，示例配置如下：

``` 
  upstream cephS3Server {
      server 192.168.10.100:8080;
      server 192.168.10.101:8080;
  }
  server {
      listen 8000;
      server_name myServer;
      location / {
          proxy_pass http://cephS3Server;
          proxy_set_header Host $host:$server_port;
      }
  }
```

###4.Hdfs###
数据存储服务类型为Hdfs时，不需要指定dsurl参数，但需要指定dsconf参数，dsconf参数为json格式，具体为连接Hdfs服务所需配置。建议参数列表如下：
####dsconf配置####
约定Hdfs服务端为高可用集群模式，具体配置项如下：

|配置项                                      | 描述                          | 建议设置                                                                              |                                                                                                                      
|--------------------------------------------|-----------------------------|-----------------------------------------------------------------------------------|
|dfs.nameservices                            | hdfs高可用集群模式下nameservice节点列表 | scmserver(与服务端配置一致)                                                               |
|fs.defaultFS                                | 文件系统名称                      | hdfs://scmserver(与服务端配置一致)                                                        |
|dfs.ha.namenodes.scmserver                  | hdfs高可用集群模式下namenode节点列表    | nn1,nn2  (与服务端配置一致)                                                               |
|dfs.namenode.rpc-address.<br>scmserver.nn1      | namenode节点1地址               | host1:port1(与服务端配置一致)                                                             |
|dfs.namenode.rpc-address.<br>scmserver.nn2      | namenode节点2地址               | host2:port2(与服务端配置一致)                                                             |
|dfs.client.failover.proxy.<br>provider.scmserver| hdfs客户端failover代理类          | org.apache.hadoop.hdfs.<br>server.namenode.ha.<br>ConfiguredFailoverProxyProvider |
|dfs.ha.automatic-failover.enabled.scmserver | 是否开启 namenode 失败自动切换        | true                                                                              |
|dfs.client.failover.max.attempts            | hdfs客户端重试次数，默认值为15          | 建议减小,如3~5                                                                         |
|dfs.client.failover.sleep.base.millis       | hdfs客户端重试等待时间最小间隔，默认值为500ms | 建议减小,如50~100                                                                      |
|dfs.client.failover.sleep.max.millis        | hdfs客户端重试等待时间最大间隔，默认值为15000ms | 建议减小,如100~500                                                                     |

>  **Note:**
> 
>  * 表中出现的 “fs.defaultFS” ，其设置的内容需要与 hdfs 服务端中 core-site.xml 配置文件配置的一致
> 
>  * 表中出现的 “scmserver” ，是 hdfs 高可用集群模式下 nameservices 节点的名称， “nn1”，“nn2” 为 nameservices 节点的 namenode 节点列表名称（在此处，nameservices 节点名称为 scmserver），在 hdfs 服务端中 nameservices 名称和 namenode 名称均是可自定义字符类型，这里具体配置时需要与 hdfs 服务端中 hdfs-site.xml 配置文件的配置保持一致
>
>  * 当Hdfs服务异常时，默认的失败重试相关参数较大，容易scm服务长时间无响应; 可以根据实际需要配置并适当减小dfs.client.failover.max.attempts、dfs.client.failover.sleep.base.millis、dfs.client.failover.sleep.max.millis等相关参数。SCM 将失败重试相关参数默认设置为 dfs.client.failover.max.attempts: 5, dfs.client.failover.sleep.base.millis: 100, dfs.client.failover.sleep.max.millis: 500。
>
>  * dfs.ha.automatic-failover.enabled.scmserver 配置为 true 时为开启，没有配置此项时默认为不开启，当 namenode 失败时，需要手动切换 namenode
> 
>  * 其他Hdfs相关配置参数可根据需求添加

####示例####

创建分站点，并命名为 site5，数据存储服务类型指定为Hdfs，dsuser 为 root

```lang-javascript
   $ scmadmin.sh createsite --name site5 --dstype 5 --dsuser root --dspasswd sequoiadb --dsconf '{"fs.defaultFS":"hdfs://scmserver", "dfs.nameservices":"scmserver", "dfs.ha.namenodes.scmserver":"nn1,nn2", "dfs.client.failover.proxy.provider.scmserver":"org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider", "dfs.namenode.rpc-address.scmserver.nn1":"host1:port1","dfs.namenode.rpc-address.scmserver.nn2":"host2:port2"}' --gateway server2:8080 --user admin --passwd admin --mdsurl metaServer1:11810,metaServer2:11810 --mdsuser sdbadmin --mdspasswd /home/scmadmin/sdb.passwd 
```

> **Note:**
> 
> * dsuser、dspasswd 分别指定 ssh 连接的服务器用户名、密码
> 
> * 创建Hdfs类型分站点，名称为site5, hdfs文件系统名称为"hdfs://scmserver"，nameservices 为 scmserver，scmserver 的 namenode 列表为 nn1、nn2，nn1、nn2 的具体地址分别为 host1:port1、host2:port2
>
> * 主站点元数据存储服务地址 mdsurl 为 metaServer1:11810 ，metaServer2:11810 （可配置多个，根据实际需求配置），mdsuser 为 sdbadmin，mdspasswd 为 /home/scmadmin/sdb.passwd


###5.Sftp###

####示例

创建分站点，并命名为 site5，数据存储服务类型指定为 Sftp

```lang-javascript
   $ scmadmin.sh createsite --name site5 --dstype 8 --dsurl 192.168.1.100:22 --dsuser root --dspasswd secretKeyFilePath --mdsurl metaServer1:11810,metaServer2:11810 --mdsuser sdbadmin --mdspasswd /home/scmadmin/sdb.passwd --gateway server:8080 --user admin --passwd
```

> **Note:**
> 
> - dsurl、dsuser、dspasswd 分别指定 ssh 连接的服务器地址、用户名和密码文件路径。
> 
> - 由于 Linux 服务器一般会限制 ssh 连接的会话数量，因此文件操作的并发能力受到最大会话数量的限制（一般默认为 10），建议修改 ssh 的最大会话数量（修改完后需要重启该站点下的节点）。
> 
> - 由于每启动一个 Sftp 站点下的节点都会占用一个 ssh 连接，因此一个 Sftp 站点下可启动的节点数量受 Linux 服务器最大连接数的限制，一般为 10-60，建议调整 ssh 的最大连接数量。

####调整 ssh 的最大会话数量####
```lang-javascript
   # 编辑 Linux 上的 ssh 配置文件
   $ vi /etc/ssh/sshd_config
   
   # 修改 MaxSessions 为 1000，表示最高支持 1000 个对文件操作的并发
   MaxSessions 1000
   
   # 重启 sshd
   $ service sshd restart 或 /etc/init.d/sshd restart （不同 Linux 版本的重启方式会有所不同）
```


####调整 ssh 的最大连接数量####
```lang-javascript
   # 编辑 Linux 上的 ssh 配置文件
   $ vi /etc/ssh/sshd_config
   
   # 修改 MaxStartups 为 100:30:200，表示初始最高支持 100 个连接，当连接数达到 100 后，有 30% 的几率拒绝，达到 200 后直接拒绝
   MaxStartups 100:30:200
   
   # 重启 sshd
   $ service sshd restart 或 /etc/init.d/sshd restart （不同 Linux 版本的重启方式会有所不同）
```

[lifecycle_config]:Development/Java_Driver/lifecycle_operation.md
[sendpassword_tool]:Maintainance/Tools/Scmadmin/sendpassword.md
[primary_standby_cephs3]:Architecture/data_storage.md