这里介绍如何使用 Java 驱动接口编写使用链路追踪功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。 


更多查看 [Java API][java_api]。

##示例##
* 查看链路信息

```lang-javascript
// 创建 session
ScmSession session = ScmFactory.Session
        .createSession(new ScmConfigOption("scmserver:8080/rootsite", "admin", "admin"));

// 获取10条最新的链路信息
ScmSystem.ServiceTrace.listTrace(session, 10);

// 获取耗时超过 10000 微秒的链路信息
ScmSystem.ServiceTrace.listTrace(session, 10000L, 10);

// 查询途径认证服务，额外信息中 http.status_code 为 500 的链路信息：
Map<String, String> condition = new HashMap<String, String>();
condition.put("http.status_code", "500");
ScmSystem.ServiceTrace.listTrace(session, "auth-server", null, null, null, condition, 10);
```
>  **Note：**
>
>  * 使用该功能前请确保已经部署了链路追踪服务
>


[java_api]:api/java/html/index.html