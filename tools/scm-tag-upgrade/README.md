标签升级工具提供 SequoiaCM 标签升级重构的能力，用于处理 SequoiaCM 3.6.1 版本之前的工作区标签升级。由于 SequoiaCM 3.6.1 版本将文件标签进行了重构，引入了标签检索能力，若 3.6.1 之前创建的工作区希望使用新引入的标签检索能力，那么需要使用本工具进行处理。

##参数##

|参数      |缩写  |描述          |
|----------|------|--------------|
|--help    |-h    |获取帮助文档  |


##子命令列表##

| 子命令                        |描述            |
|----------------------------|----------------|
| upgradeWorkspaceTag |指定工作区，执行文件数据标签升级重构 |
| continueUpgradeTag    |当 upgradeWorkspaceTag 命令执行过程中异常中断时，可以通过这个子命令继续执行升级重构|


##upgradeWorkspaceTag##

指定工作区，执行文件数据标签升级重构，执行重构期间，业务访问工作区将会报错。

###参数###


|选项           |缩写 |描述                                                                                    |是否必填|
|---------------|-----|----------------------------------------------------------------------------------------|--------|
|--workspaces    | |指定要重构升级的工作区列表|是|
|--tag-lib-domain    | |指定标签库 domain，domain 需要已存在|是|
|--thread    | |重构工作区时的线程数，默认：5|否|
|--mdsurl    | |元数据服务 SequoiaDB 地址，如：host1:11810,host2:11810|是|
|--mdsuser    | |元数据服务 SequoiaDB 用户名|是|
|--mdspasswd    | |元数据服务 SequoiaDB 密码，不指定值表示交互式输入|是|


###示例###

```lang-javascript
scm-tag-upgrade.sh  upgradeWorkspaceTag --workspaces ws1,ws2  --tag-lib-domain domain1  --mdsurl 192.168.31.11:11810 --mdsuser user --mdspasswd paswd 
Status File Path: /opt/sequoiacm/tools/sequoiacm-tag-upgrade/./status/status-041f2c65-94b0-4f48-951e-0eeed8380e8b
Processing workspace(1/1): ws_quarter
Refactoring user tag, TotalFiles: 1000, ProcessedFiles 1000, FailedFiles: 0, Progress: 100%
Refactoring empty custom tag...
All workspace processed successfully.
```

> **Note:**
>
> * 输出的状态文件用于出错后通过 continueUpgradeTag 子命令继续执行

##continueUpgradeTag##

指定状态文件，继续执行工作区数据重构升级

###参数###


|选项           |缩写 |描述                                                                                    |是否必填|
|---------------|-----|----------------------------------------------------------------------------------------|--------|
|--status-file    | |指定状态文件，状态文件由 upgradeWorkspaceTag 子命令输出|是|


###示例###

   ```lang-javascript
   scm-tag-upgrade.sh  continueUpgradeTag --status-file ./status/status-91ca5e88-2d1a-432a-93d6-823ea0e38b1a
   ```