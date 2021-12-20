本节将介绍一个小规模的 SequoiaCM 集群从版本 3.1.2 滚动升级至 3.2 的过程。

## 滚动升级##

滚动升级是一种在线升级方式，SequoiaCM 的滚动升级流程需要注意以下几点：

- 选择在业务量最小的时间段内进行升级。

- 先对备中心节点进行升级，再升级主中心节点。

- 服务升级涉及到节点重启

    - 执行升级过程中，此服务相关的业务会出现部分请求失败，失败率与其部署的节点数成反比。（若服务仅部署单个节点，则升级过程中此服务不可用）

    - 待此次升级完成或一定时间间隔（约 30 s）后系统恢复正常。

## 案例##

### 环境介绍###

![升级案例集群环境][env_upgrade_case]

SequoiaCM 集群为两中心部署，每个服务均有一个节点实例落于主、备中心。此次升级需进行以下操作：

- 升级原有服务：cloud 组件服务、配置服务、调度服务、内容服务（主站点、分站点）

- 新增服务（可选操作）：om-server、守护进程工具

### 前置准备###

> 本章节假设 SequoiaCM 的安装用户为 scmadmin，用户组为 scmadmin_group，服务的安装路径为 /opt/sequoiacm/。

将新版本 SequoiaCM 3.2 版本的安装包上传至主、备中心的各个部署机（图示主机 A、B、C、D），并执行以下步骤：

1. 安装包解压缩

   ```
   $ tar -zxvf sequoiacm-3.1.3-release.tar.gz -C /opt/scm_upgrade/
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

### 新增服务###

1. 新增 OM 服务

   1). 切换至需要部署 OM 服务的机器

   2). 解压缩 OM 服务的安装包

    ```
      $ tar -zxvf /opt/scm_upgrade/sequoiacm/package/sequoiacm-om-3.1.3-release.tar.gz -C /opt/sequoiacm/
      ```

   3). 编辑部署配置文件，启动 OM 节点，详细步骤参考 OM 管理服务[部署][om_deploy]章节。

2. 新增守护进程

   > 守护进程无法跨机监控，需要部署至主、备中心上的所有机器，以监控整个 SequoiaCM 集群。

   **环境准备**

   若当前机器上部署 Zookeeper 作为 SequoiaCM 依赖服务（图示主机 A、B、C），需要替换 zkServer.sh 脚本，以支撑守护进程监控 Zookeeper 节点的相关功能。

   1). 移除本地 Zookeeper 服务的 zkServer.sh 脚本

   ```
   $ mv /opt/sequoiacm/zookeeper-3.4.12/bin/zkServer.sh  /opt/sequoiacm/zookeeper-3.4.12/bin/zkServer.sh.backup
   ```

   2). 使用安装包下的 3.4.12.sh 脚本替换 Zookeeper 服务目录下的 zkServer.sh 脚本

   ```
   $ mv /opt/scm_upgrade/sequoiacm/sequoiacm-deploy/bindata/zk_shell/3.4.12.sh /opt/sequoiacm/zookeeper-3.4.12/bin/zkServer.sh
   ```

   3). 修改 zkServer.sh 的所属用户和组，并赋予脚本可执行权限

   ```
   $ chown scmadmin:scmadmin /opt/sequoiacm/zookeeper-3.4.12/bin/zkServer.sh
   $ chmod +x /opt/sequoiacm/zookeeper-3.4.12/bin/zkServer.sh
   ```

   **部署守护进程**

   参照守护进程[部署][daemon_deploy]章节中单独部署的剩余步骤。

[om_deploy]:Om/deploy.md

[daemon_deploy]:Maintainance/Daemon/deploy.md

[env_upgrade_case]:Maintainance/Upgrade/env_upgrade_case.png