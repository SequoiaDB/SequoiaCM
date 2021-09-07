
# SequoiaCM

SequoiaCM 是一个基于 Spring Cloud 微服务架构的企业分布式内容管理系统。它主要被用于存储和管理企业大量的非结构化数据。

## 1 项目目录结构

```shell
|--dev.py       # 编译打包项目的脚本
|--doc          # 用户手册（ Markdown 文档、文档编译工具）
|--driver       # 用户驱动源码
|--README.md    # 打包部署 SequoiaCM 的说明文档
|--script       # 项目脚本（编译、部署等）
|--src          # 所有服务源码
|--testcases    # 测试用例
|--thirdparty   # 第三方库
|--tools        # 项目工具（部署工具、性能测试框架等）
```


## 2 打包

### 2.1 环境要求

| 软件环境 | 版本        |
| -------- | ----------- |
| jdk      | 1.8及以上   |
| python   | 2.7.x版本   |
| maven    | 3.3.9及以上 |
| node.js    | 12.0及以上 |

```xml
<!-- maven需要在settings.xml配置私服 -->
<mirrors>
    <mirror>
      <id>central</id>
      <mirrorOf>*</mirrorOf>
      <name>Human Readable Name for this Mirror.</name>
      <url>http://192.168.20.204:8082/repository/maven-public/</url>
    </mirror>
</mirrors>
```

### 2.2 clone项目

如果是在 Windows 下操作，在 clone 项目之前还需要设置 ：

```shell
# 设置禁止 git 自动转换换行符
git config --global core.autocrlf false
git config --global core.safecrlf true
```

> 注：若是不设置禁用，git 换行符在 Windows 和 Linux 上将会不一致，这会导致 Windows 上编译的 jar 无法在 Linux 上使用。

```shell
# 克隆 SequoiaCM 源码
git clone http://gitlab.sequoiadb.com/sequoiadb/sequoiacm.git
```

### 2.3 检查环境

```shell
# 检查用户环境是否符合打包要求
bash checkScmEnv.sh
```

> 注：如果是在 Windows 上打包，先打开 git 命令行窗口，再执行命令。

### 2.4 打包项目

```shell
# 进入源码根目录，编译、打包项目
python dev.py --compile all --archive
```

编译打包之后的产物在 `源码根目录/archive-target` 目录下 `sequoiacm-3.1.1-release.tar.gz` 。

> 注：更多参数可以通过 `python dev.py -h` 进行查看。

### 2.5 解压目录

```shell
cd /opt/git/sequoiacm/archive-target
# 解压项目编译后的包
tar -zxvf sequoiacm-3.1.1-release.tar.gz
```

产物解压后的目录结构：

```shell
|--deploy.log       # 部署日志（该文件部署之后才会生成）
|--driver           # 用户驱动的压缩包
|--pacakge          # 各种服务的压缩包
|--README.md        # 部署说明文档
|--scm.py           # 部署集群、操作工作区的脚本
|--sequoiacm-deploy # SequoiaCM 部署工具
```

## 3 部署

### 3.1 修改配置文件 deploy.cfg

根据[官网](https://doc.sequoiadb.com/cn/sequoiacm-cat_id-1551174548-edition_id-301)配置 deploy.cfg ，使用的 Domain 应该先在 SequoiaDB 中创建。

> 注：单机部署 SequoiaCM 集群需要 16G 内存。如果机器内存不足，可以多加机器，将部分服务部署在新加的机器上。也可以在 deploy.cfg 的 [sitenode] 和 [servicenode] 的 CustomNodeConf 加上 '{scm.jvm.options: "-Xmx256m"}' 。

### 3.2 执行部署

所有参与部署的机器，需要关闭防火墙和相互配置好 host 映射关系，具体操作可根据[官网](https://doc.sequoiadb.com/cn/sequoiacm-cat_id-1551174435-edition_id-301)进行。

```shell
# 执行部署 SequoiaCM 脚本
/opt/git/sequoiacm/archive-target/sequoiacm/scm.py cluster --deploy --conf /opt/git/sequoiacm/archive-target/sequoiacm/sequoiacm-deploy/conf/deploy.cfg
```

scm.py 脚本通过选择 cluster 或者 workspace ，可以用来部署 scm 集群或者操作工作区

> 注：如果部署失败，请先清除再重新部署，将 `--deploy` 替换成 `--clean` 即可清除。
>
> 更多参数可以通过 `scm.py cluster --help` 进行查看。

### 3.3 查看所有服务

```http
# 查看服务注册 Eureka ，确保部署的所有服务都成功开启
http://{SERVICE-CENTER-IP:PORT}
```

### 3.4 创建工作区

根据[官网](https://doc.sequoiadb.com/cn/sequoiacm-cat_id-1551174548-edition_id-301)配置 workspaces.json ，使用的域 Domain 应先在 SequoiaDB 中创建。

```shell
# 创建工作区的命令
/opt/git/sequoiacm/archive-target/sequoiacm/scm.py workspace --create --grant-all-priv --conf /opt/git/sequoiacm/archive-target/sequoiacm/sequoiacm-deploy/conf/workspaces.json
```

> 注：使用 --grant-all-priv 参数可以给工作区的创建者赋予该工作区的所有权限
>
> 更多参数可以通过 `scm.py workspace --help` 进行查看。

## 4 节点目录

SequoiaCM 部署完了之后，每台主机会在 deploy.cfg 的 InstallPath 指定路径上创建服务的节点目录，每一服务目录结构整体如下：

```shell
|--bin              # 包含该服务对应的管理工具、节点控制工具以及服务部署工具
|--conf             # 启动该服务节点的配置文件
|--jars/lib         # 该服务需要的 jar 包，在 content 服务中是 lib
|--log              # 该服务的使用日志
|--tmp              # 存放该服务节点的部署配置文件
|--deploy.json      # 该服务节点的配置配置文件模板
|--deploy.py        # 该服务节点的部署脚本
```

如下是内容服务节点的 log 目录说明，其它服务的 log 目录也类似：

```shell
|--admin
    |--admin.log                    # 执行 scmadmin.sh 脚本输出的日志
|--content-server
    |--15000                        # 根据该服务设置的端口命名
        |--audit.log                # 审计日志
        |--configserver.log         # 该服务节点运行过程输出的日志
        |--error.out                # Linux 标准错误流输出日志
|--start
    |--start.log                    # 工具启动节点的输出日志
```

## 5 更新某个服务的程序文件

修改了某一服务的源码，想要测试修改结果，可以尝试如下做法，以修改了配置服务源码为例：

1. 将配置服务源码打成 jar 包
2. 停止该服务
3. 替换jar包
4. 重启服务
5. 在 Eureka 中检查该服务是否重启成功

```shell
cd /opt/sequoiacm/sequoiacm-config/bin
confctl.sh stop -t all
cd /opt/sequoiacm/sequoiacm-config/jars
# 替换jar包
confctl.sh start -t all
```

> 注：其他服务的部署命令可见[官网](https://doc.sequoiadb.com/cn/sequoiacm-cat_id-1540966099-edition_id-301)。

## 6 Q&A

### 6.1 部署类

Q1.执行部署文件，提示 `Configuration error:failed to parse conf...` ，怎么办？

A1.deploy.cfg 文件内容格式不对，检查配置格式是否存在问题。

---

Q2.执行部署命令出错，如何定位问题？

A2.解决步骤：

1. 进入 `/opt/sequoiacm/deploy.log` ，查看部署日志，根据日志最后的报错信息，定位是哪一个微服务出现问题
2. 进入该微服务的 log 目录下，查看该微服务的 start.log 目录，查看该微服务节点是否启动成功，若未成功启动，则根据报错解决
3. 若启动成功，则查看该服务运行过程的输出日志（ 服务名.log ）

---

Q3.部署成功了，但检查 Eureka 却缺少服务，怎么办？

A3.可能是因为内存不足发生了 oom kill 。可通过命令 `grep "Out of memory" /var/log/messages` 检查部署该服务机器。

解决办法有三种，如下：

- 调节最大堆内存，在 deploy.cfg 服务节点的 CustomNodeConf 加上 '{scm.jvm.options: "-Xmx256m"}'
- 多增加一台机器，将部分服务部署到该机器上，再重新部署
- 先释放内存再部署，先执行刷盘 `sync` ，再释放资源 `echo 3 > /proc/sys/vm/drop_caches` ，最后改回 `echo 0 > /proc/sys/vm/drop_caches` ，默认为0，即系统自动管理，3表示释放所有。

---

Q4.部署全文索引节点失败，怎么办？

A4.先确定是否了部署 elasticsearch 环境，再确定是否已经将 elasticsearch 地址添加到全文索引节点配置项中，若不是以上两个原因引起，则根据 Q2 定位问题。

### 6.2 使用类

Q1.使用 scm.py 脚本创建工作区时，出现 `create meta collection failed:wsName=工作区...` ，怎么办？

A1.因为重新部署之前没有先清理工作区，导致 SequoiaDB 中残留了工作区的集合空间。具体报错信息见 `config-server` 的日志。

解决办法如下：

1. 连接到元数据服务的 sdb shell 
2. `drop 工作区_META` 这个CollectionSpace
3. 重新创建工作区

