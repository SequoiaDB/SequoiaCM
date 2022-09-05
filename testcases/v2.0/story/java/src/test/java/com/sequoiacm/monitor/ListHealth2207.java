package com.sequoiacm.monitor;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmHealth;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description:SCM-2207-2208:获取服务状态,一个服务类型只有一个节点
 * @author fanyu
 * @Date:2018年9月11日
 * @version:1.0
 */
public class ListHealth2207 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
    }

    @Test
    private void testList() throws Exception {
        ScmCursor< ScmHealth > cursor = null;
        try {
            cursor = ScmSystem.Monitor.listHealth( session, null );
            while ( cursor.hasNext() ) {
                ScmHealth healthInfo = cursor.getNext();
                if ( healthInfo.getStatus().equals( "UP" ) ) {
                    Assert.assertNotNull( healthInfo.getNodeName() );
                    Assert.assertNotNull( healthInfo.getServiceName() );
                } else {
                    Assert.fail( healthInfo.getStatus() + " "
                            + healthInfo.getServiceName() + " "
                            + healthInfo.getNodeName() );
                }
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test
    private void testGet() throws Exception {
        ScmCursor< ScmHealth > cursor = null;
        try {
            cursor = ScmSystem.Monitor.listHealth( session,
                    site.getSiteServiceName() );
            while ( cursor.hasNext() ) {
                ScmHealth healthInfo = cursor.getNext();
                if ( healthInfo.getStatus().equals( "UP" ) ) {
                    Assert.assertNotNull( healthInfo.getNodeName() );
                    Assert.assertEquals( healthInfo.getServiceName(),
                            site.getSiteServiceName() );
                } else {
                    Assert.fail( healthInfo.getStatus() + " "
                            + healthInfo.getServiceName() + " "
                            + healthInfo.getNodeName() );
                }
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }
}
