grantrole 子命令提供赋予角色资源操作权限的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--role     |-r   |用户角色                                                  |是      |
|--type     |     |资源类型。默认:workspace，支持类型:'workspace','directory'|否   |
|--resource |     |被授权的资源。支持工作区类型和目录类型，例如:'wsName'或'wsName:/root/dir1'|是      |
|--privilege|     |授权类型。支持类型：'READ','CREATE', 'UPDATE', 'DELETE', 'ALL'|是      |
|--url      |     |(gateway)网关地址，eg:'localhost:8080/rootsite',rootsite是站点服务名 |是      |
|--user     |     |管理员用户名                                              |是      |
|--password |     |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入|否      |
|--password-file| |管理员密码文件，与 password 互斥                          |否      |

>  **Note:**
>
>  * 参数 --password、--password-file 两者填写其一

###示例###

赋予角色 role 对工作区 ws01 的可读权限

   ```lang-javascript
   $  scmadmin.sh grantrole --role role --resource ws01 --privilege READ --url localhost:8080/rootsite --user admin --password
   ```