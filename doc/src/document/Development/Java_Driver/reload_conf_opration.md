这里介绍如何使用 Java 驱动接口编写使用动态刷新配置功能的程序。主要用于在运行时动态修改节点配置。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。

更多查看 [Java API][java_api]。

##示例##

* 修改配置

```lang-javascript
ScmSession session  = ScmFactory.Session
        .createSession(new ScmConfigOption("scmserver:8080", "admin", "admin"));

// 构造刷新配置的参数：给 schedule-server 及 auth-server 节点增加 scm.audit.userType.LOCAL=ALL 配置项，
// 并删除 scm.audit.userType.TOKEN 配置项
ScmConfigProperties conf = ScmConfigProperties.builder().service("schedule-server", "auth-server")
        .updateProperty("scm.audit.userType.LOCAL", "ALL")
        .deleteProperty("scm.audit.userType.TOKEN").build();

// 执行刷新
ScmUpdateConfResultSet ret = ScmSystem.Configuration.setConfigProperties(session, conf);

// 检查刷新结果
System.out.println(ret.getFailures());
System.out.println(ret.getSuccesses());
```

>  **Note:**
>
>  * 动态修改配置的接口只允许拥有管理员角色的用户访问
>
>  * 目前允许修改审计、慢操作告警相关的参数，禁止修改其它 scm 配置项（'scm.*'），禁止修改节点注册相关的配置项（包括服务名、端口号、eureka等配置）

>  * 上述规则以外的未识别配置项默认拒绝修改，可以通过 ScmConfigProperties.Builder.acceptUnknownProperties() 接口调整该行为
 
>  * 动态修改配置操作会在节点的配置文件目录下产生修改前的配置文件备份


[java_api]:api/java/html/index.html