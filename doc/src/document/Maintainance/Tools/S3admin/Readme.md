s3admin 提供 s3 server 配置管理功能

## 参数 ##

|参数      |缩写        |描述          |
|----------|-----------|--------------|
|--help    |-h         |获取帮助文档  |
|--version |-v         |获取版本信息  |

>  **Note:**
>
>  * 获取 s3admin 的帮助文档:s3admin.sh -h
>
>  * 获取特定子命令的帮助文档:s3admin.sh -h subcommand

##子命令列表##

|子命令    |描述             |
|----------|----------------|
|[createnode][createnode]|创建s3服务节点  |
|[set-default-region][set-default-region]|设置 S3 默认区域|
|[show-default-region][show-default-region]|查看 S3 默认区域|
|[refresh-accesskey][refresh-accesskey]|重置用户的 Accesskey|


[createnode]:Maintainance/Tools/S3admin/createnode.md
[set-default-region]:Maintainance/Tools/S3admin/set-default-region.md
[show-default-region]:Maintainance/Tools/S3admin/show-default-region.md
[refresh-accesskey]:Maintainance/Tools/S3admin/refresh-accesskey.md
