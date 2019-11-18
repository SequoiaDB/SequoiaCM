listuser 子命令提供查看 SequoiaCM 用户的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--url   |     |(gateway)网关地址，eg:'localhost:8080'|是      |
|--user|     |登陆用户名         |是      |
|--password|     |登陆密码        |是      |


###示例###

查看SequoiaCM 用户

   ```lang-javascript
   $  scmadmin.sh listuser --url localhost:8080 --user admin --password admin
   ```