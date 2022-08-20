内容服务以站点的形式存在于SequoiaCM 集群中，提供内容管理的核心功能：文件、批次以及目录的增删改查，后台任务的创建与执行。结构示意图如下：

![内容服务][contentserver]

> * 一个 SequoiaCM 集群中可以存在多个站点，每个站点中的内容服务节点以站点名作为服务名注册至注册中心，即每个站点都是一个独立的微服务。站点的详细介绍可以参考[站点章节][site]  

> * 元数据服务负责储存文件、批次、文件夹等业务的元数据记录。站点的数据服务负责储存文件的内容数据。同一个文件的文件内容数据可以冗余存储至多个站点，可以通过后台任务、跨站点读文件等流程控制站点间文件内容数据的分布。



[contentserver]:Architecture/Microservice/contentserver.png
[site]:Architecture/Business_Concept/site.md