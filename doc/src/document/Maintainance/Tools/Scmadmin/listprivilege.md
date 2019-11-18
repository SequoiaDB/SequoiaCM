listprivilege 子命令提供查看角色权限的功能。 

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--role |-r     |用户角色 |是      |
|--url   |     |(gateway)网关地址，eg:'localhost:8080'|是      |
|--user|     |管理员用户名         |是      |
|--password|     |管理员密码        |是      |

###示例###

查看角色 role的所有权限

   ```lang-javascript
   $  scmadmin.sh listprivilege -r role --url localhost:8080 --user admin --password admin
   ```