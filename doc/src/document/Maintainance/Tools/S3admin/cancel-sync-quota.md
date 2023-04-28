
cancel-sync-quota 子命令用于中止桶的额度同步。当桶长期处于额度同步状态时系统性能将下降，为避免影响业务，可通过此命令中止额度同步。

##子命令选项##

| 选项        | 缩写 | 描述                                            | 是否必填 |
| ----------- | ---- | ----------------------------------------------- | -------- |
| --bucket    |  -   | 指定待中止额度同步的桶名                               | 是 |
| --user      |  -u  | 指定用户名，该用户需拥有桶所属工作区的所有权限（ALL）| 是 |
| --password  |  -p  | 指定用户密码，取值为空时表示采用交互的方式输入密码           | 是 |
| --url       |  -   | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>`    | 是    |

###示例###

中止桶 mybucket 正在进行中的额度同步：

```lang-bash
$ s3admin.sh cancel-sync-quota --bucket mybucket --url localhost:8080 -u admin -p
```

中止成功后，将返回如下信息：

```lang-text
cancel sync quota successfully:bucket=mybucket
```
