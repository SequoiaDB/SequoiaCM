updateconfig 子命令提供查看全局配置的功能。

##子命令选项##

| 选项       | 缩写 | 描述                                 | 是否必填 |
| ---------- | ---- | ------------------------------------ | -------- |
| --config-name | - | 指定待查看的配置项名称，取值为空时表示查看所有配置 | 否 |
| --url      | - | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>` | 是 |
| --user     | - | 指定管理员用户名                            | 是 |
| --password | - | 指定管理员用户密码，取值为空时表示采用交互的方式输入密码 | 否 |

选项 --config-name 支持指定的配置项如下：

| 配置项 | 描述 |
| ------ | ---- |
| scm.tagLib.defaultDomain | 工作区标签库的默认 domain |

##示例##

查看工作区标签库的默认 Domain

```lang-javascript
$ confadmin.sh show-global-config --url localhost:8080 --config-name scm.tagLib.defaultDomain --user admin --password admin
```