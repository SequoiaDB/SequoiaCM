package com.sequoiacm.config;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmAuditInfo;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class TestConfig extends ScmTestMultiCenterBase {
    private ScmSession session;
    private Sequoiadb db;

    @BeforeClass
    public void setUp() throws ScmException {
        session = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        db = new Sequoiadb(getSdb1().getUrl(), getSdbUser(), getSdbPasswd());
        ScmSystem.Configuration.setConfigProperties(session,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .deleteProperty("scm.audit.userMask").deleteProperty("scm.audit.mask")
                        .deleteProperty("scm.audit.userType.LOCAL")
                        .deleteProperty("scm.audit.user." + getScmUser())
                        .deleteProperty("scm.audit.userType.ALL").build());
    }

    @Test
    public void test() throws ScmException {
        ScmUpdateConfResultSet ret = ScmSystem.Configuration.setConfigProperties(session,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .updateProperty("scm.audit.userType.LOCAL", "").build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());

        truncateLog();

        ScmFactory.Session
                .createSession(
                        new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()))
                .close();

        ScmFactory.Workspace.getWorkspace(getWorkspaceName(), session);

        ScmSystem.Schedule.list(session, new BasicBSONObject()).close();

        makesureNoLog();

        ret = ScmSystem.Configuration.setConfigProperties(session,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                        .updateProperty("scm.audit.userType.LOCAL", "ALL").build());

        ScmSession tmpSs = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ScmCursor<ScmAuditInfo> c = ScmFactory.Audit.listInstance(session,
                new BasicBSONObject(ScmAttributeName.Audit.TYPE, "LOGIN"));
        Assert.assertTrue(c.hasNext());
        c.close();
        tmpSs.close();

        truncateLog();

        ScmFactory.Workspace.getWorkspace(getWorkspaceName(), session);
        c = ScmFactory.Audit.listInstance(session,
                new BasicBSONObject(ScmAttributeName.Audit.TYPE, "WS_DQL"));
        Assert.assertTrue(c.hasNext());
        c.close();

        truncateLog();

        ScmSystem.Schedule.list(session, new BasicBSONObject()).close();
        c = ScmFactory.Audit.listInstance(session,
                new BasicBSONObject(ScmAttributeName.Audit.TYPE, "SCHEDULE_DQL"));
        Assert.assertTrue(c.hasNext());
        c.close();

        ret = ScmSystem.Configuration.setConfigProperties(session, ScmConfigProperties.builder()
                .service("rootsite").updateProperty("scm.audit.userType.LOCAL", "DIR_DQL").build());

        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
        truncateLog();
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), session);
        ScmFactory.Directory.listInstance(ws, new BasicBSONObject());
        c = ScmFactory.Audit.listInstance(session,
                new BasicBSONObject(ScmAttributeName.Audit.TYPE, "WS_DQL"));
        Assert.assertEquals(c.getNext(), null);
        c.close();
        c = ScmFactory.Audit.listInstance(session,
                new BasicBSONObject(ScmAttributeName.Audit.TYPE, "DIR_DQL"));
        Assert.assertEquals(c.getNext().getType(), "DIR_DQL", c.toString());
        c.close();
        truncateLog();

    }

    private void makesureNoLog() {
        DBCollection cl = db.getCollectionSpace("SCMAUDIT").getCollection("AUDIT_LOG_EVENT");
        Assert.assertEquals(cl.getCount(), 0);
    }

    private void truncateLog() {
        DBCollection cl = db.getCollectionSpace("SCMAUDIT").getCollection("AUDIT_LOG_EVENT");
        cl.truncate();
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmSystem.Configuration.setConfigProperties(session,
                ScmConfigProperties.builder().service("rootsite", "schedule-server", "auth-server")
                .deleteProperty("scm.audit.userType.LOCAL").build());
        session.close();
        db.close();
    }
}
