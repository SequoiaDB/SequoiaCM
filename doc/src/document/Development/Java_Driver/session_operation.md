这里介绍如何使用 Java 驱动接口编写使用会话功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。
    
更多查看 [Java API][java_api]。

##示例##
* 创建 Session

```lang-javascript
// 配置每个机房的网关地址
ScmUrlConfig url = ScmUrlConfig.custom()
	.addUrl("DefaultRegion","zone1",Arrays.asList("scmserver1:8080/rootsite")) // 注意站点名小写
	.addUrl("DefaultRegion","zone2",Arrays.asList("scmserver2:8080/rootsite"))
	.build();
// 创建 session 配置类，支持配置业务程序优先连接的机房，示例优先连接 zone1 机房下的服务节点
ScmConfigOption conf = new ScmConfigOption(url, "DefaultRegion", "zone1", "admin", "admin",ScmRequestConfig.custom().build());
// 创建 session
ScmSession session = null;
try {
	session = ScmFactory.Session.createSession(conf);
	// 使用 session 进行业务操作......
}
finally {
	if (session != null) {
		session.close();
	}
}
```
>  **Note：**
>
>  * region 参数的默认值为 DefaultRegion ，目前通过 SequoiaCM 工具部署的集群仅有该 region ，暂时不支持部署多个 region
>
>  * zone 参数代表部署 SequoiaCM 时规划的机房名
[java_api]:api/java/html/index.html