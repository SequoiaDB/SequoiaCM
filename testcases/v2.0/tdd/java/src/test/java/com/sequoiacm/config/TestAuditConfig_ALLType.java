package com.sequoiacm.config;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.TestAuditConfigBase;

public class TestAuditConfig_ALLType extends TestAuditConfigBase {
    private static final Logger logger = Logger.getLogger(TestAuditConfig_ALLType.class);
    private ScmSession session;

    @BeforeClass
    public void setUp() throws ScmException {
        session = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
    }

    /*
     * 测试步骤： 
     * （1）清空与该测试用户有关所有配置  （2）进行动态配置 
     * （3）创建新用户（LOCAL类型），并给予工作区和ADMIN权限，给予权限要sleep
     * （4）进行所有审计类型操作               （5）查询审计日志，判断是否符合预期配置 
     * （6） 用户类型修改为TOKEN   （7）再次执行步骤（4）（5）
     * （8）清除该用户配置，删除用户和角色
     */
    @Test
    public void test() throws ScmException, InterruptedException {
        String usernamepre = "audit_User";
        String password = "audit_Password";

        // userType.LOCAL=DIR_DQL|FILE_DML
        // ALL测试 userType与旧配置：LOCAL配置生效 使用ALL配置不生效（LOCAL）
        long time = System.currentTimeMillis();
        String username = usernamepre + time;
        clearAudit(session, username);
        ScmUpdateConfResultSet ret = ScmSystem.Configuration.setConfigProperties(session,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .updateProperty("scm.audit.userMask", "ALL")
                        .updateProperty("scm.audit.mask", "LOGIN|WS_DQL")
                        .updateProperty("scm.audit.userType.LOCAL", "DIR_DQL|FILE_DML").build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
        createUser(session, username, ScmUserPasswordType.LOCAL);
        sleep();
        ScmSession sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        Set<String> auditTypes = new HashSet<String>();
        auditTypes.add("DIR_DQL");
        auditTypes.add("CREATE_FILE");
        auditTypes.add("DELETE_FILE");
        Assert.assertEquals(
                judgeAudit(session, logger, auditTypes, username, "LOCAL"), true);

        // ALL=LOGIN|WS_DQL
        // TOkEN没配置使用ALL配置（TOKEN）
        ScmUser user = ScmFactory.User.getUser(session, username);
        ScmFactory.User.alterUser(session, user, new ScmUserModifier()
                .setPasswordType(ScmUserPasswordType.TOKEN).setPassword(password, password));
        sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        auditTypes = new HashSet<String>();
        auditTypes.add("LOGIN");
        auditTypes.add("WS_DQL");
        Assert.assertEquals(
                judgeAudit(session, logger, auditTypes, username, "TOKEN"), true);
        deleteUser(session, username);
        clearAuditUser(session, username);

        // ALL测试 user与ALL （LOCAL）
        time = System.currentTimeMillis();
        username = usernamepre + time;
        clearAudit(session, username);
        ret = ScmSystem.Configuration.setConfigProperties(session,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .updateProperty("scm.audit.userType.ALL", "LOGIN|WS_DQL")
                        .updateProperty("scm.audit.user." + username, "DIR_DQL|FILE_DML").build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
        createUser(session, username, ScmUserPasswordType.LOCAL);
        sleep();
        sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        auditTypes = new HashSet<String>();
        auditTypes.add("DIR_DQL");
        auditTypes.add("CREATE_FILE");
        auditTypes.add("DELETE_FILE");
        Assert.assertEquals(
        judgeAudit(session, logger, auditTypes, username, "LOCAL"), true);
        
        // ALL测试 user与ALL （TOKEN）
        user = ScmFactory.User.getUser(session, username);
        ScmFactory.User.alterUser(session, user, new ScmUserModifier()
                .setPasswordType(ScmUserPasswordType.TOKEN).setPassword(password, password));
        sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        auditTypes = new HashSet<String>();
        auditTypes.add("DIR_DQL");
        auditTypes.add("CREATE_FILE");
        auditTypes.add("DELETE_FILE");
        Assert.assertEquals(
                judgeAudit(session, logger, auditTypes, username, "TOKEN"), true);
        deleteUser(session, username);
        clearAuditUser(session, username);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        clearAudit(session, getScmUser());
        session.close();
    }
}
