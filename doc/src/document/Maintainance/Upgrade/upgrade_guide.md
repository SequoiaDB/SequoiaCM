SequoiaCM 提供升级脚本 scmupgrade.py 辅助完成各个服务的升级，本节主要介绍升级脚本，升级前准备以及如何升级原有服务。

## 升级前准备##

> 本节假设 SequoiaCM 的安装用户、用户组分别为 scmadmin、scmadmin_group。

- 阅读[升级注意事项][upgrade_tip]，完成升级准备，如节点配置调整
- 将新版本 SequoiaCM 安装包上传至 SequoiaCM 系统的安装机，并执行以下步骤：

1. 安装包解压缩

   ```
   $ tar -zxvf sequoiacm-3.1.3-release.tar.gz -C /opt/scm_upgrade/
   ```

2. 指定 /opt/scm_upgrade/sequioacm/ 目录所属用户及用户组为 SequoiaCM 的安装用户、用户组

   ```
   $ chown scmadmin:scmadmin_group /opt/scm_upgrade/sequoiacm/ -R
   ```

3. 赋予升级脚本的可执行权限

   ```
   $ chmod +x /opt/scm_upgrade/sequoiacm/scmupgrade.py
   ```

## 升级脚本介绍##

scmupgrade.py

|参数名|参数描述|备注|
|------|------------|----|
|--install-path|指定服务安装路径|必填，表示需要升级/回滚的服务节点|
|--service|指定节点类型|必填，可通过 —help 查看节点类型列表|
|--start|指定升级/回滚后自动重启节点|非必填，默认不启动，需要手动启节点|
|--rollback|指定该参数表示回滚操作|非必填，回滚 –install-path 路径下的节点|
|--nocheck|指定不做类型校验|非必填，默认会校验节点类型与指定的服务安装路径是否匹配|
|--dryrun|指定以dryrun模式运行|非必填，预演命令执行流程|
|--help/-h|打印帮助信息|非必填|

##升级原有服务##

以内容服务为例，此处假设内容服务的安装路径为 /opt/sequoiacm/sequoiacm-content/。

1. 切换至 SequoiaCM 安装用户

   ```
   $ su scmadmin
   ```

2. 升级与回滚

	**执行升级**

   	```
   	$ /opt/scm_upgrade/sequoiacm/scmupgrade.py \
	--service content-server \
	--install-path /opt/sequoiacm/sequoiacm-content/ \
	--start
   	```

	> **Note：**
	>
	>  * 若升级失败，处理完异常错误，可再次执行升级。
	>  * 若无法完成升级，或升级后需要做版本回退，可执行回滚操作。

	**升级回滚**

   ```
   $ /opt/scm_upgrade/sequoiacm/scmupgrade.py \
	--service content-server \
	--install-path /opt/sequoiacm/sequoiacm-content/ \
	--rollback
   ```

	> **Note：**
	>
	>  * 若回滚失败，处理完异常错误，可再次执行回滚。

3. 升级成功后，检查升级结果

	1）检查内容服务工具是否升级至目标版本

	```
    $ /opt/sequoiacm/sequoiacm-content/bin/scmctl.sh --version
    ```

	2）若节点已启动，检查节点状态

	```
    $ /opt/sequoiacm/sequoiacm-content/bin/scmctl.sh list
    ```

## 新增服务##

参考[系统工具][tools]章节，使用相应服务的管理工具创建、启动节点即可。



[tools]:Maintainance/Tools/Readme.md
[upgrade_tip]:Maintainance/Upgrade/compatibility.md#升级注意事项
