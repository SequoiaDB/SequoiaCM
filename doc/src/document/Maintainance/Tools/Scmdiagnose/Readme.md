scmdiagnose 提供 SequoiaCM 集群诊断的相关功能，该工具位于 SequoiaCM 安装目录的 tools 工具包下。当集群出现故障时，可以使用诊断工具对集群进行日志收集、连通性检查和集群信息收集，便于问题的定位及解决。

##参数##

|参数      |缩写  |描述          |
|----------|------|--------------|
|--help    |-h    |获取帮助文档  |

> **Note:**
>
> 获取 scmdiagnose.sh 的帮助文档：scmdiagnose.sh -h

##子命令列表##

| 子命令                        |描述            |
|----------------------------|----------------|
| [log-collect][logcollect]  |收集日志信息，所收集的日志包括 SequoiaCM 节点日志、标准错误输出日志 `error.out` 和系统错误日志 `syserror.log`        |
| [conn-check][conncheck]    |对集群中所有已启动的微服务节点进行连通性检查        |
| [cluster-info][clusterinfo] |收集集群的主机和节点信息        |


[logcollect]:Maintainance/Tools/Scmdiagnose/logcollect.md
[conncheck]:Maintainance/Tools/Scmdiagnose/conncheck.md
[clusterinfo]:Maintainance/Tools/Scmdiagnose/clusterinfo.md
