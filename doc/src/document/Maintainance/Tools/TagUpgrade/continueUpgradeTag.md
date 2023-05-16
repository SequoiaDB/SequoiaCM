
continueUpgradeTag 子命令提供重启标签升级任务的能力，当升级任务异常中断时，用户可使用该命令重新开启。

##子命令选项##

| 选项 | 缩写 |描述 | 是否必填 |
| ---- | ---- |-----| -------- |
| --status-file | - | 指定状态文件，该文件由 [upgradeWorkspaceTag][upgradeWorkspaceTag] 子命令输出 | 是 |

###示例###

重启标签升级任务

```lang-bash
$ scm-tag-upgrade.sh continueUpgradeTag --status-file ./status/status-91ca5e88-2d1a-432a-93d6-823ea0e38b1a
```

[upgradeWorkspaceTag]:Maintainance/Tools/TagUpgrade/upgradeWorkspaceTag.md