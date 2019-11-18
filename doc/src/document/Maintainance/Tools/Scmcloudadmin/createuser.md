createuser 子命令提供创建用户的功能。

##子命令选项##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|----------------------------------------------------   |--------|
|--user     |-u   |新建用户的用户名                                       |是      |
|--password |-p   |新建用户的密码                                         |是      | 
|--url      |     |网关地址，如：gateway-host:8080                        |是      |
|--admin-user|     |管理员用户名                                          |是      |
|--admin-password|     |管理员密码                                        |是      |

###示例###

创建一个名为 test_user 的用户

   ```lang-javascript
   $ scmcloudadmin.sh  createuser --user test_user --password  test_pwd --url localhost:8080 --admin-user admin --admin-password admin
   ```
