本文档主要介绍 SequoiaCM 系统角色的查看、新增、修改和删除操作。

##查看角色列表##

点击左侧导航栏【系统管理】->【角色管理】，将展示已创建的角色信息。用户可通过顶部搜索栏，按角色名称检索指定的角色信息。

![角色列表][role_list]

##查看角色详情##

用户在操作栏点击 **查看** 按钮后，即可查看对应角色的详细信息，包括角色 ID、角色名、角色描述以及角色拥有的权限列表。

![查看角色详情][role_detail]

##新增角色##

用户在角色管理界面点击 **新增** 按钮后，将弹出新增角色对话框。在该对话框中填写角色名和角色描述后，点击底部 **保存** 按钮即可新增角色。

![创建角色][role_create]

##管理角色权限##

用户在操作栏点击 **角色赋权** 按钮后，将弹出角色赋权对话框。在该对话框中可完成对应角色权限信息的添加或删除。

###添加角色权限###

在该对话框中选择资源类型、资源和权限类型后，点击 **添加** 按钮后即可为该角色添加权限。

![角色添加权限][role_privilege_add]

###删除角色权限###

在该对话框中点击权限列表下的 **x** 图标即可删除该角色的权限。

![角色删除权限][role_privilege_remove]

##删除角色##

用户在操作栏点击 **删除** 按钮后，将弹出删除确认对话框。在该对话框中填写角色名后，点击底部 **确定** 按钮即可删除指定角色。

![删除角色][role_delete]

[role_list]:Om/Operation/System/role_list.png
[role_detail]:Om/Operation/System/role_detail.png
[role_create]:Om/Operation/System/role_create.png
[role_privilege_add]:Om/Operation/System/role_privilege_add.png
[role_privilege_remove]:Om/Operation/System/role_privilege_remove.png
[role_delete]:Om/Operation/System/role_delete.png