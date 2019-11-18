SequoiaCM 集群中的元数据服务负责整个集群元数据的存储，各个站点的数据服务负责文件内容的存储。元数据服务目前仅支持 SequoiaDB，数据服务支持 SequoiaDB、Hbase、Ceph S3、Ceph Swift、Hdfs。如下是文件内容在各个数据服务上的储存方式：

## SequoiaDB 数据服务 ##

文件内容在 SequoiaDB 上采用 LOB 的形式进行存储，一个文件内容对应一个 LOB，文件内容的 data_id 作为 LOB 的 id。集合空间、集合名与工作区配置相关，如工作区命名为 ws1，配置集合空间按年划分、集合按月划分文件内容，则该工作区的文件内容储存在 "ws1_LOB_年份.LOB_年月" 集合中。

## Hbase 数据服务 ##

文件内容在 Hbase 上作为一条记录进行存储，文件内容的 data_id 作为 RowKey，表结构如下所示：
![Hbase 数据结构][data_storage_hbase]

SequoiaCM 在 Hbase 建立的表具有 SCM_FILE_META、SCM_FILE_DATA 两个列族：

- SCM_FILE_META：该列族下具有两列数据，FILE_SIZE 列表示数据大小，FILE_STATUS 列表示数据状态（Available / Unavailable）。

- SCM_FILE_DATA：在该列族下具有 N 列数据，每一列储存 1M 的文件内容数据。

Hbase 表的命名与工作区配置相关，如工作区命名为 ws1，配置 Hbase 表所属 namespace 为 scm，按月划分文件内容，则该工作区的文件内容存储在 "scm:ws1_SCMFILE_年月" 表中。

## Hdfs 数据服务 ##

文件内容在 Hdfs 上作为一个文件进行存储，文件内容的 data_id 作为文件名。文件存储路径与工作区配置相关，如工作区命名为 ws1，配置工作区在 Hdfs 的根路径为 "/data/sequoiacm"，按年划分文件内容，则该工作区的文件内容储存在 "/data/sequoiacm/ws1_年份" 路径下。

## Ceph S3 数据服务 ##

文件内容在 Ceph S3 上作为一个 Object 进行存储，文件内容的 data_id 作为 Object 的 Key，Object 存储的 Bucket 与工作区配置相关，如工作区命名为 ws1，配置工作区在 Ceph S3 按月划分文件内容，则该工作区的文件内容储存在 "ws1-scmfile-年月" Bucket 下。

## Ceph Swift 数据服务 ##

文件内容在 Ceph Swif 上使用 Object 进行存储。当文件内容小于 5M，使用单个 Object 存储。当文件文件内容大于 5M，使用一个 ManifestObject 及多个 ObjectSegment 存储。文件内容的 data_id 作为 Object Name。Object 存储的 Container 与工作区配置相关，如工作区命名为 ws1，配置工作区在 Ceph Swift 按月划分文件内容，则该工作区的文件内容储存在 "ws1_scmfile_年月" Container 下。
 
[data_storage_hbase]:Architecture/data_storage_hbase.png





