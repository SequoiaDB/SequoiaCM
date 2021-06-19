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
    private void setUp() {
        site = ScmInfo.getRootSite();
        try {
            session = TestScmTools.createSession( site );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void testList() throws Exception {
        ScmCursor< ScmHealth > cursor = null;
        try {
            cursor = ScmSystem.Monitor.listHealth( session, null );
            while ( cursor.hasNext() ) {
                ScmHealth str = cursor.getNext();
                // SEQUOIACM-681暂时屏蔽掉s3节点的测试
                if ( !( str.getServiceName().equals( "s3" ) ) ) {
                    Assert.assertEquals( str.getStatus(), "UP" );
                }
                Assert.assertNotNull( str.getNodeName() );
                Assert.assertNotNull( str.getServiceName() );
            }
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }
    }

    @Test(groups = { "fourSite" })
    private void testGet() throws Exception {
        ScmCursor< ScmHealth > cursor = null;
        try {
            cursor = ScmSystem.Monitor.listHealth( session,
                    site.getSiteServiceName() );
            while ( cursor.hasNext() ) {
                ScmHealth str = cursor.getNext();
                Assert.assertEquals( str.getStatus(), "UP" );
                Assert.assertEquals( str.getServiceName(),
                        site.getSiteServiceName() );
                Assert.assertNotNull( str.getNodeName() );
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
