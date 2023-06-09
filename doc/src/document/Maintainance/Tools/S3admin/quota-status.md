quota-status 子命令用于查看桶的限额信息。

##子命令选项##

| 选项        | 缩写 | 描述                                  | 是否必填 |
|-------------| ---- |---------------------------------------| -------- |
| --bucket    | -  | 指定待查看限额信息的桶名                |    是    |
| --user      | -u | 指定用户名，该用户需拥有桶所属工作区的读权限（READ） | 是 |
| --password  | -p | 指定用户密码，取值为空时表示采用交互的方式输入密码           | 是 |
| --url       | -  | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>`  | 是 |
| --force-refresh | -  | 强制刷新额度信息缓存，以查询到最新的额度信息                     | 否    |

###示例###

查看桶 mybucket 的限额信息

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
> - 如果字段 enable 取值为 false，可能是由于异常导致系统自动关闭限额功能。在这种情况下，用户需排查异常后再次[开启限额][enable-quota]。
> - 字段 syncStatus 表示同步状态，取值包括 SYNCING（同步中）、COMPLETED（同步完成）、CANCELED（同步取消）和 DAILED（同步失败）。如果同步失败，用户需手动[触发同步][sync-quota]。

[sync-quota]:Maintainance/Tools/S3admin/sync-quota.md
[enable-quota]:Maintainance/Tools/S3admin/enable-quota.md

