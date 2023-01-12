
本文主要介绍工作区的查看、创建和删除操作。

##查看工作区列表##

点击左侧导航栏【工作区管理】，将展示工作区的名称、站点列表、创建人、创建时间和描述信息。用户可在顶部搜索栏按工作区名称过滤信息。

![工作区列表][workspace_list]

>**Note：**
>
> 列表仅展示用户拥有操作权限的工作区。如果用户关联了角色 ROLE_AUTH_ADMIN，将展示系统中所有的工作区。

##查看工作区详情##

用户在操作栏点击 **查看详情** 按钮后，可查看对应工作区的基础信息和流量统计。

###基础信息###

工作区的基础信息包括工作区名称、描述、创建人、更新人、配置、站点信息和元数据信息。SequoiaCM 管理员用户可在此页面调整工作区的缓存策略。

![工作区详情][workspace_detail]

###流量统计###

工作区的流量统计包括文件和对象的上传下载请求数、文件数增量和文件大小增量。其中文件数增量为当天文件的净增量，文件大小增量为当天净增文件的总大小。

![流量信息1][workspace_detail_traffic_1]
![流量信息2][workspace_detail_traffic_2]
![流量信息3][workspace_detail_traffic_3]

>**Note:**
>
> 流量统计功能依赖于[监控服务][admin_server]，用户需确保集群已部署监控服务。

##创建工作区##

用户在工作区列表点击 **创建工作区** 按钮后，将弹出创建工作区对话框。在该对话中填写工作区的基本配置、元数据存储站点配置、内容存储站点配置和其他配置后，点击底部 **保存** 按钮即可创建工作区。

![创建工作区][create_workspace]

>**Note:**
>
> - 如果需要同时创建多个工作区，可在对话框中点击 **继续添加** 按钮，并输入对应的工作区名称。
> - 内容存储站点必须包含主站点。如果需要同时配置多个内容存储站点，可在下拉框中指定站点后，点击 **添加站点** 按钮。

##删除工作区##

用户可删除单个工作区，也可批量删除工作区。执行删除操作前，用户需确保拥有工作区的删除权限。

###单个删除###

用户在操作栏点击 **删除** 按钮后，在弹出的提示框中点击 **确认** 按钮即可删除该工作区。

![单个删除][workspace_delete_single]

###批量删除###

用户在左侧勾选框勾选多个桶后，点击 **批量删除** 按钮，并在弹出的提示框中点击 **确认** 按钮即可批量删除桶。

![批量删除][workspace_delete_batch]

[workspace_list]:Om/Operation/workspace_list.png
[workspace_detail]:Om/Operation/workspace_detail.png
[workspace_detail_traffic_1]:Om/Operation/workspace_detail_traffic_1.png
[workspace_detail_traffic_2]:Om/Operation/workspace_detail_traffic_2.png
[workspace_detail_traffic_3]:Om/Operation/workspace_detail_traffic_3.png
[create_workspace]:Om/Operation/create_workspace.png
[workspace_delete_single]:Om/Operation/workspace_delete_single.png
[workspace_delete_batch]:Om/Operation/workspace_delete_batch.png
[admin_server]:Architecture/Microservice/admin_service.md