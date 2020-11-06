listprivilege 子命令提供查看角色权限的功能。 

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--role     |-r   |用户角色                                                  |是      |
|--url      |     |(gateway)网关地址，eg:'localhost:8080'                    |是      |
|--user     |     |登录用户名                                              |是      |
|--password |     |登录密码，指定值则使用明文输入，不指定值则命令行提示输入|否      |
|--password-file| |登录密码文件，与 password 互斥                          |否      |

>  **Note:**
>
>  * 参数 --password、--password-file 两者填写其一

###示例###

查看角色 role的所有权限

   ```lang-javascript
   $  scmadmin.sh listprivilege -r role --url localhost:8080 --user admin --password
   ```