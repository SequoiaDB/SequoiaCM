本节将介绍在线环境 SequoiaCM 在 Kubernetes 的部署，以最简 SequoiaCM 部署为示例（一个主站点，每个服务启动一个节点）

>  **Note：**
> 
>  * 请参照[环境要求][Require]中的描述，确保环境符合部署要求。  

###部署前准备###
1.SequoiaDB（3.0以上）集群：开启事务，假设地址为：192.168.31.32:11810

2.Zookeeper：假设地址为：192.168.31.32:2181

###部署###
1.上传相关部署包：sequoiacm-3.0.0.tgz

2.执行如下命令获取 SequoiaCM 配置文件

```
helm inspect values ./ sequoiacm-3.0.0.tgz  > ./myValues.yaml
```

3.编辑配置文件

```
vi ./myValues.yaml
  metasource:
     url: &meta_url  192.168.31.32:11810
     user: &meta_user  sequoiadb
     password: &meta_pwd sequoiadb
     domain: domain1  #domain 需要在 Sequoiadb 预先建立
  zookeeper:
     url: 192.168.31.32:2181 
  #...忽略其它默认配置

```

4.执行安装

```
helm install   ./sequoiacm-3.0.0.tgz -f ./myValues.yaml
```
5.等待所有节点就绪

```
kubectl get pod 
```

6.SequoiaCM 以 NodePort 形式对外暴露服务，如下命令查看服务端口

```
kubectl get svc
 eureka-zone1-svc              NodePort    10.68.166.131   <none>        8800:38800/TCP   12m
 gateway-zone1-svc             NodePort    10.68.25.187    <none>        7070:38080/TCP   12m
 scm-admin-svc                 NodePort    10.68.134.77    <none>        6879:29713/TCP   12m
```

[Require]:Kubernetes/SequoiaCM_Deploy/require.md
