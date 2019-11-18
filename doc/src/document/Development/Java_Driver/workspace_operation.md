这里介绍如何使用 Java 驱动接口编写使用工作区功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。 

   
更多查看 [Java API][java_api]。

##示例##
* 创建工作区

```lang-javascript
// 创建 session
ScmSession session = ScmFactory.Session
        .createSession(new ScmConfigOption("scmserver:8080/rootsite", "admin", "admin"));
// 构建工作区配置
ScmWorkspaceConf conf = new ScmWorkspaceConf();
conf.setName("test_ws");
conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", "meta_domain"));
conf.addDataLocation(new ScmSdbDataLocation("rootSite", "data_domain"));
// 创建工作区
ScmWorkspace workspace = ScmFactory.Workspace.createWorkspace(session, conf);
```

* 获取工作区

```lang-javascript
// 获取一个名为 test_ws 的工作区
ScmWorkspace workspace1 = ScmFactory.Workspace.getWorkspace("test_ws", session);
```

* 更新工作区

```lang-javascript
// 更新工作区描述
workspace1.updatedDescription("test workspace");
```

* 删除工作区

```lang-javascript
// 删除工作区，并清空数据
ScmFactory.Workspace.deleteWorkspace(session, "test_ws", true);
```

[java_api]:api/java/html/index.html