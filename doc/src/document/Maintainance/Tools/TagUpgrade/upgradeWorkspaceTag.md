
upgradeWorkspaceTag 子命令提供工作区标签升级重构的能力，升级过程中业务将无法访问该工作区。

##子命令选项##
 
| 选项 | 缩写 |描述                                                                                    |是否必填|
| ---- | ---- |----------------------------------------------------------------------------------------|--------|
|--workspaces     | - | 指定待升级的工作区名称<br>如果需要指定多个工作区，可使用逗号（,）间隔|是|
|--tag-lib-domain | - | 指定标签库 Domain，需保证该 Domain 已存在|是|
|--thread         | - | 指定升级任务的线程数，默认值为 5|否|
|--mdsurl         | - | 指定元数据服务对应的 SequoiaDB 地址，格式为 `<sdbserver1>:<svcname1>,<sdbserver2>:<svcname2>,...`|是|
|--mdsuser        | - | 指定元数据服务对应的 SequoiaDB 用户名|是|
|--mdspasswd      | - | 指定元数据服务对应的 SequoiaDB 密码，取值为空时表示采用交互的方式输入密码|是|

##示例##

对工作区 ws1 和 ws2 执行标签升级操作

```lang-bash
$ scm-tag-upgrade.sh upgradeWorkspaceTag --workspaces ws1,ws2  --tag-lib-domain domain1  --mdsurl 192.168.31.11:11810 --mdsuser user --mdspasswd paswd 
```

执行成功后，将生成状态文件，字段 Status File Path 的值为文件所在路径：

```lang-text
Status File Path: /opt/sequoiacm/tools/sequoiacm-tag-upgrade/./status/status-041f2c65-94b0-4f48-951e-0eeed8380e8b
Processing workspace(1/1): ws_quarter
Refactoring user tag, TotalFiles: 1000, ProcessedFiles 1000, FailedFiles: 0, Progress: 100%
Refactoring empty custom tag...
All workspace processed successfully.
```

> **Note:** 
>
> 如果升级任务异常中断，用户可使用 [continueUpgradeTag][continueUpgradeTag] 子命令重新开启升级任务。

[continueUpgradeTag]:Maintainance/Tools/TagUpgrade/continueUpgradeTag.md