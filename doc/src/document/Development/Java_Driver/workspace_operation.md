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
// 设置开启目录功能
conf.setEnableDirectory(true);
// 设置工作区文件站点缓存策略
conf.setSiteCacheStrategy(ScmSiteCacheStrategy.ALWAYS);
// 设置批次分区类型
conf.setBatchShardingType(ScmShardingType.MONTH);
// 设置批次ID时间正则表达式
// conf.setBatchIdTimeRegex(
//     "(?<=\\w{1,50}\\.[^.]{1,50}\\.)(\\d{4}-\\d{2}-\\d{2})(?=\\..{1,200})");
// 设置批次ID时间格式
// conf.setBatchIdTimePattern("yyyy-MM-dd");
// 设置批次内的文件名唯一
// conf.setBatchFileNameUnique(true);
// 创建工作区
ScmWorkspace workspace = ScmFactory.Workspace.createWorkspace(session, conf);
```
>  **Note：**
>
>  * 开/闭目录功能相关说明详见[目录][directory]
>
>  * 设置批次相关说明详见[批次][batch]
>
>  * 工作区缓存策略详见[工作区][workspace]

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
```lang-javascript
// 更新工作区中 rootSite 站点的属性，修改CS的分区规则为按月划分
ScmSdbDataLocation sdbDataLocation= new ScmSdbDataLocation("rootSite");
sdbDataLocation.setCsShardingType(ScmShardingType.MONTH);
List<ScmDataLocation> dataLocationList = new ArrayList<>();
dataLocationList.add(sdbDataLocation);
workspace1.updateDataLocation(dataLocationList);
```

* 删除工作区

```lang-javascript
// 删除工作区，并清空数据
ScmFactory.Workspace.deleteWorkspace(session, "test_ws", true);
```

[java_api]:api/java/html/index.html
[directory]:Architecture/Business_Concept/directory.md
[batch]:Architecture/Business_Concept/batch.md
[workspace]:Architecture/Business_Concept/workspace.md