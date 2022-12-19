SequoiaCM 支持通过动态配置和配置文件的方式进行参数配置，用户可根据实际需求选择配置方式。

## 动态配置方式 ##

用户可通过配置服务提供的 [updateConf][updateconfig] 和 [deleteConf][updateconfig] 在线修改节点的配置文件，如果参数的生效类型为“在线生效”，则配置完成后立即生效；如果参数的生效类型为“重启生效”，配置完成后需重启节点才能使配置生效。生效类型可参考各个服务节点的配置参数说明。

## 配置文件方式 ##

用户可通过配置文件方式配置参数，配置完成后需重启节点才能使配置生效。以修改内容服务节点 15000 为例，具体操作如下：

1. 切换至 SequoiaCM 安装目录，以 /opt/sequoiacm 为例

    ```
    $ cd /opt/sequoiacm
    ```

2. 编辑配置文件 sequoiacm-content/conf/content-server/15000/application.properties


    ```
    $ vi sequoiacm-content/conf/content-server/15000/application.properties
    ```

3. 写入需要修改的配置，重启节点使配置生效


[deleteconfig]:Maintainance/Tools/Confadmin/deleteconfig.md
[updateconfig]:Maintainance/Tools/Confadmin/updateconfig.md