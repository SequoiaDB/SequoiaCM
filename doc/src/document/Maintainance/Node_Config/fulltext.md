公共配置

|配置项| 类型| 说明|
|------|-----|-----|
|详见[公共配置][public_config]|||

SequoiaCM 配置

|配置项| 类型| 说明|
|------|-----|-----|
|scm.jvm.options|str|配置 java jvm 参数，例如：-Xmx2048M -Xms2048M -Xmn1536M，默认为空，即启动节点不添加任何 jvm 参数|
|scm.privilege.heartbeat.interval|num|权限版本号校验间隔时间，当版本号不一致时会刷新版本信息。默认值：10000，单位：毫秒|
|scm.conf.version.siteHeartbeat|num|配置站点版本号校验间隔时间。默认值：180000，单位：毫秒|
|scm.conf.version.workspaceHeartbeat|num|配置工作区版本号校验间隔时间。默认值：180000，单位：毫秒|
|scm.zookeeper.urls|str|zookeeper服务地址(ip1:port1,ip2:port2)|
|scm.zookeeper.cleanJobPeriod|num|配置服务节点清理zookeeper无效节点的周期，默认值：43200000 (12h)，单位：毫秒|
|scm.zookeeper.cleanJobResidualTime|num|配置服务节点将清理残留多久的zookeeper节点，默认值：86400000 (24h)，单位：毫秒|
|scm.fulltext.es.urls|str|elasticsearch 服务地址（http://ip1:port1,http://ip2:port2）|
|scm.fulltext.es.searchScrollTimeout|num|在elasticsearch 通过游标查询（scroll search）时，游标在 elasticsearch 服务端的超时时间，默认值：180000（3min），单位：ms|
|scm.fulltext.es.searchScrollSize|num|在elasticsearch 通过游标查询（scroll search）时，每次获取的最大记录数，默认值：1000|
|scm.fulltext.es.indexShards|num|在elasticsearch 建立索引时，索引的分片数，默认值：5|
|scm.fulltext.es.syncRefreshPolicy|str|在elasticsearch 同步索引数据时，采用的等待策略，默认值：WAIT_UNTIL，可选值：IMMEDIATE、NONE|
|scm.fulltext.es.analyzer|str|在elasticsearch 索引数据时使用的分词器，默认值：ik_max_word|
|scm.fulltext.es.searchAnalyzer|str|在elasticsearch 检索数据时对查询条件的分词器，默认值：ik_smart|
|scm.fulltext.mq.topicPartitionNum|num|文件操作消息主题的分区数，默认值：3|
|scm.fulltext.textualParser.fileSizeLimit|num|超过该大小的文件不允许建立全文索引，默认值：10485760（10m），单位：byte|
|scm.fulltext.textualParser.pic.tessdataDir|str|图片识别引擎 Tesseract 数据目录，默认值：/usr/share/tesseract-ocr/tessdata/|
|scm.fulltext.textualParser.pic.language|str|图片识别引擎 Tesseract 的识别语言，默认值：chi_sim|
|scm.zookeeper.acl.enabled     | boolean  | 是否开启 ZooKeeper ACL 权限控制，默认值：false。详情请见：[ZooKeeper 安全性配置][zookeeper_sercurity]|
|scm.zookeeper.acl.id          | str   | 授权对象，填写用户名密码串（username:password）的加密文件路径|

[public_config]:Maintainance/Node_Config/Readme.md
[zookeeper_sercurity]:Maintainance/Security/Security_Config/zookeeper.md