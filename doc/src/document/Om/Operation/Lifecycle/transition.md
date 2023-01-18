
本文档主要介绍数据流的查看、创建、修改和删除操作。

##查看数据流列表##

点击左侧导航栏【生命管理周期】->【数据流管理】，将展示已创建的数据流信息。用户可通过顶部搜索栏，按数据流名称或阶段标签检索指定的数据流信息。

![数据流列表][transition_list]

##查看数据流详情##

用户在操作栏点击 **查看** 按钮后，即可查看对应数据流的详细信息。

![查看数据流详情][transition_detail]

##创建数据流##

用户在数据流管理界面点击 **创建数据流** 按钮后，将弹出创建数据流对话框。在该对话框中填写数据流名称、起始阶段、目标阶段和流转配置后，点击底部 **保存** 按钮即可创建数据流。

![创建数据流1][transition_create_1]

点击 **扩展配置** 一栏可以配置迁移和清理的文件范围、文件查询条件、是否快速启动、数据校验级别和是否回收空间。点击 **查看结果预览** 可验证输入的查询条件是否正确。

![创建数据流2][transition_create_2]

##编辑数据流##

用户在操作栏点击 **编辑** 按钮后，将弹出编辑数据流对话框。在该对话框中修改配置后，点击底部 **保存** 按钮即可更新对应数据流的配置。

![编辑数据流][transition_update]

##修改生效范围##

用户在生效范围栏中点击编辑图标后，将弹出修改生效范围对话框。在该对话框中进行工作区的添加与移除后，点击底部 **关闭** 按钮即可更新生效范围。

![修改生效范围][transition_apply]

[transition_list]:Om/Operation/LifeCycle/transition_list.png
[transition_apply]:Om/Operation/LifeCycle/transition_apply.png
[transition_create_1]:Om/Operation/LifeCycle/transition_create_1.png
[transition_create_2]:Om/Operation/LifeCycle/transition_create_2.png
[transition_detail]:Om/Operation/LifeCycle/transition_detail.png
[workspace]:Om/Operation/workspace.md
[transition_update]:Om/Operation/LifeCycle/transition_update.png