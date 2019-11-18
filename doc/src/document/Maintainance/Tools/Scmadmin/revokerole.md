revokerole 子命令提供回收角色权限的功能。

##子命令选项##
|选项       |缩写 |描述                                                      |是否必填|
|-----------|-----|----------------------------------------------------------|--------|
|--role |-r     |用户角色 |是      |
|--type |       |资源类型。默认:workspace，支持类型:'workspace','directory'|否   |
|--resource|   |被授权的资源。支持工作区类型和目录类型，例如:'wsName'或'wsName:/root/dir1'|是      |
|--privilege|  |授权类型。支持类型：'READ','CREATE', 'UPDATE', 'DELETE', 'ALL'|是      |
|--url   |     |(gateway)网关地址，eg:'localhost:8080/rootsite',rootsite是站点服务名 |是      |
|--user|     |管理员用户名         |是      |
|--password|     |管理员密码        |是      |

###示例###

回收角色 role 对工作区 ws01 的所有权限

   ```lang-javascript
   $  scmadmin.sh revokerole --role role --resource ws01 --privilege ALL --url localhost:8080/rootsite --user admin --password admin
   ```