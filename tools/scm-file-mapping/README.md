# 批量映射工具

使用该工具可以快速将 SCM 桶外文件映射至桶内，从而获得 SCM bucket API 的访问能力，并且兼容 S3 API 访问映射后的文件。目前工具具备以下能力：

- 支持按工作区+文件匹配条件，将批量文件与桶建立映射关系。

- 支持按工作区+文件 ID 列表，将批量文件与桶建立映射关系。

主要使用场景：

1. 原有业务系统通过 SCM API（工作区 + 文件 ID）访问文件，用户希望将业务系统改造成通过 S3 API 访问。

## 1 映射工具介绍

工具目录结构如下：

```shell
/sequoiacm-file-mapping
	|--bin/           			  # 映射工具执行入口
	|--jars/           			  # 工具执行所需 jar 包
	|--start.log      			  # 工具启动日志输出路径
	|--README.md                  # 使用说明手册
```

## 2 执行映射

### 映射前准备

1. 关闭工作区目录功能。

2. 在工作区下提前创建好桶。

>  **Note:**
>
> * 关闭目录后，工作区的目录功能无法使用，且 SCM 文件只能通过文件 ID 进行读取，无法通过路径访问。
> 
> * 若业务依赖目录功能，则需要改造业务端，取消依赖，否则不能使用 S3 与 SCM 桶的功能。

### 按文件匹配条件建立映射

- 将工作区 ws1 下的所有文件，重命名为文件 ID，然后关联至 bucket1

```shell
./bin/fileMappingUtil.sh mapping \
	--work-path /opt/sequoiacm/file_mapping/ \
	--workspace ws1 \
	--file-matcher all \
	--bucket bucket1 \
	--key-type FILE_ID \
	--url localhost:8080/rootSite,localhost:8080/branchSite \
	--user admin --password
```

>  **Note:**
>
> * 每隔 5s 打印映射进度。
>
> 	success：成功总数，error：失败总数，process：本次执行处理的文件数
>
> * 若映射中断，重新执行映射可以跳过前面已完成映射的批次，中断原因可能如下：
>	
>		1. 失败数过多：通过日志排查失败原因后重新执行映射。
> 		
> 		2. 进程挂掉或机器宕机等原因：重新执行映射即可。
>
> * 旧文件名可以通过 ScmFile.getNameBeforeAttach() 接口获取。

- 将工作区 ws1 下，admin 用户创建的文件，重命名为文件 ID，然后关联至 bucket1

```shell
./bin/fileMappingUtil.sh mapping \
	--work-path /opt/sequoiacm/file_mapping/ \
	--workspace ws1 \
	--file-matcher '{"user":"admin"}' \
	--bucket bucket1 \
	--key-type FILE_ID \
	--url localhost:8080/rootSite,localhost:8080/branchSite \
	--user admin --password
```

### 按文件 ID 列表建立映射

映射执行结束后，若失败数 error 不为 0 且工作目录下文件 error_file_id.list 不为空，可指定这份文件重新映射失败的文件

-  新将工作区 ws1 下，error_file_id.list 中 ID 列表所对应的文件，重命名为文件 ID，然后关联至 bucket1

```shell
./bin/fileMappingUtil.sh mapping \
	--work-path /opt/sequoiacm/file_mapping_error/ \
	--workspace ws1 \
	--file-id /opt/sequoiacm/file_mapping/error/error_file_id.list \
	--bucket bucket1 \
	--key-type FILE_ID \
	--url localhost:8080/rootSite,localhost:8080/branchSite \
	--user admin --password
```


**mapping 命令参数**

|参数             |是否必填|描述         |
|-----------------|--------|-------------|
|--work-path      |是      |工作目录。存放工具执行中间产物（日志、进度监控文件、失败列表）|
|--workspace      |是      |工作区名|
|--bucket         |是      |bucket 名|
|--file-matcher   |与 file-id 选填其中一项    |文件匹配条件，支持以下形式：<br>all：表示工作区下的所有文件<br>'json字符串'：json 格式的文件匹配条件|
|--file-id        |与 file-matcher 选填其中一项      |id 文件路径，文件中每个 ID 占一行|
|--url            |是      |网关地址列表|
|--user           |是      |scm 用户名|
|--password       |与 password-file 选填其中一项      |scm 用户密码，不填值表示采用交互式输入|
|--password-file  |与 password 选填其中一项        |scm 密码文件|
|--key-type       |是      |文件关联类型，可选值：<br>FILE_ID：将文件重命名为文件 ID，再与桶建立映射关系。（适用场景：用户在旧系统产生的 SCM 文件，在业务端保存的是文件 ID，映射前业务系统通过文件 ID 访问文件。当业务系统希望改造成通过 bucket + 文件 ID 访问文件时，可以使用此类型完成映射）<br>FILE_NAME：直接将文件与桶建立映射关系。（适用场景：用户在旧系统产生的 SCM 文件，在业务端保存了文件 ID + 文件名。业务系统希望改造成通过 bucket + 文件名 访问文件）|
|--batch-size     |否      |批次大小，默认 5000|
|--attach-size    |否      |单次映射的 ID 列表长度，默认 50|
|--thread         |否      |并发数，默认 20|
|--max-fails      |否      |映射出错多少文件时，执行退出，默认 100|

## 3 工作目录

工作目录下存放本次执行的中间产物，以及标记映射状态，其结构如下：

```shell
/opt/sequoiacm/file_mapping/
	|--conf/            
		|--work.conf                 # 当前工作路径的环境描述信息
		|--logback.xml               # 日志配置文件
	|--error/       
		|--error_file_id.list        # 映射失败的文件 ID 列表
		|--unattachable_file_id.list # 无法映射的文件 ID 列表（e.g. 文件已经映射至其它的桶）
	|--log/fileMapping.log           # 工具执行日志
	|--mapping_progress.json         # 映射进度监控文件
```

>  **Note:**
>
> * 使用同一工作目录需要保证映射条件不变（工作区、桶、查询条件/ID文件路径）。
> 
> * unattachable_file_id.list 中存放无法映射的文件，例如：文件不存在、文件已经与其它的桶建立了映射关系，这批文件即使再重新执行映射也无法成功。
> 
> * error_file_id.list 中存放的是一些因网络、IO 等其它无法预知的错误导致映射失败的文件 ID 列表。