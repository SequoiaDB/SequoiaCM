update-quota 子命令用于更新桶的限额信息。

##子命令选项##

| 选项          | 缩写 | 描述                                            | 是否必填 |
| ------------- | ---- | ----------------------------------------------- | -------- |
| --bucket      |  -   | 指定待更新限额信息的桶名                                     | 是 |
| --user        |  -u  | 指定用户名，该用户需拥有桶所属工作区的所有权限（ALL）| 是 |
| --password    |  -p  | 指定用户密码，取值为空时表示采用交互的方式输入密             | 是 |
| --url         |  -   | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>`  | 是 |
| --max-objects |  -   | 指定桶存储对象的最大数量，默认值为 -1，表示不限制桶存储对象的数量 | 否 |
| --max-size    |  -   | 指定桶的最大存储容量，单位为 G 或 M，需手动指定单位，例如 100G<br>该选项与 --max-size-bytes 互斥 | 否 |
| --max-size-bytes  | - | 指定桶的最大存储容量，单位为字节，无需手动指定单位<br>该选项与 --used-size-bytes 互斥 | 否 |
| --used-objects    | - | 指定桶已存储对象的数量                                     | 否    |
| --used-size       | - | 指定桶已存储的容量，单位为 G 或 M，需手动指定单位，例如 100G<br>该选项与 --used-size-bytes 互斥 | 否    |
| --used-size-bytes | - | 指定桶已存储的容量，单位为字节，无需手动指定单位<br>该选项与 --used-size 互斥 | 否    |

>**Note:**
>
> - 不支持同时指定存在互斥关系的选项。
> - 用户可通过选项 --max-objects、--max-size 和 --max-size-bytes 更新桶容量和对象数量的限制，至少指定其中一个选项。如果 --max-size 和 --max-size-bytes 都未指定，将不限制桶的存储容量。

###示例###

将桶 mybucket 的最大存储容量更新为 100G、存储对象的最大数量更新为 10000

```lang-bash
$ s3admin.sh update-quota --bucket mybucket --max-size 100G --max-objects 10000 --url localhost:8080 -u admin -p 
```

更新成功后，将返回如下信息：

```lang-text
update quota successfully.
bucket: mybucket
enable: true
maxObjects: 10000
maxSize: 100.00 GB
usedObjects: 0(0.00%)
usedSize: 0.00 B(0.00%)
lastUpdateTime: 2023-04-09 22:09:36
syncStatus: COMPLETED
```

