全文检索服务主要负责维护文件的全文索引，对外提供文件的全文检索功能，底层主要依赖 Elasticsearch 实现全文索引的维护以及数据检索。

索引建立
----

全文索引配置是工作区级别的，用户通过为工作区开启全文索引功能，为工作区的文件建立全文索引。当工作区打开全文索引时，全文检索服务会首先为工作区下已存在的文件建立全文索引：

![全文索引建立][fulltext_idx_create_1]

- 由全文检索服务节点负责遍历满足索引条件的文件，并通过 Elasticsearch 建立全文索引
- 调度服务负责监控索引建立过程，统计进度，并在该全文检索服务节点故障时，将索引任务分配给其它节点继续执行

当工作区打开全文索引后，后续内容服务的文件变更操作，将会通过消息队列异步/同步的建立全文索引：

![全文索引建立][fulltext_idx_create_2]

目前全文检索服务支持为如下类型的文件建立全文索引：

- 普通文本文件
- doc、docx 格式的 Word 文件
- xls、xlsx 格式的 Excel 文件
- jpg、png、bmp 格式的图片

全文检索
---

全文检索服务负责处理用户的全文检索请求，并协调内容服务完成查询：

![全文检索][fulltext_search]

- 全文检索服务在 Elasticsearch 仅保存了文件内容的文本化数据以及必要的文件标识数据，所以在 Elasticsearch 检索到目标文件后，全文检索服务还需要查询内容服务获得完整的文件信息


[fulltext_idx_create_1]:Architecture/Microservice/fulltext_create_index_1.png
[fulltext_idx_create_2]:Architecture/Microservice/fulltext_create_index_2.png
[fulltext_search]:Architecture/Microservice/fulltext_search.png