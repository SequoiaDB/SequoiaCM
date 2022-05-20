### ZooKeeper 介绍

ZooKeeper 是一个分布式的，开放源码的分布式应用程序协调服务，在 SequoiaCM 中，ZooKeeper 主要用于分布式锁和分布式选举：

| **服务名**      | **使用场景** |
| --------------- | ------------ |
| admin-server    | 分布式锁     |
| config-server   | 分布式锁     |
| content-server  | 分布式锁     |
| fulltext-server | 分布式锁     |
| mq-server       | 分布式锁     |
| s3-server       | 分布式锁     |
| schedule-server | 分布式选举   |

### ZooKeeper 安全配置项

ZooKeeper 基于 ACL 实现访问权限控制，使用 scheme:id:permissions 来标识访问权限，其中 scheme 表示授权方式，包括 digest、ip、world 等，id 表示授权的对象，permissions 表示具体的权限值。在 SequoiaCM 中，使用 digest 作为授权方式，使用 crwda 作为权限值，用户可通过以下配置项完成 ZooKeeper ACL 权限配置：

| **配置项**                    | **类型** | **说明**                                                     |
| ----------------------------- | -------- | ------------------------------------------------------------ |
| scm.zookeeper.acl.enabled     | boolean  | 是否开启 ACL 权限控制，默认值：false                            |
| scm.zookeeper.acl.id          | str   | 授权对象，digest 授权方式下这里填写的是用户名密码串（username:password）的加密文件路径 |

### 开启 ZooKeeper 安全配置

- **开启** **ACL** **权限控制**

1. 使用 SequoiaCM [加密工具][tool_config]，生成 scm.zookeeper.acl.id 配置项的加密字符串

    ```Shell
     # 假设拟授权的用户名为 zookeeper，密码为 sequoiacm （可自由指定，设置后不可修改）
     scmadmin.sh encrypt -u zookeeper -p zookeeper:sequoiacm
    ```

2. 将生成的加密字符串保存在文件中，假设保存在：/opt/sequoiacm/secret/zookeeper.pwd，将该文件拷贝到部署 SequoiaCM 节点的机器上
3. 停止全部 SequoiaCM 节点
4. 修改各节点配置文件，加入以下配置：

    ```
    scm.zookeeper.acl.enabled=true
    scm.zookeeper.acl.id=/opt/sequoiacm/secret/zookeeper.pwd
    ```

5. 重启全部节点

   

> **Note:**
> 
> * 配置 ACL 前需要停止全部节点
> 
> * ZooKeeper 授权用户、密码在首个使用 ZooKeeper 的 SequoiaCM 节点启动后生效、SequoiaCM 集群中的所有节点的 ACL 配置需要保持一致，设置后不可更改
>
> * ACL 启用后，不允许关闭




- **关闭四字命令**

ZooKeeper 默认提供了一系列四字命令（如 stat、envi、conf 等），运维人员可以通过这些四字命令远程查看 ZooKeeper 服务的状态信息，建议关闭四字防止 ZooKeeper 被非法访问。

1. 编辑 ZooKeeper 配置文件：<ZooKeeper 安装目录>/conf/zoo.cfg，加入以下配置：

   ```
   4lw.commands.whitelist=   # 设置四字命令的白名单为空，即默认禁用所有四字命令
   ```

2. 重启 ZooKeeper 节点：

   ```
   zkServer.sh restart
   ```


[tool_config]:Maintainance/Tools/Scmadmin/encrypt.md