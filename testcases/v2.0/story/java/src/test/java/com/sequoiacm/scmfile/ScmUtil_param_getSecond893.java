package com.sequoiacm.scmfile;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmUtil;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-893:getSecond参数校验
 * @author huangxiaoni init
 * @date 2017.8.23
 */

public class ScmUtil_param_getSecond893 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private boolean runSuccess3 = false;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "ScmUtil_param_getSecond893";
    private String author = fileName;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName );
            file.setAuthor( fileName );
            fileId = file.save();
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testNomal() {
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( ScmUtil.Id.getSecond( fileId ),
                    file.getCreateTime().getTime() / 1000 );

            Assert.assertEquals( ScmUtil.Id
                    .getSecond( new ScmId( "0000000000000300012c1947" ) ), 0 );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testFileIdNotExist() {
        try {
            ScmUtil.Id.getSecond( new ScmId( "test" ) );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.assertEquals( e.getError(), ScmError.INVALID_ID,
                    e.getMessage() );
        }
        runSuccess2 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testFileIdIsNull() {
        try {
            ScmUtil.Id.getSecond( null );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT,
                    e.getMessage() );
        }
        runSuccess3 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( ( runSuccess1 && runSuccess2 && runSuccess3 )
                    || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            session.close();

        }
    }

}