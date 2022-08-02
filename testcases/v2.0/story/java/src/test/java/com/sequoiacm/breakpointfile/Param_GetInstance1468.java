package com.sequoiacm.breakpointfile;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
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
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-1468:getInstance接口参数校验
 * @author fanyu
 * @Date:2020/11/27
 * @version:1.0
 */

public class Param_GetInstance1468 extends TestScmBase {
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBDataSource();
        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test1() throws ScmException {
        String fileName = "测试1468";
        // 获取不存在的文件
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            Assert.assertTrue( e.getMessage().contains( fileName ) );
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                throw e;
            }
        }

        // fileName为null
        try {
            ScmFactory.BreakpointFile.getInstance( ws, null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        // ws为null
        try {
            ScmFactory.BreakpointFile.getInstance( null, fileName );
            Assert.fail( "exp fail but act success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }
}
