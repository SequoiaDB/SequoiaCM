resetpassword 子命令提供修改用户密码的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--reseted-user|   |被修改用户的用户名                                     |是      |
|--new-password|   |新密码                                             |是      |
|--old-password|     |旧密码，修改 admin 角色的用户时需要填写|否      |
|--url   |     |(gateway)网关地址，eg:'localhost:8080'|是      |
|--user|     |管理员用户名         |是      |
|--password|     |管理员密码        |是      |

###示例###

修改普通用户 testuser 的密码，新密码为 testpasswd

   ```lang-javascript
   $  scmadmin.sh resetpassword --reseted-user testuser --new-password testpassword --url localhost:8080 --user admin --password admin