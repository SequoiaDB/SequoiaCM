这里介绍如何使用 Java 驱动接口编写使用迁移、清理功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。
    
更多查看 [Java API](api/java/html/index.html) 。

>  **Note:**
>
>  * 支持调度管理的迁移清理任务示例请查看[任务调度操作][schedule_operation]

*	迁移任务

```lang-javascript 
//建立与分站点的会话，获取工作区实例
ScmSession session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
            new ScmConfigOption("scmserver:8080/branchsite1", "test_user", "scmPassword"));
ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);

//按匹配条件建立迁移任务：迁移分站点所有 Author 为 SequoiaCM 的文件到主站点
BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is("SequoiaCM").get();
ScmId taskID = ScmSystem.Task.startTransferTask(workspace, condition);

//如需进行更多配置，可通过 ScmTransferTaskConfig 启动调度任务
ScmTransferTaskConfig transferTaskConfig = new ScmTransferTaskConfig();
transferTaskConfig.setWorkspace(workspace);
transferTaskConfig.setTargetSite("rootSite");
transferTaskConfig.setCondition(condition);
transferTaskConfig.setMaxExecTime(36000000);
transferTaskConfig.setScope(ScmType.ScopeType.SCOPE_CURRENT);
// 设置是否开启快速启动，开启后，将不会计算任务的预估文件数
transferTaskConfig.setQuickStart(false);
// 设置数据校验级别（WEEK：弱校验，检查迁移后的文件内容大小；STRICT：强校验，检查迁移后的文件内容MD5）
transferTaskConfig.setDataCheckLevel(ScmDataCheckLevel.WEEK);
ScmId scmId = ScmSystem.Task.startTransferTask(transferTaskConfig);
```
>  **Note:**
>
>  * 迁移操作是异步的，可以通过 ScmTask 实例的 getRunningFlag 查询任务状态，getProgress 查询任务进度(返回10，表示10%)，ScmTask 实例可以通过 ScmSystem.getTask() 接口获取;
> 
> 任务状态 runningFlag 有如下六种:
> 
>  * CommonDefine.TaskRunningFlag.SCM_TASK_INIT:	初始化中;
> 
>  * CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING:	执行中;
> 
>  * CommonDefine.TaskRunningFlag.SCM_TASK_FINISH:	已完成;
> 
>  * CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL:	已取消;
> 
>  * CommonDefine.TaskRunningFlag.SCM_TASK_ABORT:	已中止;
> 
>  * CommonDefine.TaskRunningFlag.SCM_TASK_TIMEOUT:  超时退出;

*	清理任务

```lang-javascript 
// 清理分站点所有 Author 为 SequoiaCM 的文件
BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is("SequoiaCM")
        .get();
ScmSystem.Task.startCleanTask(workspace, condition);

//如需进行更多配置，可通过 ScmCleanTaskConfig 启动调度任务
ScmCleanTaskConfig cleanTaskConfig = new ScmCleanTaskConfig();
cleanTaskConfig.setCondition(condition);
cleanTaskConfig.setScope(ScmType.ScopeType.SCOPE_ALL);
cleanTaskConfig.setWorkspace(workspace);
cleanTaskConfig.setMaxExecTime(36000000);
// 设置数据校验级别（WEEK：弱校验，在清理本站点文件内容前，检查其它站的文件内容大小；STRICT：强校验，在清理本站点文件内容前，检查其它站的的文件内容MD5）
cleanTaskConfig.setDataCheckLevel(ScmDataCheckLevel.WEEK);
// 设置是否开启空间回收功能，目前仅支持SequoiaDB数据源，开启后，某个集合空间下的文件全部清理后会自动删除该集合空间
cleanTaskConfig.setRecycleSpace(true);
// 设置是否开启快速启动，开启后，将不会计算任务的预估文件数
cleanTaskConfig.setQuickStart(false);
ScmSystem.Task.startCleanTask(cleanTaskConfig);
```
>	**Note:**
>
>	*	清理任务是异步的，可以通过 ScmTask 实例获取任务状态, ScmSystem.getTask() 接口可以获取 ScmTask 实例。



*	MOVE_FILE任务

```lang-javascript 
// 移动分站点所有 Author 为 SequoiaCM 的文件到主站点
BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is("SequoiaCM")
        .get();
ScmMoveTaskConfig moveTaskConfig = new ScmMoveTaskConfig();
moveTaskConfig.setCondition(condition);
moveTaskConfig.setWorkspace(workspace);
moveTaskConfig.setTargetSite("rootSite");
moveTaskConfig.setScope(ScmType.ScopeType.SCOPE_CURRENT);
moveTaskConfig.setMaxExecTime(36000000);
// 设置是否开启快速启动，开启后，将不会计算任务的预估文件数
moveTaskConfig.setQuickStart(false);
// 设置是否开启空间回收功能，目前仅支持SequoiaDB数据源，开启后，某个集合空间下的文件内容全部移动后会自动删除该集合空间
moveTaskConfig.setRecycleSpace(true);
// 设置数据校验级别（WEEK：弱校验，检查移动后的文件内容大小；STRICT：强校验，检查移动后的文件内容MD5）
moveTaskConfig.setDataCheckLevel(ScmDataCheckLevel.WEEK);
ScmId scmId = ScmSystem.Task.startMoveTask(moveTaskConfig);
```

*	空间回收任务

```lang-javascript 
// 开启空间回收任务（目前仅支持 SequoiaDB 数据源），回收 SequoiaDB 中一个月前空的集合空间
ScmSpaceRecyclingTaskConfig spaceRecyclingTaskConfig = new ScmSpaceRecyclingTaskConfig();
spaceRecyclingTaskConfig.setWorkspace(workspace);
spaceRecyclingTaskConfig.setRecycleScope(ScmSpaceRecycleScope.mothBefore(1));
spaceRecyclingTaskConfig.setMaxExecTime(36000000);
ScmId scmId = ScmSystem.Task.startSpaceRecyclingTask(spaceRecyclingTaskConfig);
```

[schedule_operation]:Development/Java_Driver/schedule_operation.md