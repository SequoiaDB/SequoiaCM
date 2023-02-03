conn-check 子命令提供集群连通性检查功能，可以对集群中所有已启动的微服务节点进行连通性检查。

##子命令选项##

| 选项             | 描述                         | 是否必填 |
|------------------|----------------------------|------|
| --gateway        | 指定 SequoiaCM 集群的网关服务节点，格式为 `<IP>:<port>`  | 是    |

###示例###


1. 切换至工具所在路径，以 SequoiaCM 安装目录 `/opt/sequoiacm` 为例，执行语句如下：

    ```lang-bash
    $ cd /opt/sequoiacm/tools/sequoiacm-scm-diagnose
    ```

2. 检查集群中各节点的连通性

    ```lang-bash
    $ ./bin/scmdiagnose.sh conn-check --gateway 192.168.32.111:8080
    ```

   输出结果如下：

    ```lang-text
    cluster info:
    u1604-xxx:17700(service=branchsite1,ip:192.168.17.184)
    u1604-xxx:16600(service=rootsite,ip:192.168.17.184)
    s-x86-u16-040401:8840(service=admin-server,ip:192.168.32.111)
    s-x86-u16-040401:8810(service=auth-server,ip:192.168.32.111)
    s-x86-u16-040401:8830(service=config-server,ip:192.168.32.111)
    s-x86-u16-040401:8080(service=gateway,ip:192.168.32.111)
    s-x86-u16-040401:8850(service=schedule-server,ip:192.168.32.111)
    s-x86-u16-040401:8888(service=service-center,ip:192.168.32.111)
    
    Begin to checking the network segment of the nodes...
    
    The nodes network segment is 192.168.*.*
    Begin to checking the connectivity of the nodes... 
    1.u1604-xxx:17700(service=branchsite1,ip=192.168.17.184) OK
    2.u1604-xxx:16600(service=rootsite,ip=192.168.17.184) OK
    3.s-x86-u16-040401:8840(service=admin-server,ip=192.168.32.111) OK
    4.s-x86-u16-040401:8810(service=auth-server,ip=192.168.32.111) OK
    5.s-x86-u16-040401:8830(service=config-server,ip=192.168.32.111) OK
    6.s-x86-u16-040401:8080(service=gateway,ip=192.168.32.111) OK
    7.s-x86-u16-040401:8850(service=schedule-server,ip=192.168.32.111) OK
    8.s-x86-u16-040401:8888(service=service-center,ip=192.168.32.111) OK
    total:8 success:8 failed:0
    ```