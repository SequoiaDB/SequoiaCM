package com.sequoiacm.rest.serial;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description DeleteWorkspace2181.java 创建workspace
 * @author luweikang
 * @date 2018年5月24日
 */
public class DeleteWorkspace2181 extends TestScmBase {

    private static SiteWrapper site = null;
    private ScmSession session = null;
    private String wsName = "ws2179";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsName, session );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        int siteNum = ScmInfo.getSiteNum();
        ScmWorkspaceUtil.createWS( session, wsName, siteNum );
        deleteWorkspace( wsName, siteNum );

    }

    @AfterClass
    private void tearDown() {
        try {
            ScmWorkspaceUtil.deleteWs( wsName, session );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void deleteWorkspace( String wsName, int siteNum )
            throws Exception {
        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        rest.setRequestMethod( HttpMethod.DELETE )
                .setApi( "/workspaces/" + wsName ).exec();
        rest.disconnect();
        for ( int i = 0; i < 15; i++ ) {
            Thread.sleep( 1000 );
            try {
                ScmFactory.Workspace.getWorkspace( wsName, session );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_NOT_EXIST ) {
                    throw e;
                }
                TestSdbTools.Workspace.checkWsCs( wsName, session );
                return;
            }
        }
        Assert.fail( "delete ws is not done in 15 seconds: " );

    }

}
