package com.sequoiacm.scmfile;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-173:setFileName参数校验
 * @author huangxiaoni init
 * @date 2017.4.12
 */

public class ScmFile_param_setFileName173 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess1 = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile173";
    private String author = fileName;
    private List< ScmId > fileIdList = new ArrayList<>();

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
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testFileNameIsLongStr() {
        try {
            int strLeagth = 950;
            String str = TestTools.getRandomString( strLeagth );

            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( str );
            file.setTitle( str );
            file.setAuthor( fileName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );

            // check results
            ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file2.getAuthor(), fileName );
            Assert.assertEquals( file2.getTitle(), str );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void testNameIsDot() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "." );
            file.save();
            Assert.fail( "expect fail but success," + file.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasSprit() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "//" );
            file.save();
            Assert.fail( "expect fail but success," + file.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasBackslash() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "\\" );
            file.save();
            Assert.fail( "expect fail but success," + file.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasStar() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "*" );
            file.save();
            Assert.fail( "expect fail but success," + file.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasQuestionMark() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "12?" );
            file.save();
            Assert.fail( "expect fail but success," + file.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasLessThanSign() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "qwer<" );
            file.save();
            Assert.fail( "expect fail but success," + file.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasGreatThanSign() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "qwer>" );
            file.save();
            Assert.fail( "expect fail but success," + file.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testHasOrSign() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "qwer|" );
            file.save();
            Assert.fail( "expect fail but success," + file.toString() );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess1 || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

}