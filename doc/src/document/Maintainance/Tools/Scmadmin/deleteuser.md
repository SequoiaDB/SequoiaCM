deleteuser 子命令提供删除 SequoiaCM 用户的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--delete-user|   |SequoiaCM用户名                                           |是      |
|--url   |     |(gateway)网关地址，eg:'localhost:8080'|是      |
|--user|     |管理员用户名         |是      |
|--password|     |管理员密码        |是      |


###示例###

删除SequoiaCM 用户，用户名为 user

   ```lang-javascript
   $  scmadmin.sh deleteuser --delete-user user --url localhost:8080 --user admin --password admin
   ```