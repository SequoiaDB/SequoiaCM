如果用户不再需要 SequoiaCM 企业内容管理软件，或需要重新部署 SequoiaCM 集群时，可以选择卸载。

##卸载前的检查##

- 确保 SequoiaCM 未处于使用状态且不再使用
- 卸载过程中需使用 root 用户权限

##数据备份##

由于 SequoiaCM 卸载成功之后，将自动删除 SequoiaDB 集群中存放工作区元数据和内容数据的集合空间、服务节点目录，并关闭所有服务节点，用户需自行对需要保留的数据进行备份。

##卸载步骤##

卸载前需清理工作区，否则会导致 SequoiaDB 集群残留工作区相关的集合信息，影响下一次 SequoiaCM 集群部署。下述以产品包 `sequoiacm-3.2.0-release.tar.gz` 已解压缩至目录 `/opt/data/` 为例，介绍具体的卸载步骤。

1. 切换至解压后的目录

    ```lang-bash
    # cd /opt/data/sequoiacm/
    ```

2. 清理工作区

    ```lang-bash
    # ./scm.py workspace --clean --conf sequoiacm-deploy/conf/workspaces.json
    ```

3. 卸载 SequoiaCM 集群

    ```lang-bash
    # ./scm.py cluster --clean --conf sequoiacm-deploy/conf/deploy.cfg
    ```