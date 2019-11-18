##概述##

SequoiaCM 的权限模型为 用户（User）<->角色（Role）<->权限（Privilege）。如下图所示：

![总体架构][priority_arch]

1. 角色（Role）关联资源（Resource）和资源的权限（Privilege）之后，该角色就拥有资源相应的权限。

2. 用户（User）通过关联角色来取得资源的权限。

3. 除了角色拥有的权限外，其它权限一律禁止
 
4. 用户继承所属角色的所有权限

## 权限 ##

权限分为五种：创建（CREATE），读（READ），更新（UPDATE），删除（DELETE）以及所有权限（ALL）

资源分为两种：工作区资源和工作区下的目录资源

1. 工作区资源下包括：文件、目录、批次、任务调度等。

2. 单独对目录资源做权限是为了更细粒度的控制目录的权限。并且可以单独对目录进行授权，使得其它用户可以访问目录下的资源（目录+文件），但是不能访问工作区的其它资源。

3. 工作区资源和目录资源的权限有相关性，当角色对工作区资源具有 READ 权限时，其对目录资源也具有 READ 权限。但是，当角色对目录资源有 READ 权限时，其对工作区的其它资源没有 READ 权限。

4. 当角色对工作区资源有 ALL 权限时， 其可以对工作区资源进行 CREATE，READ，UPDATE，DELETE等操作，还能对工作区资源进行授权，授权范围包括工作区下的所有资源。

## 角色 ##

角色为一系列权限的集合。主要是为了方便管理多个权限，对权限进行分组等。

1. 系统内置了一个管理员用户 admin 和一个管理员角色 ROLE_AUTH_ADMIN 。

2. 管理员用户 admin 默认已经关联管理员角色 ROLE_AUTH_ADMIN 。拥有角色 ROLE_AUTH_ADMIN 的用户可以对所有资源进行授权或取消授权。但无法对资源进行实际的创建、读、更新、删除等操作。
>  **Note:**
>
>  * ROLE_AUTH_ADMIN 角色可以进行创建和删除工作区的管理操作，但无法读取工作区内的文件、目录等资源。

3. 具有工作区 ALL 权限的角色也可以将该工作区下的资源授权给其它角色，或者取消其它角色的授权。

## 用户 ##

用户是外部业务系统操作并使用 SequoiaCM 系统的帐号。绝大部分命令必须先使用用户登陆后才能执行。

1. 用户不直接拥有权限，只能通过关联角色来取得该角色的权限。

2. 当用户对某个资源进行操作时，系统会根据权限设置检查该用户是否具有相应的权限，拥有相应的权限时才能成功操作该资源

## 基本操作示例 ##

1. 系统初始时，只有一个内置的角色 ROLE_AUTH_ADMIN，以及与该角色关联的内置用户 admin

 ```
# /opt/sequoiacm/contentserver/bin/scmadmin.sh listrole --url 192.168.31.90:8080 --user admin --password admin
Name             Id                        Desc
ROLE_AUTH_ADMIN  5b87b7e4e4b0ddf6e49af255  authentication administrator
Total:1
# /opt/sequoiacm/contentserver/bin/scmadmin.sh listuser --url 192.168.31.90:8080 --user admin --password admin
Name   Id                        Roles
admin  5b87b7e4e4b0ddf6e49af256  ROLE_AUTH_ADMIN
Total:1
 ```
>  **Note:**
> 
>  * 假设网关的地址为：192.168.31.90:8080
>
>  * 初始时， admin 用户的密码为 admin

2. 创建一个新角色 ROLE_ws_test ，并授权该角色对工作区 ws_test 的所有权限（ALL）

 ```
# /opt/sequoiacm/contentserver/bin/scmadmin.sh createrole --role ROLE_ws_test --url 192.168.31.90:8080 --user admin --password admin
Create role success:ROLE_ws_test
# /opt/sequoiacm/contentserver/bin/scmadmin.sh grantrole --role ROLE_ws_test --type workspace --resource ws_test --privilege ALL --url "192.168.31.90:8080/rootsite" --user admin --password admin
Grant role success:role=ROLE_ws_test,resource=ws_test,privilege=ALL
 ```
>  **Note:**
> 
>  * 授权（grantrole）时，需要发送请求到 Content Server ，所以该命令的网关的地址需要增加 Content Server 的服务名：192.168.31.90:8080/rootsite
>
>  * 假设 工作区 ws_test 已经创建。创建工作区请参考命令[createws][createws_tool]

3. 创建一个用户 test ，并关联角色 ROLE_ws_test 。使得用户 test 具有工作区 ws_test 的所有权限。

 ```
# /opt/sequoiacm/contentserver/bin/scmadmin.sh createuser --new-user test --new-password test --url 192.168.31.90:8080 --user admin --password admin
Create user success:test
# /opt/sequoiacm/contentserver/bin/scmadmin.sh attachrole --attached-user test --role ROLE_ws_test --url 192.168.31.90:8080 --user admin --password admin
Attach role success:user=test,roleName=ROLE_ws_test
 ```

4. 后续通过用户 test 登陆 SequoiaCM 系统时，该用户就可以操作工作区 ws_test 下的所有资源。


[priority_arch]:Architecture/priority.png
[createws_tool]:Maintainance/Tools/Scmadmin/createws.md