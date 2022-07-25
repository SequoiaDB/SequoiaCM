Bucket （桶）是存在于工作区中的一个逻辑概念，用于表示文件的集合。

![桶][bucket]

文件只能隶属于某一个桶，或者不属于任何桶，当文件处于桶下时，将会额外获得如下能力：

- 支持通过桶名+文件名的方式检索文件（桶名全局唯一而非工作区下唯一，桶下文件名具有唯一约束）
- 桶下文件的多版本行为将会受到桶的版本控制状态影响
- 桶下文件可以通过 S3 协议以对象的形式进行访问

版本控制
----
用户可以为桶设定不同的版本控制状态，以调整桶下文件的多版本行为

![版本控制状态][version_control]

Disabled：桶的默认版本控制状态，桶下所有文件只有一个 null 版本

>  **Note:**
>
>  * null 版本实际是版本号为 -2.0 的文件版本，该版本文件通过 S3 访问其映射对象时， version id 为 null
>  * null 版本文件会额外携带一个 version serial 信息，描述该版本在文件所有版本列表中所处的位置

Enabled: 通过启用版本控制，可以将桶的版本控制状态调整为 Enabled 状态，该状态下文件可以包含多个版本

Suspended: 通过暂停版本控制，可以将桶的版本控制状态调整为 Suspended 状态

桶下文件多版本行为
---
当桶处于不同的版本控制状态下时，如下文件操作将会有不同行为表现

- 桶内重复上传同名文件
- 带版本控制的文件删除

> 区别于物理删除（删除文件的所有数据和元数据），该操作是指让 SequoiaCM 系统根据文件所处桶的版本控制状态，执行相对应的文件删除动作）

###桶内上传同名文件###

####Disabled####


![禁用版本控制][version_disabled]

关闭版本控制时重复上传同名文件，SCM 将会创建新版本，同时删除老版本的所有数据(不可恢复)

>  **Note:**
>
>  * Disabled 状态创建的文件版本，总是 null 版本即版本号为 -2.0

####Enabled####

![启用版本控制重复创建][version_enabled_create_file]

启用版本控制时重复上传同名文件，将会产生新的版本，历史版本不受影响

####Suspended####
![暂停版本控制重复创建][version_suspended_create_file]

暂停版本控制时重复上传同名文件，将会产生新的版本，新版本为 null 版本，若历史版本已经存在旧的 null 版本，该版本将会被删除


###带版本控制删除文件####


####Disabled####

该状态下执行带版本控制删除文件，与物理删除表现一致，将会删除该文件的所有数据，不可恢复

####Enabled####

![启用版本控制删除][version_enabled_delete_file]

该状态下执行带版本控制删除文件，将会产生一个 Delete Marker 版本，历史版本依然保留并且可被访问（指定版本进行获取文件）

>  **Note:**
>
>  * Delete Marker 相当于一个占位符（该版本不具备文件内容和用户元数据），表示该文件被执行过带版本控制的文件删除，当文件的最新版本是 Delete Marker，那么该文件在 SequoiaCM 中将如同已经被删除了一样，只有指定版本获取文件、列取文件历史版本接口可以获得该文件


####Suspended####

![暂停版本控制删除][version_suspended_delete_file]

 该状态下执行带版本控制删除文件，将会产生一个 Delete Marker 的 null 版本，若历史版本已经存在旧的 null 版本，该版本将会被删除，其它历史版本依然保留并且可被访问（指定版本号进行获取文件）


实现原理
---
![桶原理][bucket_logic]

- 在工作区下，每创建一个 Bucket，都会产生一个 Bucket 关系集合，该集合保存了 Bucket 下的所有文件信息，但该信息并不完整，只包含了少量的文件属性：文件名、文件ID、创建时间、所属用户、最新版本号等关键信息。
- 当通过 Bucket + 文件名 检索文件时，首先在 Bucket 关系集合中按文件名找到文件 ID，再通过文件 ID 在 File 集合获得文件元数据

[version_control]:Architecture/Business_Concept/bucket_version_control.png
[version_disabled]:Architecture/Business_Concept/bucket_version_disabled.png
[version_enabled_delete_file]:Architecture/Business_Concept/bucket_version_enabled_delete_file.png
[version_enabled_create_file]:Architecture/Business_Concept/bucket_version_enabled_create_file.png
[version_suspended_delete_file]:Architecture/Business_Concept/bucket_version_suspended_delete_file.png
[version_suspended_create_file]:Architecture/Business_Concept/bucket_version_suspended_create_file.png
[bucket]:Architecture/Business_Concept/bucket.png
[bucket_logic]:Architecture/Business_Concept/bucket_logic.png





