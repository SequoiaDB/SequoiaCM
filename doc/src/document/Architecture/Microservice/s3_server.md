S3 服务负责对外提供标准的 S3 协议接口，基本架构如下：

![S3服务架构][s3_server_arch]

- S3 服务节点需要依托于在某一个站点之上，当站点内包含 S3 服务节点时，该站点即具备 S3 协议的处理能力，与内容服务相同，S3 服务也具备跨站点读取其它站点数据的能力

- S3 客户端通过网关来访问各个站点上的 S3 服务，网关能够识别 S3 请求，并且根据用户配置路由 S3 请求到特定站点上的 S3 服务


> **Note:**

> * 网关将会解析 S3 请求所访问的 Bucket，根据 Bucket 所属工作区的 preferred 属性（该属性目前支持配置为某个站点名），路由到指定站点上的 S3 服务
> * 当网关无法根据 S3 请求的 Bucket 信息进行路由时（该 S3 请求不包含 Bucket 信息、Bucket 不存在等），网关将会根据集群部署情况进行路由，优先选择当前网关所属机房下的 S3 服务。


S3 服务节点逻辑上可以看作是一个特殊的内容服务节点，它与内容服务节点具有相同的内核：

![S3服务实现逻辑][s3_server_logic]

S3 服务内部实现本质上是将 S3 协议的操作请求，映射为 SequoiaCM 原生业务进行处理，例如通过 S3 协议创建一个对象，实际上 S3 服务是通过内容业务处理层创建了一个 SequoiaCM 文件

> **Note:**

> * 由于 S3 协议的处理是映射到 SequoiaCM 原生业务上，所以 SequoiaCM 原生接口与 S3 协议接口之间数据是互通，通过 SequoiaCM 原生桶接口写入的文件可以在 S3 协议接口进行访问，反之亦然


映射关系
----

概念映射表

|S3协议    |SequoiaCM 原生业务     |备注|
|----------|-----------------------|----|
|bucket    |bucket                 |两个协议下均具备 Bucket 的概念，且含义与功能基本一致|
|object    |file                   |文件内容映射为对象数据，元数据的具体映射关系见后续表格|
|region    |workspace              |仅当工作区禁用目录功能时，方可映射为 Region|
|AccessKey/SecretKey|AccessKey/SecretKey|在 SequoiaCM 中，AccessKey/SecretKey 是由某个用户产生出来的，使用该 Key 访问业务时，其权限与所属用户一致|

File 与 Object 的元数据映射

|S3 Object |SequoiaCM File         | 备注 |
|----------|-----------------------|------|
|key       |name                   ||
|user meta data|custom meta data   ||
|etag      | md5 |  S3 通过分段上传创建的对象没有这个映射关系|
|size      |size||
|content type | mime type||
|last modify time|update time||

> **Note:**

> * S3 协议规定，分段上传创建的 S3 Object，etag 不是对象数据的 md5，所以这种情况下其 etag 也不会映射到 SequoiaCM 文件的 md5 属性

> * 其它未参与映射的属性，只在各自协议下可见

Object 多版本与 File 多版本之间的映射

![S3多版本映射][s3_version_mapping]

- Object Version Id 不为 null 时，直接映射 File 的 major version 和 minor version
- Object Version Id 为 null 时，映射 File 中的 -2.0 版本，-2.0 版本在 SequoiaCM 中定义为 File 的 null 版本
- Object 的 Delete Marker 版本，映射 File 中 delete marker 为 true 的版本

> **Note:**

> *  所属桶的版本控制状态为 Disabled/Suspended 时，创建对象将会产生 version id 为 null 的版本
> *  所属桶的版本控制状态为 Enabled/Suspended 时，删除对象将会产生 Delete Marker 的版本，当对象的最新版本为 Delete Marker 时，部分接口将视该对象不存在（获取最新版本对象、列取对象）

S3 功能支持
----
如下列表描述了当前版本支持的标准 S3 功能

- List Buckets
- Delete Bucket
- Create Bucket
- Bucket Location
- Bucket Object Versions
- Get Bucket Info(HEAD)
- Put Object
- Delete Object
- Get Object
- Get Object Info(HEAD)
- Post Object
- Copy Object
- Multipart Uploads
- Presigned URLS

[s3_server_arch]:Architecture/Microservice/s3_server.png
[s3_server_logic]:Architecture/Microservice/s3_server_logic.png
[s3_version_mapping]:Architecture/Microservice/s3_version_mapping.png