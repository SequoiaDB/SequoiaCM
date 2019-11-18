scmadmin 工具提供了管理 SequoiaCM 业务配置的功能
>  **Note:**
>
>  * 当使用 scmadmin 工具对 SequoiaCM 集群站点做了创建、修改等操作之后，需要使用 scmctl 工具刷新业务配置使修改生效

##参数##

|参数      |缩写       |描述          |
|----------|-----------|--------------|
|--help    |-h         |获取帮助文档  |
|--version |-v         |获取版本信息  |

>  **Note:**
>
>  * 获取 scmadmin 的帮助文档:scmadmin.sh -h
>
>  * 获取特定子命令的帮助文档:scmadmin.sh -h subcommand

##子命令列表##

|子命令    |描述             |
|----------|-----------------|
|[encrypt][encrypt] |加密明文密码     |
|[createsite][createsite]|创建站点         |
|[listsite][listsite] |查看站点         |
|[createnode][createnode]|创建节点         |
|[createws][createws] |创建工作区       |
|[deletews][deletews]|删除工作区       |
|[alterws][alterws]|修改工作区       |
|[listws][listws]   |查看工作区       |
|[createuser][createuser]|创建SequoiaCM用户|
|[deleteuser][deleteuser]|删除SequoiaCM用户|
|[listuser][listuser]|查看SequoiaCM用户|
|[attachrole][attachrole]|为SequoiaCM用户添加角色|
|[createrole][createrole]|创建角色     |W
|[deleterole][deleterole]|删除角色     |
|[listrole][listrole]|查看角色     |
|[grantrole][grantrole]|赋予角色权限     |
|[revokerole][revokerole]|回收角色权限     |
|[listprivilege][listprivilege]|查看角色权限  |
|[resetpassword][resetpassword]|修改用户密码  |


[encrypt]:Maintainance/Tools/Scmadmin/encrypt.md
[createsite]:Maintainance/Tools/Scmadmin/createsite.md
[listsite]:Maintainance/Tools/Scmadmin/listsite.md
[createnode]:Maintainance/Tools/Scmadmin/createnode.md
[createws]:Maintainance/Tools/Scmadmin/createws.md
[deletews]:Maintainance/Tools/Scmadmin/deletews.md
[alterws]:Maintainance/Tools/Scmadmin/alterws.md
[listws]:Maintainance/Tools/Scmadmin/listws.md
[createuser]:Maintainance/Tools/Scmadmin/createuser.md
[deleteuser]:Maintainance/Tools/Scmadmin/deleteuser.md
[listuser]:Maintainance/Tools/Scmadmin/listuser.md
[attachrole]:Maintainance/Tools/Scmadmin/attachrole.md
[createrole]:Maintainance/Tools/Scmadmin/createrole.md
[deleterole]:Maintainance/Tools/Scmadmin/deleterole.md
[listrole]:Maintainance/Tools/Scmadmin/listrole.md
[grantrole]:Maintainance/Tools/Scmadmin/grantrole.md
[revokerole]:Maintainance/Tools/Scmadmin/revokerole.md
[listprivilege]:Maintainance/Tools/Scmadmin/listprivilege.md
[resetpassword]:Maintainance/Tools/Scmadmin/resetpassword.md