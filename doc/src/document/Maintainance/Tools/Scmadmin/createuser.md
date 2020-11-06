createuser 子命令提供创建 SequoiaCM 用户的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--new-user |     |SequoiaCM用户名                                           |是      |
|--new-password|  |SequoiaCM密码，指定值则使用明文输入，不指定值则命令行提示输入|是      |
|--password-type| |密码类型，默认:LOCAL,支持类型:'LOCAL', 'LDAP'             |否      |
|--url      |     |(gateway)网关地址，eg:'localhost:8080'                    |是      |
|--user     |     |管理员用户名                                              |是      |
|--password |     |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入|否      |
|--password-file| |管理员密码文件，与 password 互斥                          |否      |

>  **Note:**
>
>  * 参数 --password、--password-file 两者填写其一

###示例###

创建 SequoiaCM 用户，用户名为 user，命令行提示输入管理员密码

```lang-javascript
   $  scmadmin.sh createuser --new-user user --new-password --url localhost:8080 --user admin --password
```