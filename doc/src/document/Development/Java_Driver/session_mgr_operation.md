这里介绍如何使用 Java 驱动接口编写使用 Session  池的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。
    
更多查看 [Java API][java_api]。

##示例##

```lang-javascript
ScmConfigOption config = new ScmConfigOption("scmserver:8080/sitename", "region1",
        "zone1", "admin", "admin", null);
// 创建 SessionMgr 实例，并指定同步最新网关地址列表的间隔：5min
ScmSessionMgr sessionMgr = ScmFactory.Session.createSessionMgr(config, 5 * 60 * 1000);
ScmSession session = null;
try {
    session = sessionMgr.getSession(SessionType.AUTH_SESSION);
    // ..使用session进行业务操作
}
finally {
    if (session != null) {
        // 释放session
        session.close();
    }
    // 释放sessionMgr
    sessionMgr.close();
}
```
>  **Note:**
>
>  * ScmSessionMgr 主要提供了定时同步 SequoiaCM 集群中网关地址列表的能力，ScmSessionMgr 可以通过周期查询感知新的网关实例，并在随后的 getSession 操作中，创建绑定到新网关实例的 Session。
>   
>  * ScmSessionMgr 同步网关地址的周期时间不能小于1s。

[java_api]:api/java/html/index.html
