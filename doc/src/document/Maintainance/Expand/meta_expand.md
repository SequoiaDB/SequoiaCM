随着结构化数据的不断写入，机器的可用磁盘空间越来越小，用户需要进行扩容来满足业务的需要。下面将介绍如何对工作区元数据进行扩容。

##修改元数据域##

###示例###

以工作区 ws，新的数据域 domain2 为例。

   ```lang-javascript
   $  scmadmin.sh alterws update-metadomain --name ws --metadomain domain2 --url localhost:8080/rootsite --user admin --password
   ```

   >  **Note:**
   >
   >  * 修改工作区的元数据域后，新的分区周期的文件元数据会自动落到新的数据域上。

##手动变更集合分区##

由于工作区更换新的元数据域后，数据需要到新的分区周期才会落到新的数据域上。当机器磁盘空间不足以支撑到新的分区周期到来时，需要手动变更已有的集合分区，使数据能够尽快落到新的数据域上。

在手动操作的过程中，用户需要注意以下几点：

1. 操作前需要确定好工作区元数据的 Sharding 策略。

2. 操作前请先熟悉下述的操作指引。

3. 操作示例并不代表适用所有情况，需结合实际业务环境进行操作。

4. 该操作涉及到集合的操作，请小心谨慎操作，避免出错。

5. 文件子集合和历史文件子集合都需要操作。

6. 如果工作区配置了批次的分区规则 batch_sharding_type，批次子集合也需要操作。

###操作指引###

####修改元数据域####

以工作区 ws，新的数据域 domain2 为例。

1. 修改工作区 ws 的元数据为 domain2

    ```lang-javascript
   $  scmadmin.sh alterws update-metadomain --name ws --metadomain domain2 --url localhost:8080/rootsite --user admin --password
    ```

2. 停掉 SCM 集群所有服务

####创建新集合空间####

1. 以新的数据域 domain2 为例，创建集合空间，并指定集合空间的所属域为 domain2

   ```lang-javascript
   > db.createCS("ws_META_1", {Domain: "domain2"})
   ```

   >  **Note:**
   >
   >  * 集合空间的创建参数除了 Domain 配置为新的 domain 外，其余应当以工作区元数据中 meta_location.collection_space 中记录的为准
   >
   >  * 新集合空间名不能与工作区元数据中 extra_meta_cs 记录的重复， 建议命名方式为 wsName_META_n (n为数字，取值为 extra_meta_cs 中记录的最大值 + 1)

2. 将新建的集合空间名 ws_META_1 写进工作区 ws 元数据 extra_meta_cs 里

    ```lang-javascript
   > db.SCMSYSTEM.WORKSPACE.update({$push: {"extra_meta_cs": "ws_META_1"}},{"name": "ws"})
   ```

####文件子集合操作####

以工作区 ws，工作区元数据的 Sharding 策略为 year，当前日期为 202304 ，新的数据域 domain2 为例。

1. 连接元数据所在的 sdb 节点

2. 在新的集合空间 ws_META_1 上创建新的文件子集合

   ```lang-javascript
   > db.ws_META_1.createCL("FILE_2023", {ShardingKey: {id: 1}, ShardingType: "hash", ReplSize: -1, Compressed: true, CompressionType: "lzw", AutoSplit: true, EnsureShardingIndex: false})
   ```

   >  **Note:**
   >
   >  * 集合的创建参数除了工作区元数据中 meta_location.collection 中记录的外，还需要加上SCM系统固定的创建参数，相同参数以元数据中记录的为准，SCM系统固定的创建参数如下：
   >
   >  * {ShardingKey: {id: 1}, ShardingType: "hash", ReplSize: -1, Compressed: true, CompressionType: "lzw", AutoSplit: true, EnsureShardingIndex: false}
   >
   >  * 若 Compressed 参数最终取值为 false，需要将 CompressionType 参数删除

3. 给新的文件子集合 ws_META_1.FILE_2023 创建索引

   ```lang-javascript
   > db.ws_META_1.FILE_2023.createIndex("idx_id", {id: 1}, {Unique: true, Enforced: false})
   > db.ws_META_1.FILE_2023.createIndex("idx_data_id", {data_id: 1}, {Unique: false, Enforced: false})
   > db.ws_META_1.FILE_2023.createIndex("idx_file_name", {name: 1}, {Unique: false, Enforced: false})
   > db.ws_META_1.FILE_2023.createIndex("idx_create_time", {create_time: 1}, {Unique: false, Enforced: false})
   > db.ws_META_1.FILE_2023.createIndex("idx_data_create_time", {data_create_time: 1}, {Unique: false, Enforced: false})
   ```

    >  **Note:**
    >
    >  * 创建索引时索引名建议与示例保持一致

4. 从文件主集合中分离出当前最新的文件子集合，如这里最新的文件子集合为 ws_META.FILE_2023，具体已实际为准。

   ```lang-javascript
   > db.ws_META.FILE.detachCL("ws_META.FILE_2023")
   ```

5. 将上述第 4 步中分离出来的文件子集合 ws_META.FILE_2023 重新挂载到文件主集合 ws_META.FILE 上

   ```lang-javascript
   > db.ws_META.FILE.attachCL("ws_META.FILE_2023", {LowBound: {create_month: "202301"}, UpBound: {create_month: "202305"}})
    ```

   >  **Note:**
   >
   >  * LowBound: 该值为分离出来的文件子集合的分区范围的第一个月份，若元数据 sharding 策略为 year，则为当前年的第一个月份，如 202301；若为 quarter，则为当前季度的第一个月份，如 Q2 则为 202304
   >
   >  * UpBound: 该值为当前月份值 + 1，如当前为 202304，则 UpBound 为 202305

6. 将上述第 2 步创建的文件子集合 ws_META_1.FILE_2023 挂载到文件主集合 ws_META.FILE 上

   ```lang-javascript
   > db.ws_META.FILE.attachCL("ws_META_1.FILE_2023", {LowBound: {create_month: "202305"}, UpBound: {create_month: "202401"}})
   ```

   >  **Note:**
   >
   >  * LowBound: 该值为上述第 5 步的 UpBound
   >
   >  * UpBound: 该值为分离出来的文件子集合的分区范围的最后一个月份 + 1，若元数据 sharding 策略为 year，则为当前年的最后一个月份 + 1，如 202401；若为 quarter，则为当前季度的最后一个月份 + 1，如 Q2 则为 202307

####历史文件子集合操作####

以工作区 ws，工作区元数据的 Sharding 策略为 year，当前日期为 202304 ，新的数据域 domain2 为例。

1. 连接元数据所在的 sdb 节点

2. 在新的集合空间 ws_META_1 上创建新的历史文件子集合

   ```lang-javascript
   > db.ws_META_1.createCL("FILE_HISTORY_2023", {ShardingKey: {id: 1}, ShardingType: "hash", ReplSize: -1, Compressed: true, CompressionType: "lzw", AutoSplit: true, EnsureShardingIndex: false})
   ```

   >  **Note:**
   >
   >  * 集合的创建参数除了工作区元数据中 meta_location.collection 中记录的外，还需要加上SCM系统固定的创建参数，相同参数以元数据中记录的为准，SCM系统固定的创建参数如下：
   >
   >  * {ShardingKey: {id: 1}, ShardingType: "hash", ReplSize: -1, Compressed: true, CompressionType: "lzw", AutoSplit: true, EnsureShardingIndex: false}
   >
   >  * 若 Compressed 参数最终取值为 false，需要将 CompressionType 参数删除

3. 给新的历史文件子集合 ws_META_1.FILE_HISTORY_2023 创建索引

   ```lang-javascript
   > db.ws_META_1.FILE_HISTORY_2023.createIndex("idx_id_version", {id: 1, major_version: 1, minor_version: 1}, {Unique: true, Enforced: false})
   > db.ws_META_1.FILE_HISTORY_2023.createIndex("idx_data_id", {data_id: 1}, {Unique: false, Enforced: false})
   > db.ws_META_1.FILE_HISTORY_2023.createIndex("name_version_idx", {name: 1, version_serial: -1, major_version: -1, minor_version: -1}, {Unique: false, Enforced: false})
   > db.ws_META_1.FILE_HISTORY_2023.createIndex("idx_data_create_time", {data_create_time: 1}, {Unique: false, Enforced: false})
   ```

   >  **Note:**
   >
   >  * 创建索引时索引名建议与示例保持一致

4. 从历史文件主集合中分离出当前最新的历史文件子集合，如这里最新的历史文件子集合为 ws_META.FILE_HISTORY_2023，具体以实际为准。

   ```lang-javascript
   > db.ws_META.FILE_HISTORY.detachCL("ws_META.FILE_HISTORY_2023")
   ```

5. 将上述第 4 步中分离出来的历史文件子集合 ws_META.FILE_HISTORY_2023 重新挂载到历史文件主集合 ws_META.FILE_HISTORY 上

   ```lang-javascript
   > db.ws_META.FILE_HISTORY.attachCL("ws_META.FILE_HISTORY_2023", {LowBound: {create_month: "202301"}, UpBound: {create_month: "202305"}})
   ```

   >  **Note:**
   >
   >  * LowBound: 该值为分离出来的文件子集合的分区范围的第一个月份，若元数据 sharding 策略为 year，则为当前年的第一个月份，如 202301；若为 quarter，则为当前季度的第一个月份，如 Q2 则为 202304
   >
   >  * UpBound: 该值为当前月份值 + 1，如当前为 202304，则 UpBound 为 202305

6. 将上述第 2 步创建的历史文件子集合 ws_META_1.FILE_HISTORY_2023 挂载到历史文件主集合 ws_META.FILE_HISTORY 上

   ```lang-javascript
   > db.ws_META.FILE_HISTORY.attachCL("ws_META_1.FILE_HISTORY_2023", {LowBound: {create_month: "202305"}, UpBound: {create_month: "202401"}})
   ```

   >  **Note:**
   >  
   >  * LowBound: 该值为上述第 5 步的 UpBound
   >
   >  * UpBound: 该值为分离出来的文件子集合的分区范围的最后一个月份 + 1，若元数据 sharding 策略为 year，则为当前年的最后一个月份 + 1，如 202401；若为 quarter，则为当前季度的最后一个月份 + 1，如 Q2 则为 202307

####批次子集合操作####

以工作区 ws，工作区批次的 batch_sharding_type 策略为 year，当前日期为 202304 ，新的数据域 domain2 为例。

1. 连接元数据所在的 sdb 节点

2. 在新的集合空间 ws_META_1 上创建新的批次子集合

   ```lang-javascript
   > db.ws_META_1.createCL("BATCH_2023", {ShardingKey: {id: 1}, ShardingType: "hash", ReplSize: -1, Compressed: true, CompressionType: "lzw", AutoSplit: true, EnsureShardingIndex: false})
   ```

3. 给新的批次子集合 ws_META_1.BATCH_2023 创建索引

   ```lang-javascript
   > db.ws_META_1.BATCH_2023.createIndex("idx_id", {id: 1}, {Unique: true, Enforced: false})
   ```

   >  **Note:**
   >
   >  * 创建索引时索引名建议与示例保持一致

4. 从批次主集合中分离出当前最新的批次子集合，如这里最新的批次子集合为 ws_META.BATCH_2023，具体以实际为准。

   ```lang-javascript
   > db.ws_META.BATCH.detachCL("ws_META.BATCH_2023")
   ```

5. 将上述第 4 步中分离出来的批次子集合 ws_META.BATCH_2023 重新挂载到批次主集合 ws_META.BATCH 上

   ```lang-javascript
   > db.ws_META.BATCH.attachCL("ws_META.BATCH_2023", {LowBound: {create_month: "202301"}, UpBound: {create_month: "202305"}})
   ```

   >  **Note:**
   >
   >  * LowBound: 该值为分离出来的批次子集合的分区范围的第一个月份，若元数据 sharding 策略为 year，则为当前年的第一个月份，如 202301；若为 quarter，则为当前季度的第一个月份，如 Q2 则为 202304
   >
   >  * UpBound: 该值为当前月份值 + 1，如当前为 202304，则 UpBound 为 202305

6. 将上述第 2 步创建的批次子集合 ws_META_1.BATCH_2023 挂载到批次主集合 ws_META.BATCH 上

   ```lang-javascript
   > db.ws_META.BATCH.attachCL("ws_META_1.BATCH_2023", {LowBound: {create_month: "202305"}, UpBound: {create_month: "202401"}})
   ```

   >  **Note:**
   >
   >  * LowBound: 该值为上述第 5 步的 UpBound
   >
   >  * UpBound: 该值为分离出来的批次子集合的分区范围的最后一个月份 + 1，若元数据 sharding 策略为 year，则为当前年的最后一个月份 + 1，如 202401；若为 quarter，则为当前季度的最后一个月份 + 1，如 Q2 则为 202307

7. 上述操作完成后，重启 SCM 集群所有服务