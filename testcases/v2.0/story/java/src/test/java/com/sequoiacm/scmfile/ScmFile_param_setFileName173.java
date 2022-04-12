package com.sequoiacm.scmfile;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
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
 * @descreption SCM-173:setFileName参数校验
 * @author huangxiaoni init
 * @date 2017.4.12
 * @updateUser YiPan
 * @updateDate 2022/4/12
 * @updateRemark
 * @version 1.0
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
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( author ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testFileNameIsLongStr() throws ScmException {
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
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test2() throws ScmException {
        String fileName = " file173中文!@#$()._test+";
        // 创建
        ScmFile file1 = ScmFactory.File.createInstance( ws );
        file1.setFileName( fileName );
        ScmId fileId = file1.save();
        // 获取
        ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file2.getFileName(), fileName );

        ScmFile file3 = ScmFactory.File.getInstanceByPath( ws, "/" + fileName );
        Assert.assertEquals( file3.getFileName(), fileName );
        Assert.assertEquals( ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                        .is( fileName ).get() ),
                1 );

        // 删除
        ScmFactory.File.deleteInstance( ws, fileId, true );
        Assert.assertEquals( ScmFactory.File.countInstance( ws,
                ScmType.ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                        .is( fileName ).get() ),
                0 );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test3() throws ScmException {
        String[] invalid = { "/", "//", };
        for ( String c : invalid ) {
            try {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( "file173" + c );
                file.save();
                Assert.fail( "expect fail but success," + file.toString() );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                    throw e;
                }
            }
        }

        String[] valid = { "\\a", "*", "?", "<", ">", "|", "\"", ":", "%",
                ";" };
        try {
            for ( String c : valid ) {
                System.err.println( c );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( "file173" + c );
                file.setAuthor( author );
                file.save();
            }
        } finally {
            BSONObject query = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, query );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess1 || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}