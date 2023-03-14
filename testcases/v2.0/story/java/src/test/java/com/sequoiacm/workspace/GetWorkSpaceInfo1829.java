/**
 *
 */
package com.sequoiacm.workspace;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.element.bizconf.ScmHbaseDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description GetWorkSpaceInfo1829.java 获取Hbase数据源详细信息
 * @author luweikang
 * @date 2018年6月28日
 */
public class GetWorkSpaceInfo1829 extends TestScmBase {
    private String wsName = "ws1829";
    private ScmSession session = null;
    private SiteWrapper rootSite = null;

    @BeforeClass
    private void setUp() throws ScmException {

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
    }

    @Test(groups = { "one", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {

        ScmWorkspace ws = ScmWorkspaceUtil.createWS( session, wsName, 1 );
        List< ScmDataLocation > list = ws.getDataLocations();
        for ( ScmDataLocation scmDataLocation : list ) {
            System.out.println( scmDataLocation.getBSONObject() );
            if ( scmDataLocation.getType() == DatasourceType.HBASE ) {
                ScmHbaseDataLocation hd = ( ScmHbaseDataLocation ) scmDataLocation;
                System.out.println( hd.getShardingType() );
                System.out.println( hd.getType() );
                System.out.println( hd.getSiteName() );
            }
        }
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.Workspace.deleteWorkspace( session, wsName );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

}
