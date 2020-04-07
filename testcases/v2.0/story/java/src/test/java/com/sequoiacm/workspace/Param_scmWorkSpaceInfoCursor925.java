/**
 *
 */
package com.sequoiacm.workspace;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description:1、不存在ws,游标为空(手工测)； 2、getNext()之前游标已关闭，执行getNext()获取ws信息；
 *                                 3、getNext()遍历部分，关闭游标后再次getNext(); 4、重复关闭游标；
 *                                 5、关闭session后再关闭游标；
 *                                 6、关闭session后继续getNext()(手工测)
 * @author fanyu
 * @Date:2017年9月20日
 * @version:1.0
 */

public class Param_scmWorkSpaceInfoCursor925 extends TestScmBase {
    private static SiteWrapper site = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            session = TestScmTools.createSession( site );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testRepeatClosed() {
        ScmCursor< ScmWorkspaceInfo > cursor = null;
        try {
            cursor = ScmFactory.Workspace.listWorkspace( session );
            cursor.close();
            cursor.close();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testCloseAfterCloseSS() {
        ScmSession session = null;
        ScmCursor< ScmWorkspaceInfo > cursor = null;
        try {
            session = TestScmTools.createSession( site );
            cursor = ScmFactory.Workspace.listWorkspace( session );
            session.close();
            cursor.close();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }
}
