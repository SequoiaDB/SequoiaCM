attachrole 子命令提供为SequoiaCM用户添加角色的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--attached-user |     |用户名 |是      |
|--role |-r     |用户角色 |是      |
|--url   |     |(gateway)网关地址，eg:'localhost:8080'|是      |
|--user|     |管理员用户名         |是      |
|--password|     |管理员密码        |是      |

###示例###

为用户 user 添加角色 role

```lang-javascript
   $  scmadmin.sh attachrole --attached-user user --role role --url localhost:8080 --user admin --password admin
```