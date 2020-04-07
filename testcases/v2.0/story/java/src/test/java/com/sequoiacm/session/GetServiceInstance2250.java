package com.sequoiacm.session;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionMgr;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @Description:SCM-2250 :: ScmSystem.ServiceCenter.getServiceInstanceList接口测试
 * @author fanyu
 * @Date:2018年9月21日
 * @version:1.0
 */
public class GetServiceInstance2250 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSessionMgr sessionMgr = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        site = ScmInfo.getSite();
        sessionMgr = createSessionMgr();
        try {
            session = sessionMgr.getSession( SessionType.AUTH_SESSION );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test
    private void testListByNull() {
        List< ScmServiceInstance > list;
        try {
            list = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, null );
            for ( ScmServiceInstance instance : list ) {
                Assert.assertEquals( instance.getStatus(), "UP",
                        instance.toString() );
                Assert.assertNotNull( instance.getIp(), instance.toString() );
                Assert.assertNotNull( instance.getPort(), instance.toString() );
                Assert.assertNotNull( instance.getRegion(),
                        instance.toString() );
                Assert.assertNotNull( instance.getZone(), instance.toString() );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test
    private void testListByEmptyStr() {
        List< ScmServiceInstance > list;
        try {
            list = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, "" );
            for ( ScmServiceInstance instance : list ) {
                Assert.assertEquals( instance.getStatus(), "UP",
                        instance.toString() );
                Assert.assertNotNull( instance.getIp(), instance.toString() );
                Assert.assertNotNull( instance.getPort(), instance.toString() );
                Assert.assertNotNull( instance.getRegion(),
                        instance.toString() );
                Assert.assertNotNull( instance.getZone(), instance.toString() );
            }
            List< ScmServiceInstance > list1 = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, null );
            Assert.assertEquals( list.size(), list1.size(),
                    "list = " + list.toString() + ";list1 = " +
                            list1.toString() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test
    private void testListByNotAuthNull() {
        ScmSession session = null;
        List< ScmServiceInstance > list;
        try {
            session = sessionMgr.getSession( SessionType.NOT_AUTH_SESSION );
            list = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, null );
            for ( ScmServiceInstance instance : list ) {
                Assert.assertEquals( instance.getStatus(), "UP",
                        instance.toString() );
                Assert.assertNotNull( instance.getIp(), instance.toString() );
                Assert.assertNotNull( instance.getPort(), instance.toString() );
                Assert.assertNotNull( instance.getRegion(),
                        instance.toString() );
                Assert.assertNotNull( instance.getZone(), instance.toString() );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test
    private void testListByNotAuthEmptyStr() {
        ScmSession session = null;
        List< ScmServiceInstance > list;
        try {
            session = sessionMgr.getSession( SessionType.NOT_AUTH_SESSION );
            list = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, "" );
            for ( ScmServiceInstance instance : list ) {
                Assert.assertEquals( instance.getStatus(), "UP",
                        instance.toString() );
                Assert.assertNotNull( instance.getIp(), instance.toString() );
                Assert.assertNotNull( instance.getPort(), instance.toString() );
                Assert.assertNotNull( instance.getRegion(),
                        instance.toString() );
                Assert.assertNotNull( instance.getZone(), instance.toString() );
            }
            List< ScmServiceInstance > list1 = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, null );
            Assert.assertEquals( list.size(), list1.size(),
                    "list = " + list.toString() + ";list1 = " +
                            list1.toString() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test
    private void testListByNotAuthServiceName() {
        ScmSession session = null;
        List< ScmServiceInstance > list;
        try {
            session = sessionMgr.getSession( SessionType.NOT_AUTH_SESSION );
            list = ScmSystem.ServiceCenter.getServiceInstanceList( session,
                    site.getSiteServiceName() );
            for ( ScmServiceInstance instance : list ) {
                System.out.println( "instance = " + instance.toString() );
                Assert.assertEquals( instance.getStatus(), "UP",
                        instance.toString() );
                Assert.assertNotNull( instance.getIp(), instance.toString() );
                Assert.assertNotNull( instance.getPort(), instance.toString() );
                Assert.assertNotNull( instance.getRegion(),
                        instance.toString() );
                Assert.assertNotNull( instance.getZone(), instance.toString() );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test
    private void testListByServiceName() {
        List< ScmServiceInstance > list;
        try {
            list = ScmSystem.ServiceCenter.getServiceInstanceList( session,
                    site.getSiteServiceName() );
            for ( ScmServiceInstance instance : list ) {
                System.out.println( "instance = " + instance.toString() );
                Assert.assertEquals( instance.getStatus(), "UP",
                        instance.toString() );
                Assert.assertNotNull( instance.getIp(), instance.toString() );
                Assert.assertNotNull( instance.getPort(), instance.toString() );
                Assert.assertNotNull( instance.getRegion(),
                        instance.toString() );
                Assert.assertNotNull( instance.getZone(), instance.toString() );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test
    private void testListByInexist() {
        try {
            ScmSystem.ServiceCenter.getServiceInstanceList( session,
                    site.getSiteServiceName() + "1" );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test
    private void testSessionIsNull() {
        try {
            ScmSystem.ServiceCenter
                    .getServiceInstanceList( null, site.getSiteServiceName() );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test
    private void testSessionIsClosed() {
        ScmSession session = null;
        try {
            session = sessionMgr.getSession( SessionType.AUTH_SESSION );
            session.close();
            ScmSystem.ServiceCenter.getServiceInstanceList( session,
                    site.getSiteServiceName() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
        if ( sessionMgr != null ) {
            sessionMgr.close();
        }
    }

    private ScmSessionMgr createSessionMgr() {
        List< String > urlList = new ArrayList< String >();
        for ( String gateway : gateWayList ) {
            urlList.add( gateway + "/" + site.getSiteServiceName() );
        }
        ScmConfigOption scOpt;
        ScmSessionMgr sessionMgr = null;
        try {
            scOpt = new ScmConfigOption( urlList, TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            sessionMgr = ScmFactory.Session.createSessionMgr( scOpt, 1000 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        return sessionMgr;
    }
}
