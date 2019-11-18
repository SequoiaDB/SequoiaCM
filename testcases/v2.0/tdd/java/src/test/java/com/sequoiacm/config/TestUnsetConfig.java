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
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class TestUnsetConfig extends ScmTestMultiCenterBase {
    private ScmSession session;
    private Sequoiadb db;

    @BeforeClass
    public void setUp() throws ScmException {
        session = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        db = new Sequoiadb(getSdb1().getUrl(), getSdbUser(), getSdbPasswd());
    }

    @Test
    public void test() throws ScmException {

        // create a tmp user.
        try {
            ScmFactory.User.getUser(session, "UnsetConfigUser");
        }
        catch (Exception e) {
            ScmFactory.User.createUser(session, "UnsetConfigUser", ScmUserPasswordType.LOCAL,
                    "UnsetConfigPwd");
        }

        // set tmp user audit mask
        ScmUpdateConfResultSet ret = ScmSystem.Configuration.setConfigProperties(session,
                ScmConfigProperties.builder().service("auth-server")
                        .updateProperty("scm.audit.user.UnsetConfigUser", "ALL")
                        .updateProperty("scm.audit.userType.LOCAL", "").build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());
        truncateLog();

        // check log
        ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), "UnsetConfigUser", "UnsetConfigPwd"))
                .close();
        ScmCursor<ScmAuditInfo> c = ScmFactory.Audit.listInstance(session,
                new BasicBSONObject(ScmAttributeName.Audit.USERNAME, "UnsetConfigUser"));
        Assert.assertTrue(c.hasNext());
        c.close();

        truncateLog();

        // unset tmp user audit mask
        ret = ScmSystem.Configuration.setConfigProperties(session, ScmConfigProperties.builder()
                .service("auth-server").deleteProperty("scm.audit.user.UnsetConfigUser").build());
        Assert.assertTrue(ret.getFailures().size() == 0, ret.toString());

        truncateLog();

        // check log
        ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), "UnsetConfigUser", "UnsetConfigPwd"))
                .close();
        c = ScmFactory.Audit.listInstance(session,
                new BasicBSONObject(ScmAttributeName.Audit.USERNAME, "UnsetConfigUser"));
        ScmAuditInfo log = c.getNext();
        if (log != null) {
            Assert.fail(log.toString());
        }
        c.close();

        ScmFactory.User.deleteUser(session, "UnsetConfigUser");
    }

    private void truncateLog() {
        DBCollection cl = db.getCollectionSpace("SCMAUDIT").getCollection("AUDIT_LOG_EVENT");
        cl.truncate();
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmSystem.Configuration.setConfigProperties(session, ScmConfigProperties.builder()
                .service("auth-server").deleteProperty("scm.audit.userType.LOCAL").build());
       
        session.close();
        db.close();
    }
}
