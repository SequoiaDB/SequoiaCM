这里介绍如何使用 Java 驱动接口编写使用 Session  池的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。
    
更多查看 [Java API][java_api]。

##示例##

```lang-javascript
ScmConfigOption config = new ScmConfigOption("scmserver:8080/sitename", "DefaultRegion", "zone1",
                "admin", "admin", null);
// 创建会话池配置类
ScmSessionPoolConf sessionPoolConf = ScmSessionPoolConf.builder()
        .setSessionConfig(config)
        .setMaxCacheSize(100) // 设置会话池缓存 session 的数量
        .setMaxConnections(200)  // 设置访问 SequoiaCM 集群的最大连接数，一般为应用系统的最大并发数
        .setKeepAliveTime(1200)  // 设置 session 的有效期，单位：秒，需要小于 auth-server 配置项 scm.session.maxInactiveInterval 指定的值
        .setSynGatewayUrlsInterval(30 * 1000) // 设置从注册中心同步最新网关地址的间隔，单位：毫秒，不设置时默认关闭同步功能
        .setCheckGatewayUrlsInterval(10 * 1000) // 设置检查网关地址健康状态的间隔，单位：毫秒
        .setClearAbnormalSessionInterval(120 * 1000) // 设置清理不可用 session 的间隔，单位：毫秒
        // 设置节点组
        //.setNodeGroup("group1") 
        // 设置节点组对应的访问模式，如果是内置节点组，无需指定该配置
        //.setGroupAccessMode(ScmType.NodeGroupAccessMode.ACROSS)
        .get();
ScmSessionMgr sessionMgr = ScmFactory.Session.createSessionMgr(sessionPoolConf);
ScmSession session = null;
try {
    session = sessionMgr.getSession();
    // ..使用session进行业务操作
}
finally {
    if (session != null) {
        // session 回池
        session.close();
    }
}
//  关闭 sessionMgr
//  sessionMgr.close();
```

>  **Note:**
>
> * ScmSessionMgr 提供了定时同步 SequoiaCM 集群中网关地址列表的能力，ScmSessionMgr 可以通过周期查询感知新的网关实例，并在随后的 getSession 操作中，创建绑定到新网关实例的 Session（此功能默认关闭，可通过设置网关节点同步周期参数 synGatewayUrlsInterval 为一个大于 0 的值启用该功能）。
> * ScmSessionMgr 一般作为单例使用，应用系统中创建一个 ScmSessionMgr 对象即可。
> * MaxConnections 参数限制了会话池中的 session 访问 SequoiaCM 集群的最大并发数量，请根据应用系统实际的并发数指定。
> * 如果配置了[节点组][node_group]，会话池将在创建会话时匹配对应的网关节点。

[java_api]:api/java/html/index.html
[node_group]:Architecture/node_group.md
