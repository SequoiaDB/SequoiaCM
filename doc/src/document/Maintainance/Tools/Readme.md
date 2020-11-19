SequoiaCM 系统管理工具提供 SequoiaCM 业务部署，及节点管理等功能。可执行文件位于 SequoiaCM 安装路径的 bin 目录下。

##Spring Cloud 工具##

|工具    |脚本名 |功能   |
|--------|-------|-------|
|[配置管理工具][scmcloudadmin]|scmcloudadmin.sh|提供 Spring Cloud 服务管理功能|
|[节点管理工具][scmcloudctl]|scmcloudctl.sh|提供 Spring Cloud 节点管理相关的功能|
|[服务部署工具][cloud_deploy]|deploy.py|提供 Spring Cloud 服务部署的功能|

##内容服务工具##

|工具    |脚本名 |功能   |
|--------|-------|-------|
|[系统管理工具][scmadmin] |scmadmin.sh|提供业务部署相关的功能，包含创建站点、创建工作区等功能|
|[节点管理工具][scmctl] |scmctl.sh|提供节点相关的功能，包含节点启停、刷新节点业务配置等功能|
|[服务部署工具][contentserver_deploy]|deploy.py|提供内容服务部署的功能|

>  **Note：**
>
>  * 工具中涉及到的 ds 为 DataSource （数据源）的缩写，数据源是为 Site （站点）提供数据或元数据储存服务的数据库服务器（如 SequoiaDB ）。
>
>  * 特殊的，root site 下提供元数据储存服务的数据源为 mds （ Meta DataSource ）
>
>  * 以 SequoiaDB 为例，dsurl、dsuser、dspasswd 分别为 SequoiaDB 的 coord 节点服务地址、用户名、密码。
>
>  * mdsurl、mdsuser、mdspasswd 特指 __root site__ 下元数据存储服务 SequoiaDB 的 coord 节点服务地址、用户名、密码

##调度服务工具##

|工具    |脚本名 |功能   |
|--------|-------|-------|
|[配置管理工具][schadmin]|schadmin.sh|提供调度服务管理功能|
|[节点管理工具][schctl]|schctl.sh|提供调度服务节点管理相关的功能|
|[服务部署工具][sch_deploy]|deploy.py|提供调度服务部署的功能|

##配置服务工具##

|工具    |脚本名 |功能   |
|--------|-------|-------|
|[配置管理工具][confadmin]|confadmin.sh|提供配置服务管理功能|
|[节点管理工具][confctl]|confctl.sh|提供配置服务节点管理相关的功能|
|[服务部署工具][conf_deploy]|deploy.py|提供配置服务部署的功能|

##工具通用选项##


|参数       |缩写 |描述             |
|---------- |-----|-----------------|
|--help     |-h   |打印帮助信息     |
|--version  |-v   |查看版本信息     |


[system]:Maintainance/Tools/system_init_script.md
[scmcloudadmin]:Maintainance/Tools/Scmcloudadmin/Readme.md
[scmcloudctl]:Maintainance/Tools/Scmcloudctl/Readme.md
[cloud_deploy]:Maintainance/Tools/cloud_deploy_script.md

[scmadmin]:Maintainance/Tools/Scmadmin/Readme.md
[scmctl]:Maintainance/Tools/Scmctl/Readme.md
[contentserver_deploy]:Maintainance/Tools/contentserver_deploy_script.md

[confctl]:Maintainance/Tools/Confctl/Readme.md
[confadmin]:Maintainance/Tools/Confadmin/Readme.md
[conf_deploy]:Maintainance/Tools/config_deploy_script.md

[schctl]:Maintainance/Tools/Schadmin/Readme.md
[schadmin]:Maintainance/Tools/Schadmin/Readme.md
[sch_deploy]:Maintainance/Tools/schedule_deploy_script.md
