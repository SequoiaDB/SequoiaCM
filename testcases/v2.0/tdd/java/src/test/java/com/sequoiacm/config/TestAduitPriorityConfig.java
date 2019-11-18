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

public class TestAduitPriorityConfig extends TestAuditConfigBase {
    private static final Logger logger = Logger.getLogger(TestAduitPriorityConfig.class);
    // admin用户session
    private ScmSession adminSession;
    // 新创建用户session
    private ScmSession session;

    @BeforeClass
    public void setUp() throws ScmException {
        adminSession = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));

    }


    /*  测试步骤：
     * （1）清空与该测试用户有关所有配置      （2）进行动态配置           
     * （3）创建新用户（LOCAL类型），并给予工作区和ADMIN权限 ，给予权限要sleep
     * （4）进行所有审计类型操作                      （5）查询审计日志，判断是否符合预期配置 
     * （6） 用户类型修改为TOKEN     （7）再次执行步骤（4）（5）
     * （8）清除该用户配置，删除用户和角色
     */
    @Test
    public void test() throws ScmException, InterruptedException {
        String usernamepre = "audit_User";
        String password = "audit_Password";

        // old config:LOCAL WS_DQL|WS_DML
        // userType与旧配置不冲突时，旧配置生效  （ LOCAL）
        long time = System.currentTimeMillis();
        String username = usernamepre + time;
        // (1)清空与该测试用户有关所有配置
        clearAudit(adminSession, username);
        // （2）进行动态配置
        ScmUpdateConfResultSet ret = ScmSystem.Configuration.setConfigProperties(adminSession,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .updateProperty("scm.audit.userMask", "LOCAL|TOKEN")
                        .updateProperty("scm.audit.mask", "WS_DQL|WS_DML")
                        .updateProperty("scm.audit.userType.TOKEN", "DIR_DQL|FILE_DML").build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
        // （3）创建新用户（LOCAL类型），并给予工作区和ADMIN权限 ，给予权限要sleep
        createUser(adminSession, username, ScmUserPasswordType.LOCAL);
        sleep();
        session = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        // （4）进行所有审计类型操作
        allAuditOperation(session, time);
        Set<String> auditTypes = new HashSet<String>();
        auditTypes.add("WS_DQL");
        auditTypes.add("CREATE_WS");
        auditTypes.add("DELETE_WS");
        // （5）查询审计日志，判断是否符合预期配置
        Assert.assertEquals(judgeAudit(adminSession,logger,auditTypes, username, "LOCAL"), true);
        session.close();

        // userType config:TOKEN DIR_DQL|FILE_DML
        // userType与旧配置冲突时优先级： userType>旧配置 （TOKEN）
        ScmUser user = ScmFactory.User.getUser(adminSession, username);
        // （6） 用户类型修改为TOKEN
        ScmFactory.User.alterUser(adminSession, user, new ScmUserModifier()
                .setPasswordType(ScmUserPasswordType.TOKEN).setPassword(password, password));
        session = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        // （7）再次执行步骤（4）（5）
        allAuditOperation(session, time);
        auditTypes = new HashSet<String>();
        auditTypes.add("DIR_DQL");
        auditTypes.add("CREATE_FILE");
        auditTypes.add("DELETE_FILE");
        Assert.assertEquals(judgeAudit(adminSession,logger,auditTypes, username, "TOKEN"), true);
        session.close();
        // （8）清除该用户配置，删除用户和角色
       deleteUser(adminSession, username);
       clearAuditUser(adminSession, username);

        
        // user config:username FILE_DQL
        // user与旧配置冲突时优先级： user>旧配置 （LOCAL）
        time = System.currentTimeMillis();
        username = usernamepre + time;
        clearAudit(adminSession, username);
        ret = ScmSystem.Configuration.setConfigProperties(adminSession,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .updateProperty("scm.audit.userMask", "LDAP|TOKEN|LOCAL")
                        .updateProperty("scm.audit.mask", "LOGIN")
                        .updateProperty("scm.audit.userType.TOKEN", "DIR_DQL|FILE_DML")
                        .updateProperty("scm.audit.user." + username, "FILE_DQL").build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
        createUser(adminSession, username, ScmUserPasswordType.LOCAL);
        sleep();
        session = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(session, time);
        auditTypes = new HashSet<String>();
        auditTypes.add("FILE_DQL");
        Assert.assertEquals(judgeAudit(adminSession,logger,auditTypes, username, "LOCAL"), true);
        session.close();

        // user config:username FILE_DQL
        // user与userType冲突时优先级： user>userType（TOKEN）
        user = ScmFactory.User.getUser(adminSession, username);
        ScmFactory.User.alterUser(adminSession, user, new ScmUserModifier()
                .setPasswordType(ScmUserPasswordType.TOKEN).setPassword(password, password));
        session = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(session, time);
        auditTypes = new HashSet<String>();
        auditTypes.add("FILE_DQL");
        Assert.assertEquals(judgeAudit(adminSession,logger,auditTypes, username, "TOKEN"), true);
        session.close();
        clearAuditUser(adminSession, username);
        deleteUser(adminSession, username);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        clearAudit(adminSession, getScmUser());
        adminSession.close();
    }
}
