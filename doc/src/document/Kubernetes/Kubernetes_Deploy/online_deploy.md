本节将介绍在线环境快速搭建 Kubernetes 的部署步骤。


>  **Note：**
> 
>  * 请参照[环境要求][Require]中的描述，确保环境符合部署要求。  
>
>  * 以下部署步骤均在主控机上执行


###部署###
1.主控机上传相关部署包：kubeasz-master.zip

2.主控机配置免密登录其它机器

```
ssh-keygen -t rsa -b 2048 -N '' -f ~/.ssh/id_rsa
ssh-copy-id $ips  # $ips为所有机器的地址，包括主控机本身
```
>  **Note：**
> 
>  * 执行 ssh-copy-id 时，请根据提示输入对应主机的用户名密码。

3.解压 kubease-master.zip 

```
unzip kubease-master.zip
```

4.下载安装文件，安装 Docker

```
kubease-master/tools/easzup -D
```

5.配置集群部署规划文件

```
cp kubease-master/example/hosts.multi-node /etc/ansible/hosts  # 拷贝模板文件
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

6.确认规划的节点互通

```
ansible all -m ping  # 正常能看到节点返回 Success
```

7.执行部署脚本

```
ansible-playbook 90.setup.yml
```

8.查看部署结果

```
# 可以看到所有节点为 Ready 状态
kubectl get node 

# 查看 kubernetes-dashboard 服务对外暴露的端口：80:port/TCP，浏览器访问任意集群机器的 port 端口，查看 kubernetes-dashboard
kubectl get svc -n kube-system
```
 
###部署Helm###
Helm 是一个用于 Kubernetes 应用的包管理工具，主要用来管理 Charts。类似于 Ubuntu 中的 APT 或 CentOS 中的 YUM。

1.主控机执行安装命令

```
ansible-playbook /etc/ansible/roles/helm/helm.yml
```

2.验证结果

```
#重新 SSH 至本机
ssh localhost
#查看 helm 版本，正常显示 helm 服务端、客户端的版本
helm version
```

[Require]:Kubernetes/Kubernetes_Deploy/require.md
