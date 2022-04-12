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

public class TestAuditSingleConfig extends TestAuditConfigBase {
    private static final Logger logger = Logger.getLogger(TestAuditSingleConfig.class);
    private ScmSession session;

    @BeforeClass
    public void setUp() throws ScmException {
        session = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void test() throws ScmException, InterruptedException {
        String usernamepre = "audit_User";
        String password = "audit_Password";

        // 旧配置 （LOCAL） 
        long time = System.currentTimeMillis();
        String username = usernamepre + time;
        clearAudit(session, username);
        ScmUpdateConfResultSet ret = ScmSystem.Configuration.setConfigProperties(session,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .updateProperty("scm.audit.userMask", "LOCAL")
                        .updateProperty("scm.audit.mask", "WS_DQL|WS_DML").build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
        createUser(session, username, ScmUserPasswordType.LOCAL);
        sleep();
        ScmSession sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        Set<String> auditTypes = new HashSet<String>();
        auditTypes.add("WS_DQL");
        auditTypes.add("CREATE_WS");
        auditTypes.add("DELETE_WS");
        Assert.assertEquals(judgeAudit(session, logger,auditTypes, username, "LOCAL"), true);
        // 旧配置 （TOKEN） 
        ScmUser user = ScmFactory.User.getUser(session, username);
        ScmFactory.User.alterUser(session, user, new ScmUserModifier()
                .setPasswordType(ScmUserPasswordType.TOKEN).setPassword(password, password));
        sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        Assert.assertEquals(judgeAudit(session, logger,auditTypes, username, "TOKEN"), false);
        sessionTmp.close();
        deleteUser(session, username);
        clearAuditUser(session, username);

        // userType配置 （LOCAL） 
        time = System.currentTimeMillis();
        username = usernamepre + time;
        clearAudit(session, username);
        ret = ScmSystem.Configuration.setConfigProperties(session, ScmConfigProperties.builder()
                .service("rootsite", "schedule-server", "auth-server")
                .updateProperty("scm.audit.userType.TOKEN", "LOGIN|SCHEDULE_DML|SCHEDULE_DQL")
                .build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
        createUser(session, username, ScmUserPasswordType.LOCAL);
        sleep();
        sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        auditTypes = new HashSet<String>();
        auditTypes.add("LOGIN");
        auditTypes.add("SCHEDULE_DQL");
        auditTypes.add("CREATE_SCHEDULE");
        auditTypes.add("DELETE_SCHEDULE");
        Assert.assertEquals(judgeAudit(session, logger,auditTypes, username, "LOCAL"), false);
        // 旧配置 （TOKEN） 
        user = ScmFactory.User.getUser(session, username);
        ScmFactory.User.alterUser(session, user, new ScmUserModifier()
                .setPasswordType(ScmUserPasswordType.TOKEN).setPassword(password, password));
        sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        Assert.assertEquals(judgeAudit(session, logger,auditTypes, username, "TOKEN"), true);
        sessionTmp.close();
        deleteUser(session, username);
        
        // user配置（LOCAL） 
        time = System.currentTimeMillis();
        username = usernamepre + time;
        clearAudit(session, username);
        ret = ScmSystem.Configuration.setConfigProperties(session,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .updateProperty("scm.audit.user." + username, "BATCH_DQL|META_CLASS_DML")
                        .build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
        createUser(session, username, ScmUserPasswordType.LOCAL);
        sleep();
        sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        auditTypes = new HashSet<String>();
        auditTypes.add("BATCH_DQL");
        auditTypes.add("CREATE_META_CLASS");
        auditTypes.add("DELETE_META_CLASS");
        auditTypes.add("CREATE_META_ATTR");
        auditTypes.add("DELETE_META_ATTR");
        Assert.assertEquals(judgeAudit(session, logger,auditTypes, username, "LOCAL"), true);
        // user配置 （TOKEN） 
        user = ScmFactory.User.getUser(session, username);
        ScmFactory.User.alterUser(session, user, new ScmUserModifier()
                .setPasswordType(ScmUserPasswordType.TOKEN).setPassword(password, password));
        sessionTmp = ScmFactory.Session
                .createSession(new ScmConfigOption(getServer1().getUrl(), username, password));
        allAuditOperation(sessionTmp, time);
        Assert.assertEquals(judgeAudit(session, logger, auditTypes, username, "TOKEN"), true);
        sessionTmp.close();
        deleteUser(session, username);
        clearAuditUser(session, username);
    }

    @AfterClass
    public void tearDown() throws ScmException {
        clearAudit(session, getScmUser());
        session.close();
    }
}
