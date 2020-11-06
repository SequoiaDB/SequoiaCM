resetpassword 子命令提供修改用户密码的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--reseted-user|  |被修改用户的用户名                                        |是      |
|--new-password|  |新密码，指定值则使用明文输入，不指定值则命令行提示输入    |是      |
|--old-password|  |旧密码，指定值则使用明文输入，不指定值则命令行提示输入    |否      |
|--old-password-file|  |旧密码文件，与 old-password 互斥                     |否      |
|--url      |     |(gateway)网关地址，eg:'localhost:8080'                    |是      |
|--user     |     |管理员用户名                                              |是      |
|--password |     |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入|否      |
|--password-file| |管理员密码文件，与 password 互斥                          |否      |

>  **Note:**
>
>  * 如果修改 admin 用户，参数 --old-password、--old-password-file 两者填写其一
>
>  * 参数 --password、--password-file 两者填写其一

###示例###

1. 修改普通用户 testuser 的密码，命令行提示输入新密码、管理员密码

   ```lang-javascript
    $  scmadmin.sh resetpassword --reseted-user testuser --new-password --url localhost:8080 --user admin --password 
   ```
2. 修改管理员用户 scmadmin 的密码，命令行提示输入旧密码、新密码、管理员密码

   ```lang-javascript
    $  scmadmin.sh resetpassword --reseted-user scmadmin --old-password --new-password  --url localhost:8080 --user admin --password 
   ```