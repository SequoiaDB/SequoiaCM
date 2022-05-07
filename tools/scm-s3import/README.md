# S3 迁移工具

使用此工具实现 SequoiaS3、SCM-S3 系统之间的数据互迁。

## 1 环境准备

- 在目标 S3 系统中提前创建好桶。

- 同步存储桶的版本控制开关。若源 S3 系统中桶的版本控制状态为开启/暂停，则目标 S3 系统中相应桶需要打开版本控制开关。（迁移完成后再选择是否暂停版本控制）

## 2 迁移工具介绍

工具目录

```shell
/sequoiacm-s3import
	|--bin/           			  # 迁移工具执行入口
	|--conf/          
		|--s3import.properties	  # 工程配置文件
	|--jar/           			  # 工具执行所需 jar 包
	|--start.log      			  # 工具启动日志输出路径
```

子命令列表

|子命令  |描述                                                                |
|--------|--------------------------------------------------------------------|
|migrate |数据迁移。迁移源 S3 桶内数据至目标 S3                               |
|retry   |重试迁移。重试迁移桶内失败的对象                                    |
|compare |数据比对。比对源 S3 与目标 S3 桶内数据变更及以及对象内容、元数据校验| 
|sync    |数据同步。指定数据比对结果，同步修复源 S3 中的数据变更至目标 S3     |

命令参数

|参数             |所属子命令                 |是否必填|描述         |
|-----------------|---------------------------|--------|-------------|
|--work-path      |migrate/retry/compare/sync |是      |工作目录。同一工作目录可以多次使用，但需要保证源 S3、目标 S3、桶列表与上次执行时一致。|
|--conf           |migrate/retry/compare/sync |否      |指定工程配置文件的路径|
|--max-exec-time  |migrate/retry/compare/sync |否      |最大执行时间，未指定时不限制，单位：秒|
|--bucket         |migrate/retry/compare      |是      |需要处理的桶列表，多个桶之间用逗号分隔|
|--cmp-result-path|sync                       |是      |数据比对结果的路径|

## 3 执行迁移

### 修改工程配置文件 s3import.properties

1. 源 S3、目标 S3 地址、鉴权密钥。
 
2. 批次大小 batch_size。数据迁移时桶内 N 个对象构成一个批次，批次内的多个对象并行迁移。

3. 最大并发数 work_count。建议与 CPU 核数一致，以提升迁移速率。

其余配置项说明见下文附录章节。


### 执行迁移

迁移源 S3 中桶 sfz、xyk、sbk 内数据至目标 S3

```shell
./bin/s3import.sh migrate --work-path /opt/s3import --bucket sfz,xyk,sbk
```

>  **Note:**
> 
> * 迁移至不同的桶，使用冒号分隔，例如：--bucket sfz:scm-sfz,xyk,sbk（将源 S3 中桶 sfz 的数据迁移至目标 S3 桶 scm-sfz 上） 
> 
> * 若迁移中断，重新执行迁移即可（跳过已完成的桶、批次）


### 重试迁移

若数据迁移存在部分失败，迁移中断/结束后可重试迁移这批失败的对象

```shell
./bin/s3import.sh retry --work-path /opt/s3import --bucket sfz,xyk,sbk
```

### 数据比对

校验源 S3 与目标 S3 桶内数据变更，差异结果以桶为单位输出至 工作目录/compare_result/ 下

```shell
./bin/s3import.sh compare --work-path /opt/s3import --bucket sfz,xyk,sbk
```

>  **Note:**
> 
>  * 默认校验对象 eTag，可在工程配置文件中配置为严格比对模式 strict_comparison_mode 校验对象 md5。
>
>  * 若比对中断，重新执行比对即可（跳过以比对完成的桶、批次）

### 数据同步

当桶内存在数据差异时，可指定数据比对结果目录同步修复数据差异

```shell
./bin/s3import.sh sync --work-path /opt/s3import --cmp-result-path /opt/s3import/compare_result
```

>  **Note:**
> 
>  * 若同步中断，重新执行同步即可（跳过已完成的桶）
>
>  * 若同步存在部分失败，会提示其存放路径，例如： /opt/s3import/compare_result/error/，可指定此目录再次同步失败部分

## 4 工作目录

```shell
/opt/s3import/
	|--compare_result/         # 存放数据比对结果
	|--conf/            
		|--work_env.json       # 当前工作路径的环境描述信息
		|--logback.xml         # 日志配置文件
	|--error/                  # 存放迁移失败的对象列表
	|--log/s3import.log        # 工具执行日志
	|--compare_progress.json   # 比对进度监控文件
	|--migrate_progress.json   # 迁移进度监控文件
```

## 附录

工程配置 s3import.properties

|配置项                        |类型    |说明              |
|------------------------------|--------|------------------|
|src.s3.url                    |str     |源 S3 系统地址|
|src.s3.accessKey              |str     |源 S3 系统访问密钥|
|src.s3.secretKey              |str     |源 S3 系统访问密钥|
|src.s3.key-file               |str     |源 S3 系统访问密钥文件路径（优先使用文件中的密钥连接 S3，若此项为空，则使用明文密钥对）|
|src.s3.client.maxErrorRetry   |num     |工具执行时请求源 S3 的失败重试次数，默认值：3|
|src.s3.client.signerOverride  |str     |S3 驱动签名配置，默认值：S3SignerType（v2版本签名），填空串表示由客户端自动选择签名算法|
|src.s3.client.connTimeout     |num     |工具执行时与源 S3 建立连接的超时时长，默认值为：10000，单位：毫秒|
|src.s3.client.socketTimeout   |num     |工具执行时与源 S3 的 socket 连接超时时长，默认值为：50000，单位：毫秒|
|src.s3.client.connTTL         |num     |S3 驱动连接池内连接的过期时间，默认值：-1（不过期），单位：毫秒|
|dest.s3.url                   |str     |目标 S3 地址|
|dest.s3.accessKey             |str     |目标 S3 系统访问密钥|
|dest.s3.secretKey             |str     |目标 S3 系统访问密钥|
|dest.s3.key-file              |str     |目标 S3 系统访问密钥文件路径（优先使用文件中的密钥连接 S3，若此项为空，则使用明文密钥对）|
|dest.s3.client.maxErrorRetry  |num     |工具执行时请求目标 S3 的失败重试次数，默认值：3|
|dest.s3.client.signerOverride |str     |S3 驱动签名配置，默认值：S3SignerType（v2版本签名），填空串表示由客户端自动选择签名算法|
|dest.s3.client.connTimeout    |num     |工具执行时与目标 S3 建立连接的超时时长，默认值为：10000，单位：毫秒|
|dest.s3.client.socketTimeout  |num     |工具执行时与目标 S3 的 socket 连接超时时长，默认值为：50000，单位：毫秒|
|dest.s3.client.connTTL        |num     |S3 驱动连接池内连接的过期时间，默认值：-1（不过期），单位：毫秒|
|batch_size                    |num     |批次大小，默认值：500
|max_fail_count                |num     |最大失败数，超过此阈值进程退出，默认值：100|
|work_count                    |num     |最大并发数，默认值：50
|strict_comparison_mode        |boolean |严格比对模式。默认值：false<br>- true：比对数据时分别下载两端对象内容，计算并比对 md5<br>- flase：比对数据时校验两端对象的 eTag