
本文档主要介绍桶的查看、创建、修改和删除操作。

##查看桶列表##

点击左侧导航栏【桶管理】->【桶列表】，将展示桶的名称、所属 region、版本控制状态、创建人和创建时间。用户可在顶部搜索栏按桶名称过滤信息。

![桶列表][bucket_list]

>**Note：**
>
> 列表仅展示用户拥有操作权限的桶。如果用户关联了角色 ROLE_AUTH_ADMIN，将展示系统中所有的桶。

##查看桶详情##

用户在操作栏点击 **查看** 按钮后，对应桶的详细信息将以对话框形式展示。

![桶详情][bucket_detail]

##创建桶##

用户在桶列表界面点击 **创建桶** 按钮后，将弹出创建桶对话框。在该对话框中填写桶名、所属 region 和版本控制状态后，点击底部 **保存** 按钮即可创建桶。

![创建桶][create_bucket]

>**Note:**
>
> 如果需要同时创建多个桶，可在对话框中点击 **继续添加** 按钮，并输入对应的桶名。

##修改版本控制状态##

用户在版本控制状态栏点击需要修改的状态后，将弹出二次确认对话框。点击对话框中的 **确认** 按钮即可完成修改。

![更改版本控制状态][update_bucket_version_control]

>**Note:**
>
> 版本状态的变更规则可参考[桶版本控制][bucket_version]。

##删除桶##

用户可删除单个桶，也可批量删除桶。执行删除操作前，用户需确保拥有桶的删除权限。

###单个删除###

用户在操作栏点击 **删除** 按钮后，在弹出的提示框中点击 **确认** 按钮即可删除该桶。

![单个删除][bucket_delete_single]

###批量删除###

用户在左侧勾选框勾选多个桶后，点击 **批量删除** 按钮，并在弹出的提示框中点击 **确认** 按钮即可批量删除桶。

![批量删除][bucket_delete_batch]

[bucket_list]:Om/Operation/bucket_list.png
[bucket_detail]:Om/Operation/bucket_detail.png
[create_bucket]:Om/Operation/create_bucket.png
[update_bucket_version_control]:Om/Operation/update_bucket_version_control.png
[bucket_version]:Architecture/Business_Concept/bucket.md#版本控制
[bucket_delete_single]:Om/Operation/bucket_delete_single.png
[bucket_delete_batch]:Om/Operation/bucket_delete_batch.png