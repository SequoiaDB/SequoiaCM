### Actuator 介绍

Actuator 是 springboot 提供的用来对应用系统进行自省和监控的组件，运维人员可以通过 Actuator 暴露的端点实时监控 SequoiaCM 节点的自身状况，Actuator 提供了以下端点：

| **序号** | **端点**     | **作用**                                                     |
| -------- | ------------ | ------------------------------------------------------------ |
| 1        | /health      | 获取节点健康状态                                             |
| 2        | /env         | 获取节点环境及配置信息                                       |
| 3        | /metrics     | 报告各种应用程序度量信息，比如内存用量和 HTTP 请求计算       |
| 4        | /autoconfig  | 自动配置报告，记录哪些自动配置条件是否通过                   |
| 5        | /configprops | 描述配置属性（包括默认值）如何注入的                         |
| 6        | /beans       | 描述上下文所有 bean，以及它们之间的关系                      |
| 7        | /dump        | 获取线程活动快照                                             |
| 8        | /info        | 获取应用程序定制信息，这些信息由 info 打头的属性提供         |
| 9        | /mappings    | 描述全部 URL 路径，及它们和控制器（包括 Actuator 端点）的映射关系 |
| 10       | /trace       | 提供基本的 HTTP 请求跟踪信息，时间戳，HTTP 头等              |
| 11       | /refresh     | 刷新节点配置                                                 |
| 12       | /loggers     | 查看日志级别                                                 |

### Actuator 安全配置项

可通过以下配置项开启 Actuator 的权限控制，开启后，Actuator 端点将只能由认证后的 SequoiaCM 用户访问。

| **配置项**                  | **类型** | **备注**                                     |
| --------------------------- | -------- | -------------------------------------------- |
| management.security.enabled | boolean  | 是否开启 Actuator 端点的权限控制，默认值：false |

### 开启 Actuator 安全配置

1. 编辑各服务节点配置文件：<节点安装目录>/application.properties ，加入以下配置：

    ```
    management.security.enabled=true
    ```

2. 重启节点

