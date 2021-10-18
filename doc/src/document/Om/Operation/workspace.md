## 查看工作区列表 ##
列表展示工作的名称、站点列表、创建人、创建时间、工作区描述等信息，每页展示10条，可在顶部搜索栏使用工作区名称模糊检索。
![工作区列表][workspace_list]

>  **Note：**
>
>  * 对于普通用户，列表中只展示自己拥有权限（READ或以上权限）的工作区，对于admin用户（拥有ROLE_AUTH_ADMIN角色），列表中展示系统所有工作区。om-server会缓存用户权限信息，更新用户权限后，需要等待权限刷新后（默认120s，可通过配置文件配置）才能看到效果
>
>  * 查看工作区详情需要拥有对应工作区的READ权限，为了方便admin用户的管理，建议赋予admin用户管理所有工作区的权限


## 查看工作区详情 ##
在工作区列表界面，点击指定工作区的 查看详情 按钮，查看该工作区的基本信息、站点信息、元数据信息和统计信息。
![工作区详情][workspace_detail]



[workspace_list]:Om/Operation/workspace_list.png
[workspace_detail]:Om/Operation/workspace_detail.png