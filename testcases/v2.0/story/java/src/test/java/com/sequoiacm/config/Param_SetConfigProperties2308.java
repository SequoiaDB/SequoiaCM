package com.sequoiacm.config;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author fanyu
 * @Description: SCM-2308 ::  ScmSystem.Configuration. setConfigProperties参数校验
 * @Date:2018年12月10日
 * @version:1.0
 */
public class Param_SetConfigProperties2308 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testSSIsNull1() {
        try {
            ScmSystem.Configuration.setConfigProperties(null,
                    ScmConfigProperties.builder()
                            .service(site.getSiteServiceName())
                            .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL").build());
            Assert.fail("ScmSystem.Configuration.setConfigProperties must be failed when session is null");
        } catch (ScmException e) {
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testClosedSS() throws ScmException {
        ScmSession session = TestScmTools.createSession(site);
        session.close();
        try {
            ScmSystem.Configuration.setConfigProperties(session,
                    ScmConfigProperties.builder()
                            .service(site.getSiteServiceName())
                            .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL").build());
            Assert.fail("  ScmSystem.Configuration.setConfigProperties must be failed when session is closed");
        } catch (ScmException e) {
            if (e.getError() != ScmError.SESSION_CLOSED) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testNull() throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(site);
            ScmSystem.Configuration.setConfigProperties(session, null);
            Assert.fail("  ScmSystem.Configuration.setConfigProperties must be failed when ConfigProperties is null");
        } catch (ScmException e) {
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                Assert.fail(e.getMessage());
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }


    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testProperties1() throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(site);
            ScmSystem.Configuration.setConfigProperties(session, ScmConfigProperties.builder()
                    .updateProperty(ConfigCommonDefind.scm_audit_mask, "ALL")
                    .build());
            Assert.fail("  ScmSystem.Configuration.setConfigProperties must be failed when target is null");
        } catch (ScmException e) {
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                Assert.fail(e.getMessage());
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Test(groups = {"oneSite", "twoSite", "fourSite"})
    private void testProperties2() throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession(site);
            ScmSystem.Configuration.setConfigProperties(session, ScmConfigProperties.builder()
                    .service(site.getSiteServiceName())
                    .build());
            Assert.fail("  ScmSystem.Configuration.setConfigProperties must be failed when property is null");
        } catch (ScmException e) {
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                Assert.fail(e.getMessage());
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
    }
}
