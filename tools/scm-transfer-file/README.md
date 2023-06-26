使用示例：

    java -jar sequoiacm-transfer-file-3.2.1.jar   --fileMatcher '{}' --sdbCoord sdb:11810 --sdbUser sdbadmin --sdbPassword sequoiadb   --scmPassword admin  --siteId 2 --url 192.168.16.70:8080/targetSite --scmUser admin --workspace ws2 

    上述示例表示将工作区 ws2 中满足 fileMatcher 条件的文件，迁移到 targetSite 上
    
    工具每分钟打印一次当前执行的进度：
    INFO process info: successTransferCount=8736, timeoutCount=0
    
    successTransferCount 表示成功迁移的文件数，timeoutCount表示长时间没有迁移成功的文件数（可能是迁移失败了）


打印帮助信息：
java -jar sequoiacm-transfer-file-3.2.1.jar  --help

参数说明：

必填参数

    --url <arg>                       scm 网关地址和迁移目标站点名, 如迁移到 site2，则填写为：gatewayhost:port/site2 （站点名全小写）
    
    --targetSiteName <arg>            迁移目标站点名，如 site2

    --srcSiteName <arg>               迁移源站点名，如 site1，即表示从 site1 迁移到 site2
                                     
    --scmUser <arg>                   scm 系统用户名
    
    --scmPassword <arg>               scm 系统密码，可以不填在随后交互式输入
    
    --workspace <arg>                 迁移的工作区名
    
    --fileMatcher <arg>               文件匹配条件，可以按调度任务上的文件条件填
    
    --sdbCoord <arg>                  scm 元数据 sdb 地址
    --sdbPassword <arg>               scm 元数据 sdb 密码
    --sdbUser <arg>                   scm 元数据 sdb 用户名

非必填参数

    --batchSize <arg>                 批次大小，工具最多将会提交多少文件到内容服务上进行迁移处理（内容服务节点固定10条线程处理提交的文件），默认：100
                                      
    --fileStatusCheckInterval <arg>   每隔多长时间检查一次已提交文件的迁移状态，默认值：1000ms
    
    --fileTransferTimeout <arg>       文件多长时间未被迁移完成时，工具标记该文件为处理超时，工具不在等待该文件的处理结果，默认值：1800000ms
    
    --fileStatusCheckBatchSize <arg>  提交的文件数达到多少时，检查迁移状态，默认值 50

    --sdbConnectTimeout <arg>         sdb 连接超时，默认 10000ms

    --sdbSocketTimeout                sdb 连接读超时，默认 0ms，不超时
    
    --fileScope                       文件迁移范围，支持填写：ALL（最新文件版本和历史文件版本），CURRENT（最新文件版本），HISTORY（历史文件版本）

    --help                            打印帮助