alterws 子命令提供修改工作区的功能。

##子命令列表##

|子命令    |描述             |
|----------|-----------------|
|addsite   |向工作区添加站点 |
|updatesites|修改工作区中的站点属性 |
|update-preferred  |更新工作区 preferred 属性|
|disable-directory |设置工作区目录为禁用状态 |

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

###（2）updatesites###

updatesites 支持修改工作区中已存在的站点信息。

####子命令选项####

|选项       |缩写 |描述                                                         |是否必填|
|-----------|-----|-------------------------------------------------------------|--------|
|--name     |-n   |工作区的名字                                                 |是      |
|--data     |-d   |指定修改的站点属性                                            |是      |
|--merge-to |-m   |是否与原属性合并，默认为 true。true: 与指定的分站点属性合并，只更新修改的字段，本次未修改的字段保留原属性。false：本次修改覆盖指定分站点的属性                                          |否      |
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

修改工作区 ws 中的分站点 site3 中的 data_sharding_type 配置。

```lang-javascript
   $  scmadmin.sh alterws updatesites --name ws --data '[{site:"site3", data_sharding_type:{collection_space:"month"}}]' --url localhost:8080/rootsite --user admin --password
```

###（3）update-preferred###

update-preferred 支持更新工作区的 preferred 属性。

####子命令选项####

|选项       |缩写 |描述                                                         |是否必填|
|-----------|-----|-------------------------------------------------------------|--------|
|--name     |-n   |工作区的名字                                                 |是      |
|--preferred|-p   |指定 preferred 属性，目前只能指定为工作区内的某个站点名      |是      |
|--url      |     |(gateway)网关地址，eg:'localhost:8080/rootsite',rootsite是站点服务名（小写） |是      |
|--user     |     |管理员用户名                                                 |是      |
|--password |     |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入   |否      |
|--password-file|     |管理员密码文件，与 password 互斥                         |否      |

>  **Note:**
>
>  * 参数 --password、--password-file 两者填写其一

####示例####

修改工作区 ws 的 preferred 属性为 site3。

```lang-javascript
   $  scmadmin.sh alterws update-preferred --name ws --preferred site3 --url localhost:8080/rootsite --user admin --password
```
###（4）disable-directory###

disable-directory 设置工作区目录为禁用状态。

####子命令选项####

| 选项                  |缩写 |描述                                                         |是否必填|
|---------------------|---|-------------------------------------------------------------|--------|
| --name              |-n |工作区的名字                                                 |是      |
| --url               |   |(gateway)网关地址，eg:'localhost:8080/rootsite',rootsite是站点服务名（小写） |是      |
| --user              |   |管理员用户名                                                 |是      |
| --password          |   |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入   |否      |
| --password-file     |   |管理员密码文件，与 password 互斥                         |否      |

>  **Note:**
>
>  * 参数 --password、--password-file 两者填写其一

####示例####

设置工作区 ws 目录为禁用状态。

```lang-javascript
   $  scmadmin.sh alterws disable-directory --name ws --url localhost:8080/rootsite --user admin --password
```