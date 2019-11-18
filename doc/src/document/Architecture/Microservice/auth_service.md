认证服务基于 Spring Security 和 Spring Session  实现，负责 SequoiaCM 集群的认证管理功能。认证服务中的各个节点是平级关系，对外提供完整的认证服务功能。

![认证服务][auth_service]


## 会话管理 ##

用户的登录以及后续请求的认证统一由认证服务负责处理。认证服务为 SequoiaCM 系统提供单点登录以及分布式会话管理的支持，从而用户只需登录一次，即可访问 SequoiaCM 系统内的任意服务。

## 资源权限控制 ##

在 SequoiaCM 系统中，支持对工作区、目录两个业务资源进行细致的权限控制，认证服务负责维护这两个业务资源的权限配置，具体的权限模型可以参考[权限模型章节][priority]。具备工作区、目录业务处理逻辑的内容服务节点和调度服务节点通过心跳消息定期向认证服务同步资源权限配置。

[auth_service]:Architecture/Microservice/auth_server.png
[priority]:Architecture/priority.md