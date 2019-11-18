listsubscribers 子命令查看订阅者列表的功能。

##子命令选项##

|选项       |缩写 |描述                                                                              |是否必填|
|-----------|-----|--------------------------------------------------------------------------------|--------|
|--url      |-u   |配置服务节点的url，如：config-hostname:8190                                         |是| 

###示例###

查看订阅者列表

   ```lang-javascript
   $  confadmin.sh  listsubscribers --url localhost:8190
   ```  