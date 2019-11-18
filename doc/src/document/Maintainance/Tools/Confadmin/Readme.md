confadmin 提供 confserver 配置管理功能

##参数##

|参数      |缩写        |描述          |
|----------|-----------|--------------|
|--help    |-h         |获取帮助文档  |
|--version |-v         |获取版本信息  |

>  **Note:**
>
>  * 获取 confadmin 的帮助文档:confadmin.sh -h
>
>  * 获取特定子命令的帮助文档:confadmin.sh -h subcommand


##子命令列表##

|子命令    |描述             |
|----------|----------------|
|[createnode][create_node]|创建配置服务节点  | 
|[subscribe][subscribe] |订阅配置事件通知  |
|[unsubscribe][unsubscribe] |取消订阅配置事件通知|
|[listsubscribers][listsubscribe]|获取订阅者列表|

[create_node]:Maintainance/Tools/Confadmin/createnode.md
[subscribe]:Maintainance/Tools/Confadmin/subscribe.md
[unsubscribe]:Maintainance/Tools/Confadmin/unsubscribe.md
[listsubscribe]:Maintainance/Tools/Confadmin/listsubscribers.md

