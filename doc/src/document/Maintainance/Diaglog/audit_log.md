SequoiaCM 系统的审计日志支持丰富的审计类型，支持以文件的形式保存审计日志，同时支持审计日志持久化至 SequoiaDB。

## 存储配置 ##
节点安装目录下的 conf 目录，包含每个节点的日志配置文件 logback.xml，其中 auditTOSDB 为审计日志持久化至 SequoiaDB 的 appender ，auditTOFILE 为审计日志记录至文件的 appender。可以通过修改这两个 appender 的属性修改相关配置。

## 审计配置 ##

审计日志的配置通过节点的配置文件（application.properties）进行配置：

- scm.audit.user.xxx 配置具体用户的审计类型，如 scm.audit.user.admin = FILE_DQL|DIR_DML 表示 admin 用户需要审计 FILE_DQL 和 DIR_DML 操作

- scm.audit.userType.xxx 配置某一类用户的审计类型，如 scm.audit.userType.LOCAL = FILE_DQL|DIR_DML 表示 LOCAL 类型的用户需要审计 FILE_DQL 和 DIR_DML 操作

**Note**
>
> - 兼容旧版配置 scm.audit.userMask、scm.audit.mask，这是一对组合使用的审计配置，如：scm.audit.userMask = LOCAL|TOKEN 、 scm.audit.mask = FILE_DQL|DIR_DML 表示类型为 LOCAL 或 TOKEN 的用户需要审计 FILE_DQL 和 DIR_DML 操作
>
> - 审计配置优先级：（1）当具体用户属于该用户类型，则 具体用户配置 > 用户类型配置 ；（2）当用户类型与旧版本方式都配置同一用户类型，则用户类型 > 旧版本配置；（3）当用户类型与旧版本方式配置不同用户类型，则将旧版本配置存在的且用户类型方式不存在的用户类型，转换成用户类型配置，最终配置为两种配置方式并集


**审计操作类型掩码列表**

|操作类型掩码  |所属节点        |说明                           |
|--------------|----------------|-------------------------------|
|FILE_DML      |内容服务节点	|文件创建、删除、更新操作       |
|FILE_DQL      |内容服务节点	|查询文件操作                   |
|WS_DML        |内容服务节点	|工作空间创建、删除、更新操作   |
|WS_DQL        |内容服务节点	|查询工作空间操作               |
|DIR_DML       |内容服务节点	|目录创建、删除、更新操作       |
|DIR _DQL      |内容服务节点 	|查询目录操作                   |
|BATCH_DML     |内容服务节点	|批次创建、删除、更新操作       |
|BATCH_DQL     |内容服务节点	|查询批次操作                   |
|META_CLASS_DML|内容服务节点	|自定义元数据模板dml操作        |
|META_CLASS_DQL|内容服务节点	|查询自定义元数据模板操作       |
|META_ATTR_DML |内容服务节点    |自定义元数据属性DML操作        |
|META_ATTR_DQL |内容服务节点	|查询自定义元数据属性操作       |
|USER_DML      |认证服务节点	|用户创建、删除、更新操作       |
|USER_DQL      |认证服务节点	|查询用户操作                   |
|ROLE_DML      |认证服务节点	|角色创建、删除、更新操作       |
|ROLE _DQL     |认证服务节点	|查询角色操作                   |
|GRANT         |认证服务节点	|授权操作                       |
|LOGIN         |认证服务节点	|登录登出操作                   |
|SCHEDULE_DML  |调度服务节点	|调度任务创建、删除、更新操作   |
|SCHEDULE_DQL  |调度服务节点	|查询调度任务操作               |
|ALL           |所有节点        |所有类型                       |

 > **Note:**
 >
 > * 所有节点包括：内容服务节点、调度服务节点、认证服务节点
 >
 > * 多种审计类型用 "|" 符号隔开，如：FILE_DML|DIR_DML|WS_DML

**审计用户类型掩码列表**

|用户类型掩码 |所属节点   |说明                                             |
|-------------|-----------|-------------------------------------------------|
|LOCAL        |所有节点   |针对 SequoiaCM 系统本地用户登陆的用户记录审计日志|
|LDAP         |所有节点   |针对密码类型为 LDAP 的用户记录审计日志           |
|TOKEN        |所有节点   |针对 token 登录的用户记录审计日志                |
|ALL          |所有节点   |对所有用户记录审计日志                           |

 > **Note:**
 >
 > * 所有节点包括：内容服务节点、调度服务节点、认证服务节点
 >
 > * 多种审计类型 用 "|" 符号隔开，如：LOCAL|TOKEN

## 审计信息 ##

具体审计日志包含的信息如下：

|审计信息     |类型       |说明                                             |
|-------------|-----------|-------------------------------------------------|
|host         |String     |节点主机名                                       |
|port         |String     |节点端口号                                       |
|time         |Timestamp  |审计事件生成时间                                 |
|thread       |String     |审计事件生成线程名                               |
|type         |String     |审计操作类型                                     |
|user_type    |String     |审计用户类型                                     |
|user_name    |String     |审计用户名称                                     |
|work_space   |String     |工作空间名称                                     |
|message      |String     |审计message信息                                  |


