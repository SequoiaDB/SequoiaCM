# SCM 诊断工具

当部署 SCM 集群或集群出现故障时，可使用此工具提供的日志收集，集群连通性检查，集群信息收集功能，收集集群相关信息，方便 SCM 集群问题定位

## 1 工具介绍

### 1.1 工具目录

```shell
/sequoiacm-diagnose
	|--bin/           			  # 工具执行入口
	|--conf/                      
		|--collect.conf	                  # 远程收集配置文件
		|--logback.xml	                  # 工具日志信息配置
		|--compare-log.xml	                  # 数据一致性检测日志信息配置
		|--residue-log.xml	                  # 数据残留检测日志信息配置
	|--jar/           			  # 工具执行所需 jar 包
	|--log/
	    |--scm-diagnose.log                   # 运行工具产生的日志
	    |--datacheck
	        |--compare.log      #数据一致性检测日志文件
	        |--residue.log      #数据残留检测日志文件
```

### 1.2 子命令列表

| 子命令          | 描述                   |
|--------------|----------------------|
| log-collect  | 日志收集，指定机器收集 SCM 节点日志 |
| conn-check   | 集群连通性检查，集群下各节点连通性检查  |
| cluster-info | 集群信息收集，集群的主机和节点信息收集  |
| compare      | 数据一致性检测              |
| residue      | 数据残留检测               |

### 1.3 命令参数

#### log-collect

| 参数              | 是否必填 | 描述                                                                                  |
|-----------------|------|-------------------------------------------------------------------------------------|
| --hosts         | 否    | 已配置免密登录主机。主机与port使用：分割，port默认22，多个主机使用,分割。例如：host1:port,host2,host3:port<br/>默认收集本机 |
| --conf          | 否    | 指定收集配置文件，当配置文件配置参数与命令行参数同时指定，命令行参数值优先级高                                             |
| --services      | 否    | 指定收集的服务名，多个服务名使用，分割，例如：admin-server,auth-server,gateway 默认收集所有服务                    |
| --max-log-count | 否    | 指定每个节点收集最新修改日志文件数，默认 1                                                              |
| --output-path   | 否    | 日志收集产物目录，默认/opt/scm-diagnose                                                        |                                                                |
| --install-path  | 否    | SCM 服务安装路径，默认/opt/sequoiacm                                                         |
| --thread-size   | 否    | 多台主机日志收集并发度，每台主机单线程收集，默认单线程（多台主机单线程）                                                |
| --need-zip      | 否    | 收集产物是否压缩，以节点为单位进行压缩，默认压缩                                                            |
| --help          | 否    | 查看命令帮助                                                                              |

#### conn-check

| 参数             | 是否必填 | 描述                   |
|----------------|------|----------------------|
| --gateway      | 是    | SCM 集群网关地址节点，ip:port |
| --help         | 否    | 查看命令帮助               |

#### cluster-info

| 参数            | 是否必填 | 描述                                                                                  |
|---------------|------|-------------------------------------------------------------------------------------|
| --gateway     | 是    | SCM 集群网关地址节点，ip:port                                                                |
| --hosts       | 否    | 已配置免密登录主机。主机与port使用：分割，port默认22，多个主机使用,分割。例如：host1:port,host2,host3:port<br/>默认收集本机 |
| --conf        | 否    | 指定收集配置文件，当配置文件配置参数与命令行参数同时指定，命令行参数值优先级高                                             |
| --output-path | 否    | 集群信息收集产物目录，默认/opt/scm-diagnose                                                      |                                                                |
| --thread-size | 否    | 多台主机集群信息收集并发度，每台主机单线程收集，默认单线程（多台主机单线程）                                              |
| --need-zip    | 否    | 集群信息收集产物是否压缩，以主机为单位进行压缩，默认压缩                                                        |
| --help        | 否    | 查看命令帮助                                                                              |

#### compare

| 参数             | 是否必填 | 描述                                                                                           |
|----------------|------|----------------------------------------------------------------------------------------------|
| --work-path    | 是    | 指定工作路径，核验结果、日志将在此目录生成                                                                        |
| --workspace    | 是    | 需要进行一致性检测的工作区名                                                                               |
| --begin-time   | 是    | 要检测的文件的创建时间范围起始区间，格式 `yyyyMMdd`，如 20230301，对应文件元数据的 create_time                              |
| --end-time     | 是    | 要检测的文件的创建时间范围结束区间，格式 `yyyyMMdd`，如 20230401，对应文件元数据的 create_time                              |                                                                |
| --url          | 是    | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>/<siteName>`                                        |
| --user         | 是    | 指定管理员用户的用户名                                                                                  |
| --passwd       | 是    | 指定管理员用户的密码，不指定值表示采用交互式输入密码                                                                   |
| --check-level  | 否    | 数据检测级别，包括 1（数据大小）、2（md5），默认 md5 检测级别                                                         |
| --full         | 否    | 布尔值，检测结果是否全量输出，为false时，表示只输出检测结果不一致的信息到检测结果文件中，为true时表示全量输出，即检测结果一致的信息也会输出到检测结果文件中，默认值 false |
| --worker-count | 否    | 工具执行任务时的最大并发数，默认 1，可选范围 【1,100】，建议不要太大，太大会影响系统性能                                             |
| --help         | 否    | 查看命令帮助                                                                                       |

#### residue

| 参数                 | 是否必填 | 描述                                                    |
|--------------------|------|-------------------------------------------------------|
| --work-path        | 是    | 指定工作路径，核验结果、日志将在此目录生成                                 |
| --workspace        | 是    | 需要进行数据残留检测的工作区名                                       |
| --site             | 是    | 需要进行数据残留检测的站点名（目前只支持 SequoiaDB 数据源站点）                 |
| --url              | 是    | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>/<siteName>` |
| --user             | 是    | 指定管理员用户的用户名                                           |
| --passwd           | 是    | 指定管理员用户的密码，不指定值表示采用交互式输入密码                            |
| --data-table       | 否    | 要检测的数据表名                                              |
| --dataid-file-path | 否    | 要检测的数据 id 列表文件路径（当 data-table 为空时，必填）                 |
| --max              | 否    | 可以进行检测的最大数据量，当超过这个数时，则拒绝检测，默认值 10000，范围【1,1000000】    |
| --worker-count     |      | 工具执行任务时的最大并发数，默认 1，可选范围 【1,100】，建议不要太大，太大会影响系统性能      | 否    |
| --help             | 否    | 查看命令帮助                                                |

## 2 工具使用

### 2.1 log-collect 使用

日志收集:收集 SCM 节点的 log 和 error 日志

```shell
./bin/scmdiagnose.sh log-collect --hosts host1:port,host2,host3:port --conf conf/collection.conf 
```

日志收集产物目录

```shell
outputPath/scm-collect-logs-date
    /service1
        /host1_port
            service.log
            error.out
        /host2_port
            service.log
            error.out
    /service2
        /host1_port
            service.log
            error.out
    /service3
    ......
```

### 2.2 conn-check 使用

集群连通性检查:检查集群下各个节点之间的连通性

```shell
./bin/scmdiagnose.sh conn-check --gateway ip:port
./bin/scmdiagnose.sh conn-check --gateway ip:port > scm_diagnose.txt    #把检查连通性结果重定向文件中
```

### 2.3 cluster-info 使用

集群信息收集：收集集群规模（主机数，服务数，节点数），若配置SSH登录，可收集集群的主机信息（内存，磁盘，网络，cpu，系统以及运行状况）和节点信息（jstack，tcp连接）

```shell
./bin/scmdiagnose.sh cluster-info --gateway ip:port --conf conf/collection.conf 
```

集群信息收集产物目录

```shell
outputPath/scm-collect-cluster-date
    cluster_info.txt
    /host1
        /host_info
            memory.txt
            cpu.txt
            ....
        /nodes_info
            /service1_port
                jstack.txt
                tcp.txt
            /service2_port
            ....
      
    /host2
        .....  
```

### 2.4 compare 使用

数据一致性检测：检测文件元数据与数据一致性，多数据源间数据一致性，检测维度：数据大小、md5 值

```shell
./bin/scmdiagnose.sh compare --work-path /opt/datacheck --workspace wsName --begin-time 20230301 --end-time 20230402 --url ip:port/rootsite -user adminUser --passwd adminPasswd
```

数据一致性检测产物目录

```shell
workPath
    /compare_result #compare 子命令结果文件路径
      /runTime #检测结果文件目录
        result #检测结果文件
        null_md5 #无md5值的文件
    /secret    #数据源密码文件目录
      metasource.pwd # 元数据服务密码文件
      ds1.pwd #数据服务密码文件
      ....
```

### 2.5 residue 使用

数据残留检测：检测数据是否残留

- 指定数据表名方式

```shell
./bin/scmdiagnose.sh residue --work-path /opt/datacheck --workspace wsName --site rootSite --url ip:port/rootsite -user adminUser --passwd adminPasswd --data-table ws_default_LOB_2023.LOB_202303
```

- 指定数据 id 列表文件路径

```shell
./bin/scmdiagnose.sh residue --work-path /opt/datacheck --workspace wsName --site rootSite --url ip:port/rootsite -user adminUser --passwd adminPasswd --dataid-file-path dataIdFilePath
```

数据残留检测产物目录

```shell
workPath
    /residue_result #residue 子命令结果文件路径
      /runTime #检测结果目录，根据检测的时间生成文件名
        error_list #检测失败的数据id列表文件
        residue_list #检测残留的数据id列表文件
    /secret
      metasource.pwd # 元数据服务密码文件
      ds1.pwd #数据服务密码文件
      ....
```