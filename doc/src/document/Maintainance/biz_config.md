本章节主要汇总介绍 SequoiaCM 中常用的业务配置相关的操作。

## 工作区配置 ##

1. 工作区的创建、删除目前可以通过内容服务提供的工作区[创建][workspace_create_tool]、[删除][workspace_delete_tool]工具，以及[驱动接口][workspace_driver]完成，具体使用见该章节介绍。

2. 工作区增加站点、修改某个站点上的存储配置（分区规则、数据源配置等）、以及工作区的优先访问站点（工作区 preferred 属性）可以通过内容服务提供的[工作区更新工具][workspace_update_tool]、[驱动接口][workspace_driver]完成，具体使用见该章节介绍

    > **Note**
    >
    > - 工作区 preferred 属性目前作用在 S3 协议接口上，当外部业务通过 S3 协议访问工作区资源时，网关将会根据该属性选择优先的站点进行处理

3. 工作区的缓存策略目前可以通过 OM 工作区管理页面、驱动接口进行设置

    >**Note**
    >
    > - 工作区的缓存策略用于控制文件跨中心读取时，是否缓存在途径站点上

## 站点配置 ##

 创建、删除站点目前支持通过内容服务提供的[创建][site_create_tool]、[删除][site_delete_tool]工具完成，具体使用见该章节介绍。

  > **Note**
  >
  > - 目前站点业务配置暂不提供更新能力


## S3 协议配置 ##

1. S3 协议的默认 Region 可以通过 S3 服务提供的[ Region 设置工具][region_tool]进行调整，具体见该章节的介绍

2. S3 协议的 AccessKey、SecretKey 可以通过 S3 服务提供的[ Accesskey 工具][accesskey_tool]进行生成，具体见该章节介绍

[region_tool]:Maintainance/Tools/S3admin/set-default-region.md
[accesskey_tool]:Maintainance/Tools/S3admin/refresh-accesskey.md
[site_create_tool]:Maintainance/Tools/Scmadmin/createsite.md
[site_delete_tool]:Maintainance/Tools/Scmadmin/deletesite.md
[workspace_update_tool]:Maintainance/Tools/Scmadmin/alterws.md
[workspace_create_tool]:Maintainance/Tools/Scmadmin/createws.md
[workspace_delete_tool]:Maintainance/Tools/Scmadmin/deletews.md
[workspace_driver]:Development/Java_Driver/workspace_operation.md