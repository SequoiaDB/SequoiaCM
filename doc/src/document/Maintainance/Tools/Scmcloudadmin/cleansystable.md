cleansystable 子命令提供清理系统表的功能。

##子命令选项##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|----------------------------------------------------   |--------|
|--type     |-t   |节点类型，可选值：auth-server、admin-server            |是      |
|--url      |     |SequoiaDB 地址，如：sdbhost:11810                      |是      |
|--user     |     |SequoiaDB 用户名，默认为空                             |否      |
|--password-file| |SequoiaDB 密码文件，默认为空                           |否      |

>  **Note:**
>
>  *  此命令用于搭建 Cloud 服务之前，清理 Sequoiadb 中可能存在的残留系统表。
>
>  *  Sequoiadb 的密码的加密文件生成请查看 [encrypt 命令][encrypt_tool]

###示例###

清理 auth-server 节点残留的相关系统表

```lang-javascript
   $ scmcloudadmin.sh cleansystable --type auth-server --url localhost:11800 --user sdbadmin --password-file /home/mount/scm/contentserver/sdb.passwd
```

[encrypt_tool]:Maintainance/Tools/Scmadmin/encrypt.md