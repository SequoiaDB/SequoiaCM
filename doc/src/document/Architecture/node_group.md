
SequoiaCM 通过各个微服务间的协作，共同承载系统的主要功能和业务逻辑。但在多个业务同时访问一个节点的场景下，会导致资源恶性竞争。因此，SequoiaCM 在 v3.2.2 及以上版本提供了节点组功能，通过分组的方式实现不同业务间的资源隔离。

##访问模式##

根据不同的业务需求，节点组支持严格访问和亲和访问两种模式。

###严格访问模式###

严格访问模式下，上游服务仅访问同一 zone 内同一分组下的节点。未匹配到可用节点时，对应业务操作将失败。服务调用示意图如下：

![严格模式][along_group]

节点组 group1 和 group2 均为严格访问模式。在 zone2 内，网关服务节点无法调用同组下的内容服务节点，对应的业务操作将失败。

###亲和访问模式###

亲和访问模式下，上游服务优先访问同一 zone 内同一分组下的节点。未匹配到可用节点时，将访问其他分组或其他 zone 内的节点。服务调用示意图如下：

![亲和模式1][across_group1]

节点组 group2 为亲和访问模式，当同组的内容服务节点不可用时，将调用 group1 下的节点。如果 group1 下的节点仍不可用，则调用其他 zone 下的节点。服务调用示例图如下：

![亲和模式2][across_group2]

##适用场景##

###在线业务###

主要包括在线联机业务。该类业务对响应延时和性能的要求较高，必须保证网络和 CPU 资源的充足，例如“柜面无纸化”。

###后台业务###

主要包括后台跑批业务。该类业务对单次操作时延要求不高，但需要较大的吞吐量，例如“监管报批”、“影像批量归档”等。

##使用##

节点组相关的配置项及说明如下：

| 配置项 | 类型 | 说明 |
| ------ | ---- | ---- |
| eureka.instance.metadata-map.nodeGroup | string | 节点组名 |
| eureka.instance.metadata-map.groupAccessMode | string | 访问模式，取值如下：<br>along：严格访问模式<br>across：亲和访问模式 |

下述以内容服务节点 15000，节点目录 `/opt/sequoiacm/sequoiacm-content`为例，介绍节点组的配置步骤。

###自定义节点组###

1. 切换至节点目录

    ```lang-bash
    $ cd /opt/sequoiacm/sequoiacm-content
    ```
2. 创建节点组 group1，并设置为严格访问模式

    编辑配置文件

    ```lang-bash
    $ vi conf/content-server/15000/application.properties
    ```

    修改如下配置：

    ```lang-ini
    eureka.instance.metadata-map.nodeGroup=group1
    eureka.instance.metadata-map.groupAccessMode=along
    ```

3. 重启节点使配置生效

    ```lang-bash
    $ ./bin/scmctl.sh stop -p 15000
    $ ./bin/scmctl.sh start -p 15000
    ```

###内置节点组###

SequoiaCM 为在线和后台类型的业务提供了内置分组，用户仅需配置对应的节点组名即可使用该分组。具体配置说明如下：

| 分组名 | 说明 | 对应节点配置 |
| ------ |----- | ------------ |
| online | 适用于实时在线业务<br>该分组默认为亲和访问模式 | eureka.instance.metadata-map.nodeGroup=online |
| batch  | 适用于后台跑批业务<br>该分组默认为严格访问模式 | eureka.instance.metadata-map.nodeGroup=batch |

下述以在线类型的业务为例，创建内置节点组。

1. 切换至节点目录

    ```lang-bash
    $ cd /opt/sequoiacm/sequoiacm-content
    ```

2. 创建内置节点组 online

    编辑配置文件

    ```lang-bash
    $ vi conf/content-server/15000/application.properties
    ```

    修改如下配置：

    ```lang-ini
    eureka.instance.metadata-map.nodeGroup=online
    ```

3. 重启节点使配置生效

    ```lang-bash
    $ ./bin/scmctl.sh stop -p 15000
    $ ./bin/scmctl.sh start -p 15000
    ```

[along_group]:Architecture/along_group.png
[across_group1]:Architecture/across_group1.png
[across_group2]:Architecture/across_group2.png