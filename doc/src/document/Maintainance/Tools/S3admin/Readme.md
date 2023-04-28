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

| 子命令                                        | 描述              |
|--------------------------------------------|-----------------|
| [createnode][createnode]                   | 创建s3服务节点        |
| [set-default-region][set-default-region]   | 设置 S3 默认区域      |
| [show-default-region][show-default-region] | 查看 S3 默认区域      |
| [refresh-accesskey][refresh-accesskey]     | 重置用户的 Accesskey |
| [enable-quota][enable-quota]               | 开启限额功能         |
| [update-quota][update-quota]               | 更新限额信息         |
| [disable-quota][disable-quota]             | 关闭限额功能         |
| [quota-status][quota-status]               | 查看限额信息         |
| [sync-quota][sync-quota]                   | 触发额度同步         |
| [cancel-sync-quota][cancel-sync-quota]     | 中止额度同步         |



[createnode]:Maintainance/Tools/S3admin/createnode.md
[set-default-region]:Maintainance/Tools/S3admin/set-default-region.md
[show-default-region]:Maintainance/Tools/S3admin/show-default-region.md
[refresh-accesskey]:Maintainance/Tools/S3admin/refresh-accesskey.md
[enable-quota]:Maintainance/Tools/S3admin/enable-quota.md
[update-quota]:Maintainance/Tools/S3admin/update-quota.md
[disable-quota]:Maintainance/Tools/S3admin/disable-quota.md
[quota-status]:Maintainance/Tools/S3admin/quota-status.md
[sync-quota]:Maintainance/Tools/S3admin/sync-quota.md
[cancel-sync-quota]:Maintainance/Tools/S3admin/cancel-sync-quota.md
