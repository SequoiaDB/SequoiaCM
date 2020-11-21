package com.sequoiacm.directory.serial;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description: SCM-2942:删除工作区后，获取目录
 * @author fanyu
 * @Date:2020年06月28日
 * @version:1.0
 */
public class CrudDir2942 extends TestScmBase {
    private SiteWrapper site;
    private String wsName = "ws2942";
    private ScmSession session;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        // 创建工作区
        ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        // 创建目录
        String fullPath = "/dir2942/dir2942A/dir2942B/dir2942C/dir2942D";
        String dirPath = "/";
        while ( fullPath.indexOf( "/", dirPath.length() ) != -1 ) {
            dirPath = dirPath + fullPath.substring( dirPath.length(),
                    fullPath.indexOf( "/", dirPath.length() ) );
            ScmFactory.Directory.createInstance( ws, dirPath );
            dirPath = dirPath + "/";
        }
        ScmFactory.Directory.createInstance( ws, fullPath );
        // 删除工作区
        ScmWorkspaceUtil.deleteWs( wsName, session );
        // 获取目录
        try {
            ScmFactory.Directory.getInstance( ws, fullPath );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            ScmWorkspaceUtil.deleteWs( wsName, session );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
