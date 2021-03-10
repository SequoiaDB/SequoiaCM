package com.sequoiacm.testcommon;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.testng.Assert;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttribute;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmAuditInfo;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmBatchInfo;
import com.sequoiacm.client.core.ScmClass;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmClassBasicInfo;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.metadata.ScmAttributeConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;

public class TestAuditConfigBase extends ScmTestMultiCenterBase {

    private static final Logger logger = Logger.getLogger(TestAuditConfigBase.class);

    public void clearAudit(ScmSession adminsession, String username) throws ScmException {
        ScmUpdateConfResultSet ret = ScmSystem.Configuration.setConfigProperties(adminsession,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .deleteProperty("scm.audit.userMask").deleteProperty("scm.audit.mask")
                        .deleteProperty("scm.audit.user." + username)
                        .deleteProperty("scm.audit.userType.LOCAL")
                        .deleteProperty("scm.audit.userType.TOKEN")
                        .deleteProperty("scm.audit.userType.LDAP")
                        .deleteProperty("scm.audit.userType.ALL").build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
    }

    public void clearAuditUser(ScmSession adminsession, String username) throws ScmException {
        ScmUpdateConfResultSet ret = ScmSystem.Configuration.setConfigProperties(adminsession,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .deleteProperty("scm.audit.user." + username).build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
    }

    public ScmUser createUser(ScmSession adminSession, String username,
            ScmUserPasswordType userType) throws ScmException {
        ScmResource resource = ScmResourceFactory.createWorkspaceResource(getWorkspaceName());
        ScmRole role = ScmFactory.Role.createRole(adminSession, username, "");
        ScmFactory.Role.grantPrivilege(adminSession, role, resource, ScmPrivilegeType.ALL);
        ScmRole adminRole = ScmFactory.Role.getRole(adminSession, "ROLE_AUTH_ADMIN");
        ScmUser user = ScmFactory.User.createUser(adminSession, username, userType,
                "audit_Password");
        user = ScmFactory.User.alterUser(adminSession, user,
                new ScmUserModifier().addRole(adminRole).addRole(role));

        return user;
    }

    // 给用户授权的休眠时间
    public static void sleep() throws InterruptedException {
        Thread.sleep(10000);
    }

    public void deleteUser(ScmSession adminSession, String username) throws ScmException {
        ScmFactory.User.deleteUser(adminSession, username);
        ScmFactory.Role.deleteRole(adminSession, username);
    }

    /*
     * 判断审计 : （1）判断审计日志是否含有配置审计类型（都含有） （2）判断审计日志是否含有未配置的审计类型（不含）
     */
    public boolean judgeAudit(ScmSession adminSession, Logger logger, Set<String> auditTypes,
            String username, String userType) throws ScmException {
        int configLogCount = 0;
        boolean isSuccess = false;
        logger.info("##start##--boolean--------auditType----------");
        // 判断审计日志是否含有配置审计类型
        for (String auditType : auditTypes) {
            ScmCursor<ScmAuditInfo> cursor = ScmFactory.Audit.listInstance(adminSession,
                    new BasicBSONObject(ScmAttributeName.Audit.USERNAME, username)
                            .append(ScmAttributeName.Audit.USERTYPE, userType)
                            .append(ScmAttributeName.Audit.TYPE, auditType));
            if (cursor.hasNext()) {
                isSuccess = true;
                // 配置审计类型日志数
                while (cursor.hasNext()) {
                    ScmAuditInfo auditInfo = cursor.getNext();
                    logger.info(isSuccess + ":" + auditInfo);
                    configLogCount += 1;
                }
                cursor.close();
            }
            else {
                isSuccess = false;
                logger.info("auditNoFound: username:" + username + ",auditType:" + auditType);
                cursor.close();
                break;
            }
        }
        logger.info("##end##--boolean--------auditType----------");
        // （2）判断审计日志是否含有未配置的审计类型，即配置审计类型日志数==总审计日志数
        if (isSuccess) {
            // 总审计日志数
            int logCount = 0;
            ScmCursor<ScmAuditInfo> cursor = ScmFactory.Audit.listInstance(adminSession,
                    new BasicBSONObject(ScmAttributeName.Audit.USERNAME, username)
                            .append(ScmAttributeName.Audit.USERTYPE, userType));
            while (cursor.hasNext()) {
                ScmAuditInfo auditInfo = cursor.getNext();
                if (!auditTypes.contains(auditInfo.getType())) {
                    logger.info("auditTypeNoMatch: " + auditInfo);
                }
                logCount += 1;
            }
            if (logCount != configLogCount) {
                isSuccess = false;
                logger.info("The number of audit logs does not match: username:" + username);
            }
            cursor.close();
        }
        return isSuccess;
    }

    public void allAuditOperation(ScmSession session, long time) throws ScmException {
        // WS_DQL 内容服务节点 查询工作空间操作
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), session);
        // WS_DML 内容服务节点 工作空间创建、删除、更新操作
        ScmMetaLocation meta = ws.getMetaLocation();
        List<ScmDataLocation> dataList = ws.getDataLocations();
        ScmWorkspaceConf conf = new ScmWorkspaceConf("audit_crete_test_ws" + time, meta, dataList);
        ScmFactory.Workspace.createWorkspace(session, conf);
        ScmFactory.Workspace.deleteWorkspace(session, "audit_crete_test_ws" + time, true);

        // FILE_DML 内容服务节点 文件创建、删除、更新操作
        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName("audit_crete_test_file" + time);
        file.setAuthor("xxxx");
        file.save();
        logger.info("fileId:" + file.getFileId());
        ScmFactory.File.deleteInstance(ws, file.getFileId(), true);
        // FILE_DQL 内容服务节点 查询文件操作
        ScmCursor<ScmFileBasicInfo> fileCursor = ScmFactory.File.listInstance(ws,
                ScopeType.SCOPE_ALL, new BasicBSONObject(ScmAttributeName.File.FILE_NAME, "audit_crete_test_file" + time));
        fileCursor.close();

        // DIR_DML 内容服务节点 目录创建、删除、更新操作
        ScmFactory.Directory.createInstance(ws, "/audit_crete_test_dir" + time);
        ScmFactory.Directory.deleteInstance(ws, "/audit_crete_test_dir" + time);
        // DIR _DQL 内容服务节点 查询目录操作
        ScmFactory.Directory.getInstance(ws, "/");

        // BATCH_DML 内容服务节点 批次创建、删除、更新操作
        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        ScmTags tags = new ScmTags();
        tags.addTag("tagsValue" + time);
        batch.setName("Batch" + time);
        batch.setTags(tags);
        batch.save();
        batch.delete();
        // BATCH_DQL 内容服务节点 查询批次操作
        ScmCursor<ScmBatchInfo> batchCursor = ScmFactory.Batch.listInstance(ws,
                new BasicBSONObject());
        batchCursor.close();

        // META_CLASS_DML 内容服务节点 自定义元数据模板dml操作
        ScmClass scmClass = ScmFactory.Class.createInstance(ws, "audit_create_test_class" + time,
                "desc");
        scmClass.delete();
        // META_CLASS_DQL 内容服务节点 查询自定义元数据模板操作
        ScmCursor<ScmClassBasicInfo> classCursor = ScmFactory.Class.listInstance(ws,
                new BasicBSONObject());
        classCursor.close();

        // META_ATTR_DML 内容服务节点 自定义元数据属性DML操作
        ScmAttribute attribute = ScmFactory.Attribute.createInstance(ws, new ScmAttributeConf()
                .setName("audit_create_test_attr" + time).setType(AttributeType.STRING));
        attribute.delete();
        // META_ATTR_DQL 内容服务节点 查询自定义元数据属性操作
        ScmCursor<ScmAttribute> attrCursor = ScmFactory.Attribute.listInstance(ws,
                new BasicBSONObject());
        attrCursor.close();

        // USER_DML 认证服务节点 用户创建、删除、更新操作
        ScmUser user = ScmFactory.User.createUser(session, "audit_create_test_username" + time,
                ScmUserPasswordType.LOCAL, "audit_create_test_password");
        // USER_DQL 认证服务节点 查询用户操作
        ScmCursor<ScmUser> userCursor = ScmFactory.User.listUsers(session);
        userCursor.close();

        // ROLE_DML 认证服务节点 角色创建、删除、更新操作
        ScmRole role = ScmFactory.Role.createRole(session, "audit_create_test_role" + time, "role");
        // ROLE _DQL 认证服务节点 查询角色操作
        ScmCursor<ScmRole> roleCursor = ScmFactory.Role.listRoles(session);
        roleCursor.close();

        // GRANT 认证服务节点 授权操作
        ScmResource resource = ScmResourceFactory.createWorkspaceResource(getWorkspaceName());
        ScmFactory.Role.grantPrivilege(session, role, resource, ScmPrivilegeType.ALL);

        // USER_DML、ROLE_DML
        ScmFactory.User.deleteUser(session, user);
        ScmFactory.Role.deleteRole(session, role);

        // LOGIN 认证服务节点 登录登出操作
        session = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), "audit_User" + time, "audit_Password"));
        ScmCursor<ScmSiteInfo> siteCursor = ScmFactory.Site.listSite(session);
        String sitename1 = siteCursor.getNext().getName();
        String sitename2 = siteCursor.getNext().getName();

        // SCHEDULE_DML 调度服务节点 调度任务创建、删除、更新操作
        ScmScheduleContent copyContent = new ScmScheduleCopyFileContent(sitename2, sitename1,
                "3650d", new BasicBSONObject(), ScopeType.SCOPE_CURRENT);
        ScmSchedule schedule = ScmSystem.Schedule.create(session, getWorkspaceName(),
                ScheduleType.COPY_FILE, "audit_create_test_schedule" + time, "", copyContent,
                "* * * *,* * ?");
        schedule.delete();
        // SCHEDULE_DQL 调度服务节点 查询调度任务操作
        ScmCursor<ScmScheduleBasicInfo> scheduleCursor = ScmSystem.Schedule.list(session,
                new BasicBSONObject());
        scheduleCursor.close();

        // ALL 所有节点 所有类型
    }

}
