enable-quota 子命令用于开启桶的限额功能。开启后系统会自动触发额度同步，统计当前桶已用的容量和对象数量，如果桶内的数据较多，统计时间也会相对较长，从而影响业务性能，因此在该情况下，建议用户在业务空闲时间段内开启限额。

##子命令选项##

| 选项        | 缩写 | 描述                                       | 是否必填 |
| ----------- | ---- | ------------------------------------------ | -------- |
| --bucket    |  -   | 指定待开启限额的桶名                                          | 是    |
| --user      |  -u  | 指定用户名，该用户需拥有桶所属工作区的所有权限（ALL） | 是    |
| --password  |  -p  | 指定用户密码，取值为空时表示采用交互的方式输入密码            | 是    |
| --url       |  -   | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>`   | 是    |
| --max-objects | -  | 指定桶存储对象的最大数量，默认值为 -1，表示不限制桶存储对象的数量 | 否 |
| --max-size  |  -   | 指定桶的最大存储容量，单位为 G 或 M，需手动指定单位，例如 100G<br>该选项与 --max-size-bytes 互斥 | 否 |
| --max-size-bytes | - | 指定桶的最大存储容量，单位为字节，无需手动指定单位<br>该选项与 --max-size 互斥          | 否    |
| --used-objects    | - | 指定桶已存储对象的数量                                     | 否    |
| --used-size       | - | 指定桶已存储的容量，单位为 G 或 M，需手动指定单位，例如 100G<br>该选项与 --used-size-bytes 互斥 | 否    |
| --used-size-bytes | - | 指定桶已存储的容量，单位为字节，无需手动指定单位<br>该选项与 --used-size 互斥 | 否    |

>**Note:**
>
> - 不支持同时指定存在互斥关系的选项。
> - 用户可通过选项 --max-objects、--max-size 和 --max-size-bytes 设置桶容量和对象数量的限制，至少指定其中一个选项。如果 --max-size 和 --max-size-bytes 都未指定，将不限制桶的存储容量。
> - 用户可通过选项 --used-objects、--used-size 或 --used-size-bytes 手动设置桶额度信息，设置后能够避免触发额度同步，保障业务性能。

###示例###

1. 开启桶 mybucket 的限额功能，并设置最大存储容量为 100G、存储对象的最大数量为 10000

    ```lang-bash
    $ s3admin.sh enable-quota --bucket mybucket --max-size 100G --max-objects 10000 --url localhost:8080 -u admin -p
    ```

2. 查看是否开启成功

    ```lang-bash
    $ s3admin.sh quota-status --bucket mybucket --url localhost:8080 -u admin -p
    ```

    输出结果如下，字段 enable 取值为 true，且字段 syncStatus 取值为 COMPLETED 时，表示成功开启限额功能

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
    > 如果 enable 取值为 false，可能是由于触发异常导致系统自动关闭限额功能。在这种情况下，用户需排查异常后再次开启限额。