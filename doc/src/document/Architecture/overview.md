SequoiaCM 是基于 Spring Cloud 微服务框架开发的分布式企业内容管理系统。其总体架构如下图所示：

![总体架构][overview]


##对外接口##

目前仅提供 java 驱动供外部系统调用。

##微服务##

SequoiaCM 中各个微服务共同承载了内容管理系统的主要功能和业务逻辑。其中包括 Spring Cloud 组件服务，以及内容管理系统的核心服务。

###Cloud 服务###
cloud 组件服务包括： 网关服务、注册中心、认证服务、监控服务。

1. 网关服务 （Gateway） 基于 Zuul 实现的网关服务，主要负责请求路由和负载均衡功能。

2. 注册中心 （Service Center） 基于 Eureka 实现的注册中心，主要负责各个服务的服务发现和注册功能

3. 认证服务 （Auth Server） 主要负责用户登陆、会话管理、权限管理等功能

4. 监控管理服务 （Admin Server） 基于 Spring Boot Admin 实现的监控服务

###核心服务###

核心服务包括：内容服务、调度服务、配置服务、消息队列服务、全文检索服务。

1. 内容服务 （Content Server） 内容服务以站点的形式存在，主要负责文件管理、目录管理、批次管理等功能

2. 调度服务 （Schedule Server） 主要负责文件迁移、清理任务等后台任务

3. 配置服务 （Config Server） 主要负责工作区配置、文件元数据配置刷新等功能

4. 消息队列服务 （Message Queue Server） 主要负责系统内部的消息存储和投递

5. 全文检索服务 （Fulltext Server） 主要负责对文件全文索引的建立，对外提供全文检索功能

##数据存储##

数据存储主要包括两类：元数据服务、数据服务

1. 元数据服务 （Meta Source） 主要负责存储各个微服务的元数据信息、配置信息，以及文件的元数据信息等。目前仅支持 SequoiaDB 做为元数据服务

2. 数据服务 （Data Source） 隶属于某一个站点，主要负责存储实际文件内容。目前支持存储服务为：SequoiaDB、Hdfs、Hbase、Ceph、Sftp

##依赖服务##

1. Zookeeper：SequoiaCM 集群目前依赖 Zookeeper 提供的集群协调服务，为集群提供分布式锁支持，帮助调度服务的节点选举。

2. Elasticsearch：SequoiaCM 的全文检索能力目前依赖 Elasticsearch 提供支持。




[overview]:SequoiaCM_Intro/overall_arch.png
