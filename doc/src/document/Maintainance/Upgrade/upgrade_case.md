本节将介绍一个小规模的 SequoiaCM 集群从版本 3.1.2 滚动升级至 3.2 的过程。

## 滚动升级##

滚动升级是一种在线升级方式，SequoiaCM 的滚动升级流程需要注意以下几点：

- 选择在业务量最小的时间段内进行升级。

- 先对备中心节点进行升级，再升级主中心节点。

- 服务升级涉及到节点重启，升级过程中可能会出现短暂的业务失败。

## 案例##

### 环境介绍###

![升级案例集群环境][env_upgrade_case]

SequoiaCM 集群为两中心部署，每个服务均有一个节点实例落于主、备中心。

### 前置准备###

> - 本节假设 SequoiaCM 的安装用户、用户组分别为 scmadmin、scmadmin_group，服务的安装路径为 /opt/sequoiacm/。

将新版本 SequoiaCM 3.2 版本的安装包上传至主、备中心的各个部署机（图示主机 A、B、C、D），并执行以下步骤：

1. 安装包解压缩

   ```
   $ tar -zxvf sequoiacm-3.2-release.tar.gz -C /opt/scm_upgrade/
   ```

2. 指定 /opt/scm_upgrade/sequoiacm/ 目录所属用户及用户组为 SequoiaCM 的安装用户、用户组

   ```
   $ chown scmadmin:scmadmin_group /opt/scm_upgrade/sequoiacm/ /opt/scm_upgrade/sequoiacm/ -R
   ```

3. 赋予升级脚本的可执行权限

   ```
   $ chmod +x /opt/scm_upgrade/sequoiacm/scmupgrade.py
   ```

   > **Note：**
   >
   >  * 若安装包中无升级脚本 scmupgrade.py，需从 3.2 及之后版本的安装包中获取。

### 升级原有服务###

> 升级前需切换至 SequoiaCM 安装用户。

1. 升级 Cloud 组件服务

   1). 在备中心 Cloud 组件服务所在主机（图示主机 C、D）上执行升级

   ```
   $ /opt/scm_upgrade/sequoiacm/scmupgrade.py \
	--service cloud \
	--install-path /opt/sequoiacm/sequoiacm-cloud/ \
	--start
   ```

   > **Note：**
   >
   >  * 执行成功将会输出 upgrade success! 的提示。

   2). 在主中心 Cloud 组件服务所在主机（图示主机 A、B）上执行升级

2. 升级配置服务

   1). 在备中心配置服务所在主机上执行升级

   ```
   $ /opt/scm_upgrade/sequoiacm/scmupgrade.py \
	--service config-server \
	--install-path /opt/sequoiacm/sequoiacm-config/ \
	--start
   ```

   2). 在主中心配置服务所在主机上执行升级

3. 升级内容服务（依次升级主站点，分站点）

   1). 在备中心内容服务所在主机上执行升级

   ```
   $ /opt/scm_upgrade/sequoiacm/scmupgrade.py \
	--service content-server \
	--install-path /opt/sequoiacm/sequoiacm-content/ \
	--start
   ```

   2). 在主中心内容服务所在主机上执行升级

4. 升级调度服务

   1). 在备中心调度服务所在主机上执行升级

   ```
   $ /opt/scm_upgrade/sequoiacm/scmupgrade.py \
	--service schedule-server \
	--install-path /opt/sequoiacm/sequoiacm-schedule/ \
	--start
   ```

   2). 在主中心调度服务所在主机上执行升级

[om_deploy]:Om/deploy.md

[daemon_deploy]:Maintainance/Daemon/deploy.md

[env_upgrade_case]:Maintainance/Upgrade/env_upgrade_case.png