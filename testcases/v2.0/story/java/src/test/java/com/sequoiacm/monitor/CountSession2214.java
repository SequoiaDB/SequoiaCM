package com.sequoiacm.monitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSessionInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description: SCM-2214 :: 多个Authserver节点,查看系统的会话数
 * @author fanyu
 * @Date:2018年9月11日
 * @version:1.0
 */
public class CountSession2214 extends TestScmBase {
    private SiteWrapper site = null;
    private List< ScmSession > sessionList = new ArrayList< ScmSession >();
    private int num = 100;
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    private void setUp()
            throws InterruptedException, IOException, ScmException {
        site = ScmInfo.getSite();
        for ( int i = 0; i < num; i++ ) {
            ScmSession session = TestScmTools.createSession( site );
            sessionList.add( session );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        long count = ScmFactory.Session.countSessions( sessionList.get( 0 ) );
        Assert.assertEquals( count >= num, true );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        if ( !runSuccess ) {
            ScmCursor< ScmSessionInfo > cursor = null;
            try {
                cursor = ScmFactory.Session
                        .listSessions( sessionList.get( 0 ) );
                while ( cursor.hasNext() ) {
                    System.out.println( cursor.getNext().toString() );
                }
            } finally {
                if ( cursor != null ) {
                    cursor.close();
                }
            }
        }

        for ( ScmSession session : sessionList ) {
            session.close();
            System.out.println( "closed session = " + session.getSessionId() );
        }
    }
}
