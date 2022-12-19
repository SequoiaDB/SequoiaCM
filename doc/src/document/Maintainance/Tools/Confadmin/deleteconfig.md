deleteconfig 子命令提供动态删除服务或节点配置的功能。

##子命令选项##

|选项        |缩写 |描述                                                  |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--service  |     |服务名称，服务名称和节点名称只能填写其中一个，多个服务名称使用逗号分隔，如auth-server,schedule-server |否|
|--node     |     |节点名称，节点名称和服务名称只能填写其中一个，多个节点名称使用逗号分隔，如host1:8810,host2:8900       |否|
|--config   |     |配置项，只支持传递一个key，如scm.audit.userType.LOCAL                                          |是|
|--accept-unknown-conf||指定该参数表示允许服务删除不识别的配置项|否|
|--url      |     |网关地址，多个网关地址使用逗号分隔，如host1:8080,host2:8080                                      |是|
|--user     |     |管理员用户名                                                                                 |是|
|--password |     |管理员密码，指定值则使用明文输入，不指定值则命令行提示输入                                          |否|
|--password-file| |管理员密码文件，与 password 互斥                                                               |否|

>  **Note:**
> 
>  * 动态修改配置的接口只允许拥有管理员角色的用户访问
> 
>  * 动态修改配置操作会在节点的配置文件目录下产生修改前的配置文件备份

###示例###

动态删除配置

   ```lang-javascript
   $  confadmin.sh deleteconfig --service auth-server,schedule-server --config scm.audit.userType.LOCAL --url 192.168.31.98:8080 --user admin --password admin
   ```
   
   如果指定删除重启生效的配置，工具将会输出如下信息，提示需要重启节点
   
   ```lang-javascript
   config 'scm.sdb.connectTimeout' require restart to take effect.
   ```