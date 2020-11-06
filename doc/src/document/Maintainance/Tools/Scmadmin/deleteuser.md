deleteuser 子命令提供删除 SequoiaCM 用户的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--delete-user|   |SequoiaCM用户名                                           |是      |
|--url      |     |(gateway)网关地址，eg:'localhost:8080'                    |是      |
|--user     |     |管理员用户名                                              |是      |
|--password |     |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入|否      |
|--password-file| |管理员密码文件，与 password 互斥                          |否      |

>  **Note:**
>
>  * 参数 --password、--password-file 两者填写其一

###示例###

删除SequoiaCM 用户，用户名为 user

   ```lang-javascript
   $  scmadmin.sh deleteuser --delete-user user --url localhost:8080 --user admin --password
   ```