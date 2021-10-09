这里介绍如何使用 Java 驱动接口编写使用异步任务调度功能的程序。主要用于灵活地配置数据在数据源上的存活周期和迁移周期。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。

更多查看 [Java API][java_api]。

##示例##
* 创建调度

```lang-javascript
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));

// 创建迁移调度
// 文件迁移条件
BSONObject copyCondition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR)
        .is("zhangsan").get();
// 迁移调度内容：源站点、目标站点、文件内容最大停留时间、调度文件查询条件、范围、每次调度任务触发时最长执行时间 (ms)
ScmScheduleContent copyContent = new ScmScheduleCopyFileContent("branchSite1", "rootSite",
        "0d", copyCondition, ScopeType.SCOPE_CURRENT, 36000000);
ScmScheduleBuilder scheduleBuilder = ScmSystem.Schedule.scheduleBuilder(session);
ScmSchedule copySchedule = scheduleBuilder
        // 调度任务的工作区、名字及描述
        .workspace("test_ws").name("迁移文件").description("一个迁移的调度")
        // 调度类型，及内容
        .type(ScheduleType.COPY_FILE).content(copyContent)
        // 触发周期，cron 表达式
        .cron("0 0 0 12,24 * ?")
        // 触发时，优先在哪个 region、zone 下的内容服务节点执行
        .preferredRegion("DefaultRegion").preferredZone("zone1").build();


// 创建清理调度
// 文件清理条件
BSONObject cleancondition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is("lisi")
        .get();
// 清理调度内容：清理站点、文件内容最大停留时间、调度文件查询条件、范围、每次调度任务触发时最长执行时间 (ms)
ScmScheduleContent cleanContent = new ScmScheduleCleanFileContent("branchSite1", "3d",
        cleancondition, ScopeType.SCOPE_CURRENT, 36000000);
ScmScheduleBuilder scheduleBuilder = ScmSystem.Schedule.scheduleBuilder(ss);
ScmSchedule cleanSchedule = scheduleBuilder
        .workspace("test_ws").name("清理文件").description("一个清理的调度")
        .type(ScheduleType.CLEAN_FILE).content(cleanContent)
        .cron("0 0 0 12,24 * ?")
        .preferredRegion("DefaultRegion").preferredZone("zone1").build();

```
>  **Note:**
>
>  * ScheduleType 调度类型分为 COPY_FILE (迁移调度) 和 CLEAN_FILE (清理调度)
>
>  * 迁移调度 对符合调度条件的工作区内文件内容，按 cron 表达式表示的时间或周期，从源站点拷贝到目标站点
>
>  * 清理调度 对符合调度条件的工作区内文件内容，按 cron 表达式表示的时间或周期进行清理
>
>  * 不同站点模型对迁移/清理调度的约束详见[站点间网络模型][site_network]
>
>  * 文件查询条件 （BSONObject 对象）的属性，必须要是使用基本类型或能被序列化的对象类型
>
>  * 文件最大停留时间 表示形式为数字+d ，eg："3d" 表示调度站点文件内容存在时间要超过3天才能被调度
>
>  * cron 表达式 "0 0 0 12,24 * ?" 表示每个月 12 和 24 日的 00:00:00 进行迁移任务调度，具体请查看下方描述
>
>  * 一次调度任务最大超时时间 单位 ms，参数不写默认为 0，如果该参数 <=0 ,则表示不设超时时间；该参数表示每一次执行调度的最大执行时间，如果超出该时间此次调度停止
>
>  * preferredRegion、preferredZone 表示调度任务触发时优先在哪个机房上执行，目前版本 SequoiaCM 只有一个 Region 即 DefaultRegion，而 Zone 是用户部署集群时自由规划的，业务可以根据实际情况填写某一个 Zone

* 调度列表

```lang-javascript
ScmCursor<ScmScheduleBasicInfo> cursor = ScmSystem.Schedule.list(session,
        new BasicBSONObject());
while (cursor.hasNext()) {
    System.out.println(cursor.getNext());
}
cursor.close();
```

* 获取调度

```lang-javascript
ScmSchedule schedule = ScmSystem.Schedule.get(session, scheduleID);
System.out.println(schedule.getContent().toBSONObject());
```

* 更新调度

```lang-javascript
ScmSchedule schedule = ScmSystem.Schedule.get(session, scheduleID);
// 修改调度执行周期
// cron 表示：2018-2019 年 每个月的第三周星期六的 23:59:59 启动一次任务
schedule.updateCron("59 59 23 ? * 7#3 2018-2019");
```

* 禁用和启用调度

```lang-javascript
ScmSchedule schedule = ScmSystem.Schedule.get(session, scheduleID);
//禁用
schedule.disable();
//启用
schedule.enable();
```

* 删除调度

```lang-javascript
// 获取到调度,再删除
ScmSchedule schedule = ScmSystem.Schedule.get(session, scheduleID);
schedule.delete();
// 直接通过 id 删除
ScmSystem.Schedule.delete(session, scheduleID);
```

###cron###
cron 表达式在任务调度中用于设定一个调度任务的执行时间或执行周期

* 表达式格式

{秒} {分钟} {小时} {日期} {月份} {星期} {年份(可为空)}
>  **Note:**
>
>  * 日期与星期互斥，即不能同时存在，不存在位置用 ? 表示占位符

* 字段值表

|字段       |允许值        |允许的特殊字符   |
|----------|--------------|---------------|
|秒         |0-59         |, - * /         |
|分钟       |0-59         |, - * /         |
|小时       |0-23         |, - * /         |
|日期       |1-31         |, - * / ？ L W C|
|月份       |1-12 或 JAN-DEC|, - * /         |
|星期       |1-7 或 SUN-SAT |, - * / ？ L C #|
|年份       |空，1970-2099 |, -  * /       |
>  **Note:**
>  
>  * 星期 从星期日开始计算，即 1 表示星期天 
> 
>  * cron 表达式对特殊字符大小写不敏感

* 特殊字符含义

|特殊字符|含义                      |举例说明                              |
|-------|---------------------------|------------------------------------- |
|"," |表示字段的枚举值              |eg："1,30 * * * * ?" 表示每分钟的 1、30 秒|
|"-" |表示字段的范围值              |eg："* 29-31 * * * ?" 表示每个小时的 29-31 分钟的每秒|
|"\*"|表示字段域的任意值            |eg："59 59 23 31 12 ? *" 表示每年的 12月31日 的 23:59:59 |
|"/" |表示字段时间增量              |eg："0/20 * * * * ?" 表示每分钟的 0 、20、40 秒|
|"?" |表示日期或星期的占位符        |                                     |
|"L" |表示日期或星期域的最后值      |eg："* * * ? * 7L" 表示每个月的最后一个星期六的每秒；eg："* * * 6L * ？" 表示每个月倒数第 6 日的每秒|
|"W" |表示离日期最近的工作日        |eg："* * * 15W * ?" 如果 15 是周六，则表示最近工作日的周五的每秒；   如果 15 是周日，则表示最近工作日的周一的每秒|
|"C" |表示关联日历某日或星期几|eg："* * * 5C * ?" 表示关联日历中每个月第5日的每秒；eg："* * * ? * 1C" 表示关联日历中每个月每个星期日的每秒|
|"#" |表示月份的第几周的星期几      |eg："* * * ? * 5#3" 表示每个月第 3 周的星期四的每秒|
>  **Note:**
>    
>  * 工作日 指周一至周五，"W" 所指的工作日不能跨月份，如 "1W" 为周六，最近的工作日为周一，而不是上月周五
>
>  * "L" 之前的数字省略表示，每周最后一天或每月的最后一天
>
>  * "C" 如果没有关联日历，则包含是所有年份的日历

[site_network]:Architecture/site_network.md
[java_api]:api/java/html/index.html