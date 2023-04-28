
sync-quota 子命令用于触发桶的额度同步。执行该命令后系统将统计当前桶已用的容量和对象数量，如果桶内的数据较多，统计时间也会相对较长，从而影响业务性能，因此在该情况下，建议用户在业务空闲时间段内开启限额。

##子命令选项##

| 选项       | 缩写 | 描述                       | 是否必填 |
| ---------- | ---- | -------------------------- | -------- |
| --bucket   |  -   | 指定待触发额度同步的桶名     | 是 |
| --user     |  -u  | 指定用户名，该用户需拥有桶所属工作区的所有权限（ALL）| 是    |
| --password |  -p  | 指定用户密码，取值为空时表示采用交互的方式输入密码           | 是    |
| --url      |  -   | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>`  | 是    |

###示例###

1. 指定桶 mybucket 触发额度同步

    ```lang-bash
    $ s3admin.sh sync-quota --bucket mybucket --url localhost:8080 -u admin -p 
    ```

2. 查看同步状态

    ```lang-bash
    $ s3admin.sh quota-status --bucket mybucket --url localhost:8080 -u admin -p
    ```

    输出结果如下：
    
    ```lang-text
    bucket: mybucket
    enable: true
    maxObjects: 10000
    maxSize: 100.00 GB
    usedObjects: 5000(50.00%)
    usedSize: 50.00 G(50.00%)
    lastUpdateTime: 2023-04-09 22:16:12
    syncStatus: COMPLETED
    ```

    >**Note:**
    >
    > 字段 syncStatus 表示同步状态，取值包括 SYNCING（同步中）、COMPLETED（同步完成）、CANCELED（同步取消）和 DAILED（同步失败）。如果同步失败，用户需再次触发额度同步。


