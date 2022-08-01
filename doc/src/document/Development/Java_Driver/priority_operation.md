这里介绍如何使用 Java 驱动接口编写使用权限功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。
    
更多查看 [Java API][java_api]。

##示例##
* 创建角色，并授权角色

 ```lang-javascript
// 使用管理员 admin 登录网关服务
ScmSession session = ScmFactory.Session
        .createSession(new ScmConfigOption("scmserver:8080/rootsite", "admin", "admin"));
// 创建角色 role
ScmRole role = ScmFactory.Role.createRole(session, "test_role", "desc");
// 创建工作区资源
ScmResource resource = ScmResourceFactory.createWorkspaceResource("test_ws");
// 授权角色 role 工作区 test_ws 的 ALL 权限
ScmFactory.Role.grantPrivilege(session, role, resource, ScmPrivilegeType.ALL);
 ```
  >  **Note:**
  > 
  >  * 创建角色需要管理员权限

* 用户关联角色，使用户获得权限

 ```lang-javascript
// 创建用户 user
ScmUser user = ScmFactory.User.createUser(session, "test_user", ScmUserPasswordType.LOCAL,
        "scmPassword");
// 用户 user 关联角色 role，该操作之后 user 就具有 role 角色的权限
ScmFactory.User.alterUser(session, user, new ScmUserModifier().addRole(role));
 ```

[java_api]:api/java/html/index.html