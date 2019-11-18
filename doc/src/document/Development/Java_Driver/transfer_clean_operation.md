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
```
>	**Note:**
>
>	*	清理任务是异步的，可以通过 ScmTask 实例获取任务状态, ScmSystem.getTask() 接口可以获取 ScmTask 实例。

[schedule_operation]:Development/Java_Driver/schedule_operation.md