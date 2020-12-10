createws 子命令提供工作区的创建功能。

##子命令选项##

|选项           |缩写 |描述                                                                                    |是否必填|
|---------------|-----|----------------------------------------------------------------------------------------|--------|
|--name         |-n   |工作区的名字                                                                            |是      |
|--meta         |-m   |指定工作区元数据的存储位置及参数                                                        |是      |
|--data         |-d   |指定工作区数据的存储位置                                                                |是      |
|--description  |     |工作区描述                                                                              |否      |
|--disable-directory |     |目录功能不开启，不指定默认开启目录功能                                             |否      |
|--batch-sharding-type|     |批次分区类型，可选类型值：none，month，quarter，year，不指定默认为 none           |否      |
|--batch-id-time-regex|     |批次ID时间信息正则表达式，不指定默认为 null                                       |否      |
|--batch-id-time-pattern|     |批次ID时间格式，不指定默认为null                                                |否      |
|--batch-file-name-unique|     |批次内的文件名唯一，不指定默认批次内的文件允许重名                             |否      |
|--url          |     |(gateway)网关地址，eg:'localhost:8080/rootsite',rootsite是站点服务名（小写）            |是      |
|--user         |     |管理员用户名                                                                            |是      |
|--password     |     |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入                              |否      |
|--password-file|     |管理员密码文件，与 password 互斥                                                        |否      |



> **Note:**  
>  
> * --meta 选项只能指定为主站点，并且 --data 参数必须包含主站点。
>
> * 参数 --password、--password-file 两者填写其一
>
> * 开/闭目录功能相关说明详见[目录][directory]
>
> * 设置批次相关说明详见[批次][batch]

选项 --meta 接受一个 JSON Object 格式的字符串，表示元数据站点，支持的 Key 如下：

|Key            |Value Type |描述                                                                                    |是否必填|
|---------------|-----------|----------------------------------------------------------------------------------------|--------|
|site           |str        |站点的名字                                                                              |是      |
|domain         |str        |数据落在指定命名的域                                                                    |是      |
|meta_sharding_type|str     |指定元数据的 Sharding 策略，支持的 Sharding 策略为：'year','quarter','month'，默认：'year'|否|
|meta_options   |object     |增加元数据 CollectionSpace、Collection 的创建参数，Collection 创建参数对文件元数据子表生效，如：meta_options:{collection_space:{LobPageSize:262144},collection:{ReplSize:-1}}|否|


选项 --data 接受一个 JSON Array 格式的字符串，元素为 Object, 表示一组数据站点，每种类型的数据站点支持的 Key 如下：

SequoiaDB 数据站点：

|Key            |Value Type |描述                                                                                    |是否必填|
|---------------|-----------|----------------------------------------------------------------------------------------|--------|
|site           |str        |站点的名字                                                                              |是      |
|domain         |str        |数据落在指定命名的域                                                                    |是      |
|data_sharding_type|object  |指定数据 CollectionSpace、Collection 的 Sharding 策略，CollectionSpace 的默认 Sharding 策略为 'year',Collection 的默认 Sharding 策略为 'month'，Collection 的 Sharding 策略不支持 'none'，如：data_sharding_type:{collection_space:"year",collection:"month"}|否|
|data_options   |object     |指定数据 CollectionSpace、Collection 的创建参数，如：data_options:{collection_space:{LobPageSize:262144},collection:{ReplSize:-1}}|否|

Hbase 数据站点：

|Key            |Value Type |描述                                                                                    |是否必填|
|---------------|-----------|----------------------------------------------------------------------------------------|--------|
|site           |str        |站点的名字                                                                              |是      |
|data_sharding_type|str     |指定数据的 Sharding 策略，默认 Sharding 策略为 'month'                                  |否      |
|hbase_namespace|str        |指定一个已存在的 namespace，文件数据表将在该 namespace 下建立，默认使用 hbase 内置 namespace：'default'|否      |   

Ceph_Swift 数据站点：

|Key            |Value Type |描述                                                                                    |是否必填|
|---------------|-----------|----------------------------------------------------------------------------------------|--------|
|site           |str        |站点的名字                                                                              |是      |
|data_sharding_type|str     |指定数据的 Sharding 策略，默认 Sharding 策略为 'month'                                  |否      |

Hdfs 数据站点：

|Key            |Value Type |描述                                                                                    |是否必填|
|---------------|-----------|----------------------------------------------------------------------------------------|--------|
|site           |str        |站点的名字                                                                              |是      |
|data_sharding_type|str     |指定数据的 Sharding 策略，默认 Sharding 策略为 'month'                                  |否      |
|hdfs_file_root_path|str    |指定文件数据存放的路径，默认：'/scm'                                                    |否      |

Ceph_S3 数据站点：

|Key            |Value Type |描述                                                                                    |是否必填|
|---------------|-----------|----------------------------------------------------------------------------------------|--------|
|site           |str        |站点的名字                                                                              |是      |
|data_sharding_type|str     |指定数据的 Sharding 策略，默认 Sharding 策略为 'month'                                  |否      |
|container_prefix|str       |指定桶名前缀（注意定义的前缀需要符合 S3 的桶名规范），默认前缀：workspaceName-scmfile（workspace 名字中的大写字母会被转成小写，'_' 会被转为 '-'）|否|

sharding 类型说明：

|类型  |描述                 |例子                    |
|------|---------------------|------------------------|
|year  |按年分集合空间/集合  |workspaceName_LOB_2017  |
|quarter|按季度分集合空间/集合|workspaceName_LOB_2017Q3|
|month |按月分集合空间/集合  |workspaceName_LOB_201707|
|none  |不分表               |workspaceName_LOB       |

###示例###

1. 采用默认参数创建一个名为 ws 的工作区

   ```lang-javascript
   $ scmadmin.sh createws --name ws --meta '{site:"rootSite",domain:"metaDomain"}' --data '[{site:"rootSite",domain:"dataDomain"}，{site:"site2",domain:"dataDomain"}]' --url localhost:8080/rootsite --user admin --password 
   ```
> **Note:** 
>
> * 主站点名为 rootSite，分站点名为 site2
>
> * 元数据存储在 rootSite 元数据存储服务的域 metaDomain 中
>
> * 数据存储在 rootSite 数据存储服务的域 dataDomain 和 site2 数据存储服务的域 dataDomain 中
>
> * 使用默认方式对数据进行分区
>
> * 使用默认的选项创建collection、collectionspace


2. 采用自定义批次参数创建一个名为 ws 的工作区

   ```lang-javascript
   $ scmadmin.sh createws --name ws --meta '{site:"rootSite",domain:"metaDomain"}' --data '[{site:"rootSite",domain:"dataDomain"}]' --batch-sharding-type month --batch-id-time-regex '\w{1,20}-\d{8}' --batch-id-time-pattern yyyyMMdd  --url localhost:8080/rootsite --user admin --password
   ```
> **Note:** 
>
> * 主站点名为 rootSite
>
> * 元数据存储在 rootSite 元数据存储服务的域 metaDomain 中
>
> * 数据存储在 rootSite 数据存储服务的域 dataDomain
>
> * 工作区中批次的分区策略为 'month'，时间信息的格式为 yyyyMMdd
>
> * 批次ID获取时间信息的正则表达式为 \w{1,20}-\d{8}。表示1-20个合法字符(大小写字母、数字、下划线)、连字符、8个数字（对应上述的日期格式yyyyMMdd）。合法自定义ID如：BatchId_1523-20200903


3. 采用自定义参数创建一个名为 ws 的工作区

   ```lang-javascript
   $ scmadmin.sh createws --name ws --meta '{site:"rootSite",domain:"metaDomain",meta_sharding_type:"year",meta_options:{collection_space:{LobPageSize:262144},collection:{ReplSize:1}}}' --data '[{site:"rootSite",domain:"dataDomain",data_sharding_type:{collection_space:"quarter",collection:"month"},data_options:{collection_space:{LobPageSize:262114},collection:{ReplSize:1}}}]' --batch-sharding-type month --batch-id-time-regex '(?<=\w{1,50}\.[^.]{1,50}\.)(\d{4}-\d{2}-\d{2})(?=\..{1,200})' --batch-id-time-pattern yyyy-MM-dd --batch-file-name-unique --disable-directory --url localhost:8080/rootsite --user admin --password
   ```
> **Note:** 
>
> * 主站点名为 rootSite
>
> * 元数据存储在 rootSite 元数据存储服务的域 metaDomain 中，指定元数据 Sharding 策略为 'year'
>
> * 增加元数据 CollectionSpace 创建参数：{LobPageSize:262144}，增加文件元数据 Collection 创建参数：{ReplSize:1}
>
> * 数据存储在 rootSite 数据存储服务的域 dataDomain，指定数据的 CollectionSpace 的 Sharding 策略为 'quarter'，Collection 的 Sharding 策略为 'month'
>
> * 增加数据 CollectionSpace 创建参数：{LobPageSize:262144}，增加数据 Collection 创建参数：{ReplSize:1}
>
> * 工作区中批次的分区策略为 'month'，时间信息的格式为 yyyy-MM-dd，批次内的文件名必须唯一
>
> * 批次ID获取时间信息的正则表达式为 (?<=\w{1,50}\\.[\^.]{1,50}\\.)(\d{4}-\d{2}-\d{2})(?=\\..{1,200})。其中， (?<=\w{1,50}\\.[\^.]{1,50}\\.) 表示以1-50个合法字符(大小写字母、数字、下划线)、点字符、1-50个非点字符、点字符开头；
(\d{4}-\d{2}-\d{2}) 表示中间是4个数字、连字符、2个数字、连字符、2个数字（对应上述的日期格式yyyy-MM-dd）；(?=\\..{1,200}) 表示以点字符、1-200个任意字符结尾。合法自定义ID如：BatchId_1523.aps_image.2020-09-03.test3
>
> * 工作区的目录功能不开启


[directory]:Architecture/Bussiness_Concept/directory.md
[batch]:Architecture/Bussiness_Concept/batch.md

