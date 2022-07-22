守护进程不具备跨机监控能力，若要监控整个 SequoiaCM 集群，则需要在每一台安装机上部署守护进程。

## 环境要求##

守护进程依赖于 Linux 的 crontab 定时任务，需保证安装机上此功能可用。

- 验证方法

  以 CentOS 为例，执行列取当前用户创建的 crontab 定时任务，无错误输出。

  ```lang-bash
  $ crontab -l
  ```

## 快速部署##

- SequoiaCM 3.1.3 及之后版本，使用部署工具部署 SequoiaCM 集群时，默认会在所有主机上安装并启动守护进程。

- 可通过修改部署工具配置文件 deploy.cfg 禁用（不安装）守护进程。

  ```lang-ini
  [daemon]
  EnableDaemon
  false
  ```

## 单独部署##

### SequoiaCM 服务准备###

对于 3.1.2 及之前版本的 SequioaCM 集群，引入守护进程前需要升级所有的服务工具（此过程无需停止节点）。如下步骤以升级内容服务工具为例。

1. 解压 sequoiacm-3.1.3-release.tar.gz 压缩包至 /opt/scm_new/

   ```lang-bash
   $ tar -zxvf sequoiacm-3.1.3-release.tar.gz -C /opt/scm_new/
   ```

2. 进入服务包目录，并解压内容服务安装包

   ```lang-bash
   $ cd /opt/scm_new/sequoiacm/package
   $ tar -zxvf sequoiacm-content-3.1.3-release.tar.gz
   ``` 

3. 将新版本的内容服务工具包拷贝至内容服务安装目录的子目录 lib 下

   ```lang-bash
   $ mv  /opt/scm_new/sequoiacm/package/sequoiacm-content/lib/sequoiacm-content-tools-3.1.3.jar /opt/sequoiacm/sequoiacm-content/lib
   ```

   > **Note：**
   >
   >  * 其他类型服务的工具包在 jars 目录下。

4. 指定新版本工具包所属用户及用户组为 SequoiaCM 的安装用户、用户组

   ```lang-bash
   $ chown scmadmin:scmadmin_group /opt/sequoiacm/sequoiacm-content/lib/sequoiacm-content-tools-3.1.3.jar
   ```

5. 移除旧版本工具包

   ```lang-bash
   $ mv  /opt/sequoiacm/sequoiacm-content/lib/sequoiacm-content-tools-3.1.2.jar /opt/sequoiacm/sequoiacm-content/sequoiacm-content-tools-3.1.2.jar.backup
   ```

### Zookeeper 服务准备###

对于使用 SequoiaCM 3.1.2 及之前版本安装包部署的 Zookeeper 服务，需要升级其 zkServer.sh 脚本。如下步骤以 3.4.12 版本的Zookeeper 服务为例。

1. 解压 sequoiacm-3.1.3-release.tar.gz 压缩包至 /opt/scm_new/

   ```lang-bash
   $ tar -zxvf sequoiacm-3.1.3-release.tar.gz -C /opt/scm_new/
   ```

2. 移除 Zookeeper 服务旧版本的 zkServer.sh 脚本

   ```lang-bash
   $ mv  /opt/sequoiacm/zookeeper-3.4.12/bin/zkServer.sh /opt/sequoiacm/zookeeper-3.4.12/bin/zkServer.sh.backup
   ```

3. 使用安装包下的 3.4.12.sh 脚本替换 Zookeeper 服务目录下的 zkServer.sh 脚本

   ```lang-bash
   $ cp /opt/new/sequoiacm/sequoiacm-deploy/bindata/zk_shell/3.4.12.sh /opt/sequoiacm/zookeeper-3.4.12/bin/zkServer.sh
   ```

4. 修改 zkServer.sh 的所属用户和组，并赋予脚本可执行权限

   ```lang-bash
   $ chown scmadmin:scmadmin zkServer.sh
   $ chmod +x zkServer.sh
   ```

### 部署守护进程###

1. 解压 sequoiacm-3.1.3-release.tar.gz 压缩包

   ```lang-bash
   $ tar -zxvf sequoiacm-3.1.3-release.tar.gz -C /opt/scm_new/
   ```

2. 拷贝守护进程安装包至服务目录 /opt/sequoiacm 下

   ```lang-bash
   $ cp /opt/scm_new/sequoiacm/package/daemon-3.1.3-release.tar.gz /opt/sequoiacm
   ```

3. 进入服务安装目录，并解压缩守护进程安装包

   ```lang-bash
   $ cd /opt/sequoiacm
   $ tar -zxvf daemon-3.1.3-release.tar.gz
   ```

4. 指定守护进程目录所属用户及用户组为 SequoiaCM 的安装用户、用户组

   ```lang-bash
   $ chown scmadmin:scmadmin_group /opt/sequoiacm/daemon/ -R
   ```

5. 赋予守护进程 bin 目录下所有文件的可执行权限

   ```lang-bash
   $ chmod +x /opt/sequoiacm/daemon/bin/*
   ```

5. 开启守护进程

   ```lang-bash
   $ /opt/sequoiacm/daemon/bin/scmd.sh start
   ```