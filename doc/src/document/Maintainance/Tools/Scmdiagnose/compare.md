当 SCM 系统发生极端异常时（如底层数据源数据丢失、损坏），可能会导致出现数据源之间数据不一致或文件有元数据，没有数据的问题。

compare 子命令提供数据一致性检测功能，通过检测文件的数据大小和 md5 值，判断文件元数据与数据的一致性、多个数据源间的数据一致性。

##子命令选项##

| 选项          | 缩写 | 描述                                                                                         | 是否必填 |
| ------------- |------| -------------------------------------------------------------------------------------------- | -------- |
| --work-path   | -   | 指定检测结果的归档目录                  | 是 |
| --workspace   | -   | 指定待检测文件所属的工作区                                  | 是 |
| --begin-time  | -   | 指定时间区间的起始时间，格式为 `yyyyMMdd` | 是 |
| --end-time    | -   | 指定时间区间的结束时间，格式为 `yyyyMMdd`<br>系统仅对时间区间内创建的文件进行数据检测 | 是 |
| --url         | -   | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>/<siteName>`                                                   | 是    |
| --user        | -   | 指定管理员用户名                                                                                  | 是    |
| --passwd      | -   | 指定管理员用户密码，取值为空时表示采用交互的方式输入密码 | 是    |
| --check-level | -   | 指定文件的检测级别，默认值为 2，可选取值如下：<br>1：检测数据大小<br>2：检测数据大小和 md5 值，如果文件不存在 md5 值则仅检测数据大小 | 否    |
| --full         |  -  | 指定是否归档所有文件的检测结果，默认值为 false，表示仅归档数据不一致的文件 | 否    |
| --worker-count |  -  | 指定检测任务的最大并发数，默认值为 1，取值范围为 [1,100]<br>用户需根据业务环境设置该选项，避免因取值过大影响系统性能 | 否    |

>**Note:**
>
> 如果文件存在多个版本，检测过程将跳过存在 [Delete Marker 标识][bucket]的文件。

###示例###

1. 切换至工具所在路径，以 SequoiaCM 安装目录 `/opt/sequoiacm` 为例，执行语句如下：

    ```lang-bash
    $ cd /opt/sequoiacm/tools/sequoiacm-scm-diagnose
    ```

2. 对工作区 ws_default 下的文件执行数据检测

    ```lang-bash
    $ ./bin/scmdiagnose.sh compare --work-path /opt/datacheck --workspace ws_default --begin-time 20230101 --end-time 20230331 --url 192.168.17.183:8080/rootSite --user admin --passwd admin
    ```

   检测完成后，结果将保存在归档目录下

    ```lang-text
    ...
    [15:10:04] Finish! workspace: ws_default, process: 5006, fail: 0, same: 5006, different: 0, cost: 0 min 9 s 701 ms, the result in: /opt/datacheck/compare_result/2023-04-11-15-09-54/
    ```

[bucket]:Architecture/Business_Concept/bucket.md