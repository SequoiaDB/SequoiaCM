这里介绍如何使用 Java 驱动接口编写使用会话功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。
    
更多查看 [Java API][java_api]。

##示例##
* 创建 Session

```lang-javascript
// 站点列表
ArrayList<String> urlList = new ArrayList<String>();
urlList.add("scmserver:8080/rootsite"); // 注意站点名小写
// 创建session配置类
ScmConfigOption conf = new ScmConfigOption(urlList, "admin", "admin");
ScmSession session = null;
try {
    session = ScmFactory.Session.createSession(conf);
    // 使用session进行业务操作......
}
finally {
    if (session != null) {
        session.close();
    }
}
```

[java_api]:api/java/html/index.html