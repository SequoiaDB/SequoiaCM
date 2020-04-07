package com.sequoiacm.monitor;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmHostInfo;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description:SCM-2209::查询节点的主机状态,节点分布在同一台机器
 * @author fanyu
 * @Date:2018年9月11日
 * @version:1.0
 */
public class ListHostInfo2209 extends TestScmBase {
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
    private void testList() {
        ScmCursor< ScmHostInfo > cursor = null;
        try {
            cursor = ScmSystem.Monitor.listHostInfo( session );
            while ( cursor.hasNext() ) {
                ScmHostInfo str = cursor.getNext();
                Assert.assertNotNull( str.getCpuIdle() );
                Assert.assertNotNull( str.getCpuOther() );
                Assert.assertNotNull( str.getCpuSys() );
                Assert.assertNotNull( str.getCpuUser() );
                Assert.assertNotNull( str.getFreeRam() );
                Assert.assertNotNull( str.getHostName() );
                Assert.assertNotNull( str.getTotalRam() );
                Assert.assertNotNull( str.getTotalSwap() );

            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
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
