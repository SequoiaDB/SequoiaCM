
本文档主要介绍通过 Java 驱动接口编写与[生命周期管理][lifecycle_manage]功能相关的程序示例。下述示例仅供参考，详细使用方法可查看 [Java API][java_api]。

##示例##

###设置生命周期规则配置###

SequoiaCM 支持通过 Java 驱动或导入 xml 文件的方式配置生命周期管理规则，目前导入 xml 文件的方式仅支持在第一次配置时使用。下述以 SequoiaCM 安装目录为 `/opt/sequoiacm`、导入 xml 文件的方式介绍生命周期管理的配置和使用，具体步骤如下：

```lang-javascript
    # 1.切换至 SequoiaCM 安装目录
    $ cd /opt/sequoiacm
   
    # 2. 创建 xml 文件
    $ touch lifecycle.xml
   
    # 3. 编辑配置文件
    $ vi lifecycle.xml
   
    # 根据实际需求进行配置
    <LifeCycleConfiguration>
        <!--自定义阶段标签配置-->
        <StageTagConfiguration>
            <!--Name：阶段标签的名称，Desc：阶段标签的描述信息。-->
            <StageTag Name="Temp1" Desc="临时存储1"/>
            <StageTag Name="Temp2" Desc="临时存储2"/>
        </StageTagConfiguration>
    
        <!--数据流配置-->
        <TransitionConfiguration>
            <!--Name：数据流名称-->
            <Transition Name="HotWarm">
                <!--数据流向。Source：起始阶段标签，Dest：目标阶段标签-->
                <Flow Source="Hot" Dest="Warm"/>
                <!--迁移任务配置-->
                <!--任务条件。Mode：模式（取值为 ALL 表示满足所有条件，ANY 表示满足任一条件），MaxExecTime：最大执行时长，Rule：触发周期-->
                <TransitionTriggers Mode="ALL" MaxExecTime='7200' Rule="0 0/2 * * * ?">
                    <!--触发条件，至少配置一条。Mode：模式（取值为 ALL 表示匹配所有条件，ANY 表示匹配任一条件）CreateTime：文件最长存在时间，LastAccessTime：文件的最近访问时间，BuildTime：文件的最长停留时间-->
                    <Trigger ID="1" Mode="ANY" CreateTime="30d" LastAccessTime="3d" BuildTime="30d"/>
                    <Trigger ID="2" Mode="ALL" CreateTime="30d" LastAccessTime="3d" BuildTime="30d"/>
                </TransitionTriggers>
    
                <!--清理任务配置-->
                <!--任务条件。Mode：模式（取值为 ALL 表示满足所有条件，ANY 表示满足任一条件），MaxExecTime：最大执行时长，Rule：触发周期-->
                <CleanTriggers Mode="ANY" MaxExecTime='7200' Rule="0 0/2 * * * ?">
                    <!--触发条件，至少配置一条。Mode：模式（取值为 ALL 表示匹配所有条件，ANY 表示匹配任一条件）TransitionTime：文件延迟清理时间，LastAccessTime：文件的最近访问时间-->
                    <Trigger ID="1" Mode="ALL" TransitionTime="30d" LastAccessTime="3d"/>
                    <Trigger ID="2" Mode="ANY" TransitionTime="30d" LastAccessTime="3d"/>
                </CleanTriggers>
    
                <!--文件匹配条件，非必配，需填写 json 格式-->
                <Matcher>{}</Matcher>
    
                <!--额外配置信息-->
                <!--QuickStart：是否开启快速启动，RecycleSpace：是否回收空间，DataCheckLevel：数据校验级别（取值为 strict 表示强校验，week 表示弱校验），Scope：文件查询范围（取值为 ALL 表示所有版本，CURRENT 表示当前版本，HISTORY 表示历史版本）-->
                <ExtraContent QuickStart='false' RecycleSpace='true' DataCheckLevel="strict" Scope="ALL"/>
            </Transition>
    
            <Transition Name="HotCold">
                <Flow Source="Hot" Dest="Cold"/>
                <TransitionTriggers Mode="ALL" MaxExecTime='7200' Rule="0 0/2 * * * ?">
                    <Trigger ID="1" Mode="ANY" CreateTime="30d" LastAccessTime="3d" BuildTime="30d"/>
                    <Trigger ID="2" Mode="ALL" CreateTime="30d" LastAccessTime="3d" BuildTime="30d"/>
                </TransitionTriggers>
                <Matcher>{"author": "test"}</Matcher>
                <ExtraContent QuickStart='true' RecycleSpace='true' DataCheckLevel="strict" Scope="CURRENT"/>
            </Transition>
        </TransitionConfiguration>
    </LifeCycleConfiguration>

    # 4. 根据实际情况编写生命周期管理相关的 Java 程序
	ScmSession session=ScmFactory.Session.createSession(
            new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
    InputStream in = null;
    // 导入 xml 文件
    try{
        in = new FileInputStream(<"E:\\opt\\sequoiacm\\lifecycle.xml");
        ScmSystem.LifeCycleConfig.setLifeCycleConfig(scmSession, in);
    }
    finally{
        if(in!=null){
            in.close();
        }
    }
    // 指定站点的名称和阶段标签
    ScmFactory.Site.setSiteStageTag(session, "rootSite", "Hot");
    ScmFactory.Site.setSiteStageTag(session, "branchSite1", "Cold");
    // 将工作区 test_ws 与数据流 HotWarm 关联
    ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);
    ScmTransitionSchedule scmTransitionSchedule = workspace.applyTransition("HotWarm", "DefaultRegion", "zone1");
```

   > **Note：**
   >
   > * 触发条件中的 ID 必须唯一。
   > * 配置中 CreateTime、LastAccessTime、BuildTime 和 TransitionTime 的取值均以 d 结尾，表示天数。
   > * 如果未配置清理任务，数据流仅生成 move_file 调度任务；如果配置了清理任务，将根据迁移任务条件生成 copy_file 调度任务，根据清理任务条件生成 clean_file 调度任务。

###阶段标签###

- 添加阶段标签

    ```lang-javascript
    //指定阶段标签的名称以及描述信息
    ScmSystem.LifeCycleConfig.addStageTag(session, "Temp1", "临时存储1");
    ```

  >**Note:**
  >
  > 阶段标签的名称不能为空，且必须唯一。

- 获取阶段标签列表

    ```lang-javascript
    ScmSystem.LifeCycleConfig.getStageTag(session);
    ```

- 删除阶段标签

    ```lang-javascript
    //指定阶段标签的名称
    ScmSystem.LifeCycleConfig.removeStageTag(session, "Temp1");
    ```

  >**Note:**
  >
  > 不支持删除系统阶段标签 Hot、Warm 和 Cold。

###数据流###

- 创建数据流

    ```lang-javascript
    // 定义迁移任务的规则:
    // 指定任务模式、最大执行时长、任务触发周期的 cron 表达式
    ScmTransitionTriggers.Builder transitionTriggersBuilder = new ScmTransitionTriggers.Builder("ANY", 7200, "0 0 0 12,24 * ?");
    // 配置任务的触发条件，需指定条件 id、模式、文件最长存在时间、文件的最近访问时间、文件的最长停留时间
    ScmTransitionTriggers transitionTriggers = transitionTriggersBuilder.addTrigger("1", "ALL", "3d", "2d", "4d").build();
    // 指定数据流名称、起始阶段标签、目标阶段标签、任务的触发条件
    ScmLifeCycleTransition transition=new ScmLifeCycleTransition("HotWarm", "Hot", "Warm", transitionTriggers);
    
    // 定义清理任务的规则：
    // 指定任务模式、最大执行时长、任务触发周期的 cron 表达式
    ScmCleanTriggers.Builder cleanTriggersBuilder = new ScmCleanTriggers.Builder("ANY", 7200, "0 0 0 12,24 * ?");
    // 配置任务的触发条件，需指定条件 id、模式、文件延迟清理时间、文件的最近访问时间
    ScmCleanTriggers cleanTriggers = cleanTriggersBuilder.addTrigger("1", "ANY", "3d", "3d").build();
    transition.setCleanTriggers(cleanTriggers);

    // 定义全局任务规则:
    // 指定数据校验级别
    transition.setDataCheckLevel("strict");
    // 指定文件查询范围
    transition.setScope("ALL");
    // 指定是否快速启动任务
    transition.setQuickStart(false);
    // 指定是否回收空闲空间
    transition.setRecycleSpace(true);
    
    //创建数据流
    ScmSystem.LifeCycleConfig.addTransition(session, transition);
    ```

- 更新数据流

    ```lang-javascript
    //指定数据流的名称以及需要更新的规则
    ScmLifeCycleTransition transition = ScmSystem.LifeCycleConfig.getTransition(session, "HotWarm");
    transition.setQuickStart(false);
    ScmSystem.LifeCycleConfig.updateTransition(session, "HotWarm", transition);
    ```

- 获取数据流列表

    ```lang-javascript
    ScmSystem.LifeCycleConfig.getTransitionConfig(session);
    ```

- 移除数据流

    ```lang-javascript
    // 指定要移除的数据流的名字
    ScmSystem.LifeCycleConfig.removeTransition(session, "HotWarm");
    ```

- 创建一次性数据流

    ```lang-javascript
    // 按匹配条件建立迁移任务：迁移源阶段标签站点所有 Author 为 SequoiaCM 的文件到目标阶段标签的站点
    BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is("SequoiaCM")
            .get();
    // 指定迁移类型、工作区、任务最长执行时间、匹配条件、源阶段标签、目标阶段标签、优先执行的地区 region、优先执行的机房 zone
    ScmOnceTransitionConfig config = new ScmOnceTransitionConfig("move_file", workspace, 7000,
            condition, "Hot", "Warm", "DEFAULT_REGION", "zone1");
    // 指定是否快速启动任务
    config.setQuickStart(false);
    // 指定数据校验级别
    config.setDataCheckLevel(ScmDataCheckLevel.STRICT);
    // 指定文件查询范围
    config.setScope(ScmType.ScopeType.SCOPE_CURRENT);
    // 指定是否回收空闲空间
    config.setRecycleSpace(true);
    ScmId scmId = ScmSystem.LifeCycleConfig.startOnceTransition(config);
    ```

  > **Note：**
  >
  > - 用户可通过实例 ScmTask 下的方法 getProgress() 查询任务进度。
  > - 用户可通过实例 ScmTask 下的方法 getRunningFlag() 查询任务状态，具体状态说明如下：
      >  - CommonDefine.TaskRunningFlag.SCM_TASK_INIT：初始化中
  >  - CommonDefine.TaskRunningFlag.SCM_TASK_RUNNING：执行中
  >  - CommonDefine.TaskRunningFlag.SCM_TASK_FINISH：已完成
  >  - CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL：已取消
  >  - CommonDefine.TaskRunningFlag.SCM_TASK_ABORT：已中止
  >  - CommonDefine.TaskRunningFlag.SCM_TASK_TIMEOUT：超时退出

###站点操作###

- 设置站点的阶段标签

    ```lang-javascript
    // 指定站点的名称和阶段标签
    ScmFactory.Site.setSiteStageTag(session, "rootSite", "Hot");
    ```

- 获取站点的阶段标签

    ```lang-javascript
    // 指定站点的名称
    ScmFactory.Site.getSiteStageTag(session, "rootSite");
    ```

- 修改站点的阶段标签

    ```lang-javascript
    // 指定站点的名称和阶段标签
    ScmFactory.Site.alterSiteStageTag(session, "rootSite", "Warm");
    ```

  >**Note:**
  >
  > 将同步更新使用该站点的数据流信息，仅对当前站点下的工作区生效。

- 移除站点的阶段标签

    ```lang-javascript
    // 指定站点的名称
    ScmFactory.Site.unsetSiteStageTag(session, "rootSite");
    ```

  >**Note:**
  >
  > 移除站点的阶段标签前，需保证该站点未被数据流使用。


###工作区操作###

- 关联数据流

    ```lang-javascript
    ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);
    // 方式一：直接关联数据流
    ScmTransitionSchedule scmTransitionSchedule = workspace.applyTransition("HotWarm", "DefaultRegion", "zone1");
    
    // 方式二：关联数据流后，自定义数据流规则
    ScmLifeCycleTransition transition = new ScmLifeCycleTransition();
    transition.setScope("CURRENT");
    // 指定数据流名称、数据流规则、优先执行的地区 region、优先执行的机房 zone
    ScmTransitionSchedule scmTransitionSchedule = workspace.applyTransition("HotWarm", transition, "DefaultRegion", "zone1");
    ```

  > **Note：**
  >
  > - 通过方式一关联数据流，当源数据流定义的规则发生变更，工作区将依据更新后的规则重新建立调度任务。
  > - 通过方式二自定义数据流，该数据流定义的规则仅对当前工作区生效。

- 获取已关联的数据流信息

    ```lang-javascript
    List<ScmTransitionSchedule> transitionScheduleList = workspace.listTransition();
    ```

- 获取指定数据流的信息

    ```lang-javascript
    //指定数据流名称
    ScmTransitionSchedule transitionSchedule = workspace.getTransition("HotWarm");
    ```

- 获取基于数据流生成的调度任务信息

    ```lang-javascript
    ScmTransitionSchedule transitionSchedule = workspace.getTransition("HotWarm");
    for(ScmId scheduleId : transitionSchedule.getScheduleIds()){
        ScmSchedule schedule = ScmSystem.Schedule.get(session, scheduleId);
        System.out.println(schedule.getContent().toBSONObject());
    }
    ```

- 启用或禁用数据流

    ```lang-javascript
    //指定数据流名称
    ScmTransitionSchedule transitionSchedule = workspace.getTransition("HotWarm");
    //启用
    transitionSchedule.enable();
    //禁用
    transitionSchedule.disable();
    ```

- 更新数据流

    ```lang-javascript
    //指定数据流名称
    ScmTransitionSchedule transitionSchedule = workspace.getTransition("HotWarm");
    ScmLifeCycleTransition transition = transitionSchedule.getTransition();
    //以更新quickStart为例：将quickStart设置为true
    transition.setQuickStart(true);
    workspace.updateTransition("HotWarm", transition);
    ```

  > **Note：**
  >
  > 数据流更新后仅对当前工作区生效，且工作区将依据更新后的规则重新建立调度任务。

- 移除数据流

    ```lang-javascript
    //指定数据流名称
    workspace.removeTransition("HotWarm");
    ```

  > **Note：**
  >
  > 数据流移除后，依据该数据流生成的调度任务将被删除。

[java_api]:api/java/html/index.html
[lifecycle_manage]:Architecture/lifecycle_manage.md