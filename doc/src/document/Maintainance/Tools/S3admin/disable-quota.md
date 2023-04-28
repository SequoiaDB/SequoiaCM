disable-quota 子命令用于关闭桶的限额功能。

##子命令选项##

| 选项       | 缩写 | 描述                   | 是否必填 |
| ---------- | ---- | ---------------------- | -------- |
| --bucket   | -    | 指定待关闭限额功能的桶           | 是    |
| --user     | -u   | 指定用户名，该用户需拥有桶所属工作区的所有权限（ALL） | 是    |
| --password | -p   | 指定用户密码，取值为空时表示采用交互的方式输入密码            | 是    |
| --url      | -    | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>`   | 是    |

###示例###

关闭桶 mybucket 的限额功能：

```lang-bash
$ s3admin.sh disable-quota --bucket mybucket --url localhost:8080 -u admin -p 
```

关闭成功后，将返回如下信息：

```lang-text
disable quota successfully:bucket=mybucket
```