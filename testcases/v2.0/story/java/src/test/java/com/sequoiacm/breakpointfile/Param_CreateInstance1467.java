package com.sequoiacm.breakpointfile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * @Description:SCM-1467:createInstance接口参数校验
 * @author fanyu
 * @Date:2020/11/4
 * @version:1.0
 */

public class Param_CreateInstance1467 extends TestScmBase {
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
        String fileName = " file1467 中文.!@#$*()_+::<>\"test";
        // 创建
        ScmBreakpointFile breakpointFile1 = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        breakpointFile1.upload( new ByteArrayInputStream( new byte[ 0 ] ) );

        // 获取
        ScmBreakpointFile breakpointFile2 = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        Assert.assertEquals( breakpointFile2.getFileName(), fileName );

        // 删除
        ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        Assert.assertFalse( ScmFactory.BreakpointFile.listInstance( ws,
                ScmQueryBuilder
                        .start( ScmAttributeName.BreakpointFile.FILE_NAME )
                        .is( fileName ).get() )
                .hasNext() );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test2() throws ScmException {
        String[] chars = { "/", "%", "\\", ";" };
        for ( String c : chars ) {
            try {
                ScmBreakpointFile breakpointFile1 = ScmFactory.BreakpointFile
                        .createInstance( ws, "1467 " + c );
                breakpointFile1
                        .upload( new ByteArrayInputStream( new byte[ 0 ] ) );
                Assert.fail( "exp fail but act success!!! c = " + c );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                    throw e;
                }
            }
        }

        try {
            ScmFactory.BreakpointFile.createInstance( ws, null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }

        try {
            ScmFactory.BreakpointFile.createInstance( null, "1467" );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @AfterClass
    private void tearDown() {
        session.close();
    }
}
