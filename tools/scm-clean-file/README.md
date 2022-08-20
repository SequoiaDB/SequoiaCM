#清理SCM文件
使用示例：
java -Xmx2048m -jar sequoiacm-clean-file-3.2.1.jar scmFileClean  --workspace ws8 --fileMatcher '{}' --cleanSiteId 1 --queueSize 1000 --thread 50  --metaSdbCoord 192.168.30.82:11810,192.168.30.83:11810,192.168.30.84:11810 --metaSdbUser sdbadmin --metaSdbPassword sdbadmin --cleanSiteLobSdbCoord 192.168.30.82:11810,192.168.30.83:11810,192.168.30.84:11810  --cleanSiteLobSdbUser sdbadmin --cleanSiteLobSdbPassword sdbadmin --zkUrls 192.168.30.82:2181
参数说明：
    --cleanSiteId <arg>               需要清理的站点
    --cleanSiteLobSdbCoord <arg>      需要清理站点的sdb lob 存储地址
    --cleanSiteLobSdbPassword <arg>   需要清理站点的sdb lob 存储密码
    --cleanSiteLobSdbUser <arg>       需要清理站点的sdb lob 存储用户
    --fileMatcher <arg>               清理的文件匹配条件
    --metaSdbCoord <arg>              元数据sdb地址
    --metaSdbPassword <arg>           元数据sdb用户
    --metaSdbUser <arg>               元数据sdb密码
    --queueSize <arg>                 线程池队列大小
    --thread <arg>                    线程池线程数
    --workspace <arg>                 清理的工作区
    --zkUrls <arg>                    zookeeper 地址

#清理 Zookeeper 文件
java -Xmx2048m -jar sequoiacm-clean-file-3.2.1.jar zkClean --zkUrls 192.168.30.82:2181 --maxBuffer 6 --maxResidualTime 180000
参数说明：
    --maxBuffer <arg>         zk client buffer 大小，单位mb
    --maxResidualTime <arg>   zk 文件夹下子文件夹超过多少时，需要检测清理该文件夹
    --zkUrls <arg>            zkUrls


