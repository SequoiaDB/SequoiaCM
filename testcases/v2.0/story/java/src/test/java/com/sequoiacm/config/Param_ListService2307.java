package com.sequoiacm.config;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @author fanyu
 * @Description: SCM-2307 :: ScmSystem.ServiceCenter.listService参数校验
 * @Date:2018年12月04日
 * @version:1.0
 */
public class Param_ListService2307 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            List< String > services = ScmSystem.ServiceCenter
                    .getServiceList( session );
            Boolean flag = false;
            for ( String service : services ) {
                if ( service.equalsIgnoreCase( site.getSiteServiceName() ) ) {
                    flag = true;
                    break;
                }
            }
            Assert.assertTrue( flag,
                    "site.getSiteServiceName() must be in services,services ="
                            + " " + services.toString() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSSIsNull() {
        try {
            ScmSystem.ServiceCenter.getServiceList( null );
            Assert.fail(
                    "ServiceCenter.getServiceList must be failed when session"
                            + " is null" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test1() throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            List< ScmServiceInstance > instances = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session,
                            site.getSiteServiceName() );
            Assert.assertTrue( instances.size() >= 1 );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testSSIsNull1() {
        try {
            ScmSystem.ServiceCenter.getServiceInstanceList( null,
                    site.getSiteServiceName() );
            Assert.fail(
                    "ServiceCenter.getServiceInstanceList must be failed when"
                            + " session is null" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testClosedSS() throws ScmException {
        ScmSession session = TestScmTools.createSession( site );
        session.close();
        try {
            ScmSystem.ServiceCenter.getServiceInstanceList( session,
                    site.getSiteServiceName() );
            Assert.fail(
                    "ServiceCenter.getServiceInstanceList must be failed when"
                            + " session is closed" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testServiceNameNoExist() throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmSystem.ServiceCenter.getServiceInstanceList( session,
                    site.getSiteServiceName() + "_inexistence" );
            Assert.fail(
                    "ServiceCenter.getServiceList must be failed when servicename is not exist" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
    }
}
