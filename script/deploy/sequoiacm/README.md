SequoiaCM
=================
SequoiaCM 是新一代分布式企业内容管理软件，用于企业海量非结构化数据的存储和管理。

部署集群
-----------------
* 1 . 规划集群，编辑配置文件：./sequoiacm-deploy/conf/deploy.cfg

* 2 . 执行部署

 ```lang-javascript
    python scm.py cluster --deploy --conf ./sequoiacm-deploy/conf/deploy.cfg
 ```

创建工作区
-----------------
* 1 . 规划工作区，编辑配置文件：./sequoiacm-deploy/conf/workspaces.json

* 2 . 执行创建

 ```lang-javascript
    python scm.py workspace --create --conf ./sequoiacm-deploy/conf/workspaces.json
 ```