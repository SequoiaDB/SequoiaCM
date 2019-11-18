createuser 子命令提供创建 SequoiaCM 用户的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--new-user|   |SequoiaCM用户名                                           |是      |
|--new-password|   |SequoiaCM密码                                             |是      |
| --password-type|     |密码类型，默认:LOCAL,支持类型:'LOCAL', 'LDAP'       |否      |
|--url   |     |(gateway)网关地址，eg:'localhost:8080'|是      |
|--user|     |管理员用户名         |是      |
|--password|     |管理员密码        |是      |


###示例###

创建 SequoiaCM 用户，用户名为 user，密码为 passwd

```lang-javascript
   $  scmadmin.sh createuser --new-user user --new-password passwd --url localhost:8080 --user admin --password admin
```