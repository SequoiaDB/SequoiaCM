package com.sequoiacm.monitor;

import java.io.IOException;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmHealth;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

/**
 * @Description: SCM-2244:countSessions/GetStatus/ListHostInfo/gaugeResponse/
 *               showFlow接口测试
 * @author fanyu
 * @Date:2018年9月15日
 * @version:1.0
 */
public class Monitor_Param2244 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp()
            throws InterruptedException, IOException, ScmException {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        session.close();
    }

    @Test(groups = { "fourSite" })
    private void testCountSession() {
        try {
            ScmFactory.Session.countSessions( null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmFactory.Session.countSessions( session );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "fourSite" })
    private void testGetStatus() {
        try {

            ScmSystem.Monitor.listHealth( null, null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmSystem.Monitor.listHealth( session, null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmCursor< ScmHealth > cursor = ScmSystem.Monitor
                    .listHealth( session, "sdbserver" + UUID.randomUUID() );
            while ( cursor.hasNext() ) {
                ScmHealth info = cursor.getNext();
                System.out.println( "info = " + info.getNodeName() );
            }
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_INTERNAL_SERVER_ERROR ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { "fourSite" })
    private void testListHostInfo() {
        try {
            ScmSystem.Monitor.listHostInfo( null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmSystem.Monitor.listHostInfo( session );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "fourSite" })
    private void testGauge() {
        try {
            ScmSystem.Monitor.gaugeResponse( null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmSystem.Monitor.gaugeResponse( session );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "fourSite" })
    private void testShowFlow() {
        try {
            ScmSystem.Monitor.showFlow( null );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        try {
            ScmSystem.Monitor.showFlow( session );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }
}
