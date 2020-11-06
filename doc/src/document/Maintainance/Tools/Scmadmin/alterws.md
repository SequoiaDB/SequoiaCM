alterws 子命令提供修改工作区的功能。

##子命令列表##

|子命令    |描述             |
|----------|-----------------|
|addsite   |向工作区添加站点 |

##子命令介绍##

###（1）addsite###

addsite 支持向工作区增加分站点。

####子命令选项####

|选项       |缩写 |描述                                                         |是否必填|
|-----------|-----|-------------------------------------------------------------|--------|
|--name     |-n   |工作区的名字                                                 |是      |
|--data     |-d   |指定新增的分站点                                             |是      |
|--url      |     |(gateway)网关地址，eg:'localhost:8080/rootsite',rootsite是站点服务名（小写） |是      |
|--user     |     |管理员用户名                                                 |是      |
|--password |     |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入   |否      |
|--password-file|     |管理员密码文件，与 password 互斥                         |否      |

>  **Note:**
>
>  * --data JSON格式描述，支持字段与字段含义见子命令 createws 的 --data 参数
>
>  * 参数 --password、--password-file 两者填写其一

####示例####

为工作区 ws 添加分站点 site3，添加后在 site3 上可以访问工作区 ws 的文件。

```lang-javascript
   $  scmadmin.sh alterws addsite --name ws --data '[{site:"site3",domain:"dataDomain"}]' --url localhost:8080/rootsite --user admin --password
```