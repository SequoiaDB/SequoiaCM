listrole 子命令提供查看角色的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--url   |     |(gateway)网关地址，eg:'localhost:8080'|是      |
|--user|     |登录用户名         |是      |
|--password|     |登录密码        |是      |


###示例###

查看角色

   ```lang-javascript
   $  scmadmin.sh listrole --url localhost:8080 --user admin --password admin
   ```