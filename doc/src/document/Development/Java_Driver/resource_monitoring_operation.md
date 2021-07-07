这里介绍如何使用 Java 驱动接口编写使用资源监控功能的程序。主要用于节点主机、节点服务、工作区流量、系统会话、网关响应资源。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。

更多查看 [Java API][java_api]。

##示例##
* 监控主机CPU、内存

```lang-javascript
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
//所有节点主机CPU、memory监控
ScmCursor<ScmHostInfo> hostCursor = ScmSystem.Monitor.listHostInfo(session);
while (hostCursor.hasNext()) {
    ScmHostInfo hostInfo = hostCursor.getNext();
    System.out.println("listHost: name=" + hostInfo.getHostName() 
            + ", idleCpuTime=" + hostInfo.getCpuIdle()
            + ", systemCpuTime=" + hostInfo.getCpuSys()
            + ", userCpuTime=" + hostInfo.getCpuUser()
            + ", otherCpuTime=" + hostInfo.getCpuOther() 
            + ", freeRam=" + hostInfo.getFreeRam() 
            + ", freeSwap=" + hostInfo.getFreeSwap());
}
hostCursor.close();
```

>  **Note:**
>
>  * 时间单位 ms（毫秒）
>
>  * 内存单位 Byte（字节）


* 监控节点服务状态

```lang-javascript
//null 表示所有节点服务，也可以填服务名称
ScmCursor<ScmHealth> healthCursor=ScmSystem.Monitor.listHealth(session, null);
while(healthCursor.hasNext()) {
    ScmHealth health=healthCursor.getNext();
    System.out.println("listHealth: nodeName="+ health.getNodeName()
            + ", serviceName=" +health.getServiceName() 
            + ", status="+ health.getStatus());
}
healthCursor.close();
```


* 监控网关响应

```lang-javascript
ScmCursor<ScmGaugeResponse> gaugeResponseCursor = ScmSystem.Monitor.gaugeResponse(session);
while (gaugeResponseCursor.hasNext()) {
    ScmGaugeResponse gaugeResponse = gaugeResponseCursor.getNext();
    System.out.println("listReponse: nodeName=" + gaugeResponse.getNodeName()
            + ", serviceName=" + gaugeResponse.getServiceName() 
            + ", responseCount=" + gaugeResponse.getCount() 
            + ", responseTime="+ gaugeResponse.getResponseTime());
}
gaugeResponseCursor.close();
```

>  **Note:**
>
>  * 时间单位 ms（毫秒）


* 监控工作区的上传下载流量

```lang-javascript
ScmCursor<ScmFlow> flowCursor = ScmSystem.Monitor.showFlow(session);
while (flowCursor.hasNext()) {
    ScmFlow flow = flowCursor.getNext();
    System.out.println("listFlow: workSpace=" + flow.getWorkspaceName() 
            + ", uploadFlow=" + flow.getUploadFlow() 
            + ", loadFlow=" + flow.getDownloadFlow());
}
flowCursor.close();
```

>  **Note:**
>
>  * 内存单位 kb（千字节）


* 监控会话

```lang-javascript
long sessionCount = ScmFactory.Session.countSessions(session);
System.out.println("countSession：" + sessionCount);
ScmCursor<ScmSessionInfo> sessionCursor = ScmFactory.Session.listSessions(session);
SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
while (sessionCursor.hasNext()) {
    ScmSessionInfo sessionInfo = sessionCursor.getNext();
    System.out.println("listSession: id=" + sessionInfo.getSessionId() 
            + ", username=" + sessionInfo.getUsername()
            + ", createTime="+ format.format(sessionInfo.getCreationTime()));
}
sessionCursor.close();
```


* 工作区级别文件业务统计

```lang-javascript
// 手动刷新 test_ws 工作区文件访问量
ScmSystem.Statistics.refresh(session, StatisticsType.TRAFFIC, "test_ws");
// 文件访问量统计
ScmCursor<ScmStatisticsTraffic> trafficCursor = ScmSystem.Statistics.listTraffic(session,
        ScmQueryBuilder.start().put(ScmAttributeName.FileDelta.WORKSPACE_NAME).is("test_ws")
                .get());
while (trafficCursor.hasNext()) {
    ScmStatisticsTraffic traffic = trafficCursor.getNext();
    System.out.println("listTraffic: workspaceName=" + traffic.getWorkspaceName() 
             + ", trafficCount=" + traffic.getTraffic()
             + ", trafficType=" + traffic.getType()
             + ", recordTime=" + traffic.getRecordTime());
}
trafficCursor.close();

// 手动刷新 test_ws 工作区文件增量
ScmSystem.Statistics.refresh(session, StatisticsType.FILE_DELTA, "test_ws");
// 文件增量统计
ScmCursor<ScmStatisticsFileDelta> fileDeltaCursor = ScmSystem.Statistics
        .listFileDelta(session, ScmQueryBuilder.start()
                .put(ScmAttributeName.FileDelta.WORKSPACE_NAME).is("test_ws").get());
while (fileDeltaCursor.hasNext()) {
    ScmStatisticsFileDelta fileDelta = fileDeltaCursor.getNext();
    System.out.println("listFileDelta: workspaceName=" + fileDelta.getWorkspaceName()
            + ", deltaCount=" + fileDelta.getCountDelta() 
            + ", deltaSize=" + fileDelta.getSizeDelta() 
            + ", recordTime=" + fileDelta.getRecordTime());
}
fileDeltaCursor.close();



```

>  **Note:**
>
>  * StatisticsType 统计类型 TRAFFIC（访问量）、FILE_DELTA（文件增量）
>
>  * TRAFFIC（访问量）表示工作区周期内的文件上传总数和文件下载总数
>
>  * FILE_DELTA（文件增量）表示工作区周期内的文件上传的总数和总大小
>
>  * 业务统计按 [监控服务][monitoring_service] 配置的统计周期和执行时间进行记录
>
>  * refresh 接口 手动刷新此次周期开始到刷新时的统计记录

* 用户级别文件业务统计

```lang-javascript
ScmFileStatistician s = ScmSystem.Statistics.fileStatistician(session);
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
Date begin = sdf.parse("2021-01-01");
Date end = sdf.parse("2021-01-03");

// 查询 user1 在 2021-01-01 至 2021-01-03 （不包含 2021-01-03）时间段内工作区 workspace1 下的文件上传统计信息
ScmFileStatisticInfo data = s.upload().beginDate(begin).endDate(end).user("user1")
        .workspace("wokspace1").timeAccuracy(ScmTimeAccuracy.DAY).get();
```

>  **Note:**
>
>  * 监控服务默认不进行用户级别的文件统计，需要手动配置后才能获得上述指标，具体介绍见[监控服务功能章节][admin_server]



[java_api]:api/java/html/index.html
[monitoring_service]:Maintainance/Node_Config/cloud.md
[admin_server]:Architecture/Microservice/admin_service.md
