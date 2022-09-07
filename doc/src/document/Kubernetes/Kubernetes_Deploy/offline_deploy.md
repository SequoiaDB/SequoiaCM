本节将介绍离线环境快速搭建 Kubernetes 的部署步骤。


>  **Note：**
> 
>  * 请参照[环境要求][Require]中的描述，确保环境符合部署要求。  
>
>  * 以下部署步骤均在主控机上执行


###部署 Kubernetes###
1.主控机上传相关部署包：kubeasz-offline.tar.gz
>  **Note：**
> 
>  * 离线包版本：Kubernetes v1.15.0，Helm v2.14.1，Docker CE 18.09.6。


2.主控机配置免密登录其它机器

```
ssh-keygen -t rsa -b 2048 -N '' -f ~/.ssh/id_rsa

# $ips为所有机器的地址，包括主控机本身
ssh-copy-id $ips  
```
>  **Note：**
> 
>  * 执行 ssh-copy-id 时，请根据提示输入对应主机的用户名密码。

3.解压 kubeasz_offline.tar.gz 至 /etc/ 

```
tar -xvf kubeasz_offline.tar.gz -C /etc/
```

4.配置集群部署规划文件

```
cp /etc/ansible/example/hosts.multi-node /etc/ansible/hosts  # 拷贝模板文件
vi /etc/ansible/hosts  # 编辑规划文件，内容如下
# 'etcd' cluster should have odd member(s) (1,3,5,...)
# variable 'NODE_NAME' is the distinct name of a member in 'etcd' cluster
[etcd]
192.168.1.1 NODE_NAME=etcd1
192.168.1.2 NODE_NAME=etcd2
192.168.1.3 NODE_NAME=etcd3

# master node(s)
[kube-master]
192.168.1.1
192.168.1.2

# work node(s)
[kube-node]
192.168.1.3
192.168.1.4

#...其它缺省配置
```

最简环境 [etcd]、 [kube-master]、 [kube-node]均填写本机 IP 即可

5.确认规划的节点互通

```
ansible all -m ping  # 正常能看到节点返回 Success
```


6.配置 Docker 安装参数

```
vi /etc/ansible/roles/docker/defaults/main.yml

 # 按需修改 docker 容器存储目录，确保有 10g 以上的磁盘空间
 STORAGE_DIR: "/var/lib/docker"


 # 信任的 HTTP 仓库,填写本机 IP:5000,后续将在本机创建私人 Docker 仓库
 INSECURE_REG: '["本机IP:5000"]'
 
 #...忽略其它缺省配置
   
```

7.检查离线文件，安装 Docker

```
cd /etc/ansible && ./tools/easzup -D
```
8.安装 Docker 私人仓库

```
#载入离线仓库镜像
docker load -i /etc/ansible/down/registry  
# 启动私人仓库容器，仓库的存储目录为本机的 /opt/docker_registry
docker run -d -v /opt/docker_registry:/var/lib/registry -p 5000:5000 --restart=always registry 
```

9.启动 kubeasz 容器

```
cd /etc/ansible && ./tools/easzup -S
```

10.通过 kubeasz 容器进行 Kubernetes 部署

```
#进入容器
docker exec -it kubeasz sh  

#修改离线安装参数
cd /etc/ansible 
sed -i 's/^INSTALL_SOURCE.*$/INSTALL_SOURCE: "offline"/g' roles/chrony/defaults/main.yml
sed -i 's/^INSTALL_SOURCE.*$/INSTALL_SOURCE: "offline"/g' roles/ex-lb/defaults/main.yml
sed -i 's/^INSTALL_SOURCE.*$/INSTALL_SOURCE: "offline"/g' roles/kube-node/defaults/main.yml
sed -i 's/^INSTALL_SOURCE.*$/INSTALL_SOURCE: "offline"/g' roles/prepare/defaults/main.yml

#开始安装
ansible-playbook 90.setup.yml 

#安装完毕，退出容器
exit

```

11.查看部署结果

```
# 刷新环境变量
su -

# 可以看到所有节点为 Ready 状态
kubectl get node 

# 查看 kubernetes-dashboard 服务对外暴露的端口：80:port/TCP，浏览器访问任意集群机器的 port 端口，查看 kubernetes-dashboard
kubectl get svc -n kube-system
```

###部署 Helm###
Helm 是一个用于 Kubernetes 应用的包管理工具，主要用来管理 Charts。类似于 Ubuntu 中的 APT 或 CentOS 中的 YUM。

1.在主控机推送 Helm 服务端 tiller 镜像至私人仓库

```
# a) 载入离线镜像
docker load -i /etc/ansible/down/kube_tiller.tar

# b) 查看镜像的 ID
docker images
REPOSITORY      TAG      IMAGE ID            CREATED             SIZE
easzlab/tiller  v2.14.1  ac22eb1f780e        2 months ago        94.2MB

# c) 重新tag
docker tag ac22eb1f780e  主控机IP:5000/tiller:v2.14.1

# d) push 至私人仓库
docker push 主控机IP:5000/tiller:v2.14.1
```

2.主控机创建本地 Helm Repo

```
mkdir -p /opt/helm-repo
nohup  /etc/ansible/bin/helm serve --address 127.0.0.1:8879 --repo-path /opt/helm-repo &
````

3.主控机修改 Helm 安装配置

```
vi /etc/ansible/roles/helm/defaults/main.yml 
  #修改为私人仓库的 tiller 镜像
  tiller_image: 主控机IP:5000/tiller:v2.14.1
 #修改为本地仓库
 repo_url: http://127.0.0.1:8879
```

4.通过 kubeasz 容器部署 Helm

```
#进入容器
docker exec -it kubeasz sh  
#执行安装
ansible-playbook /etc/ansible/roles/helm/helm.yml 
#退出容器
exit
```

5.验证结果

```
# 刷新环境变量
su -

#查看 helm 版本，正常显示 helm 服务端、客户端的版本
helm version
```

[Require]:Kubernetes/Kubernetes_Deploy/require.md
