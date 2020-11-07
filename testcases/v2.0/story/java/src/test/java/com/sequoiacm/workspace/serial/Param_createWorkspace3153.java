package com.sequoiacm.workspace.serial;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Description: SCM-3153:createWorkspace参数校验
 * @author fanyu
 * @Date:2020/11/5
 * @version:1.0
 */

public class Param_createWorkspace3153 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
    }

    // bug : SEQUOIACM-597
    @Test(groups = { "oneSite", "twoSite", "fourSite" },enabled = false)
    private void test1() throws ScmException, InterruptedException {
        String wsName = "工作区3153 .!@#$*()_<>test";
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( ScmWorkspaceUtil.getDataLocationList( ScmInfo.getSiteNum() ) );
        conf.setMetaLocation(ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setName( wsName );
        conf.setBatchIdTimeRegex( ".*" );
        conf.setBatchShardingType( ScmShardingType.YEAR );
        conf.setBatchIdTimePattern( "yyyyMMdd" );
        conf.setBatchFileNameUnique( true );
        // 创建工作区
        ScmFactory.Workspace.createWorkspace( session, conf );
        ScmWorkspaceUtil.wsSetPriority( session,wsName );
        // 获取工作区
        ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace( wsName, session );
        Assert.assertEquals(ws2.getName(), wsName);
        // 删除工作区
        ScmFactory.Workspace.deleteWorkspace( session, wsName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test2() throws ScmException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( ScmWorkspaceUtil.getDataLocationList( ScmInfo.getSiteNum() ) );
        conf.setMetaLocation(ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setBatchIdTimeRegex( ".*" );
        conf.setBatchShardingType( ScmShardingType.YEAR );
        conf.setBatchIdTimePattern( "yyyyMMdd" );
        conf.setBatchFileNameUnique( true );

        String[] chars = { "/", "%", "\\", ";" ,":"};
        for ( String c : chars ) {
            try {
                conf.setName( "ws3153" + c);
                Assert.fail( "exp fail but act success!!! c = " + c );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                    throw e;
                }
            }
        }

        try {
            ScmFactory.Workspace.createWorkspace( null, conf );
            Assert.fail( "exp fail but act success!!! ");
        }catch ( ScmException e ){
            if(e.getError() != ScmError.INVALID_ARGUMENT){
                throw e;
            }
        }

        try {
            ScmFactory.Workspace.createWorkspace( session, null );
            Assert.fail( "exp fail but act success!!!" );
        }catch ( ScmException e ){
            if(e.getError() != ScmError.INVALID_ARGUMENT){
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        session.close();
    }
}
