package com.sequoiacm.querybuilder;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-378: is多个相同字段
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、countInstance带查询条件查询文件，对多个相同字段做is（给key添加指定value）匹配查询；
 * 2、检查ScmQueryBuilder结果正确性； 3、检查查询结果正确性；
 */

public class Is378 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private static String fileName = "Is378";
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private ScmWorkspace ws = null;
    private ScmFile file = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileNum = 3;
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

            readyScmFile();
            file = ScmFactory.File.getInstance( ws, fileIdList.get( 2 ) );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testQueryByExistCond() throws Exception {
        try {
            // build condition
            String key = ScmAttributeName.File.FILE_NAME;
            String value = file.getFileName();

            BSONObject cond = ScmQueryBuilder.start( key ).is( 111 ).put( key )
                    .is( value ).get();

            String expCond = "{ \"" + key + "\" : \"" + value + "\"}";
            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    expCond.replaceAll( "\\s*", "" ) );

            // count
            long count = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( count, 1 );

            runSuccess1 = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testQueryByNotExistCond() throws Exception {
        try {
            // build condition
            String key = ScmAttributeName.File.CREATE_TIME;
            long value = file.getCreateTime().getTime();

            BSONObject cond = ScmQueryBuilder.start( key ).is( value )
                    .put( "k2" ).is( " " ).get();

            String expCond = "{ \"" + key + "\" : " + value + " , "
                    + "\"k2\" : \" \"}";
            Assert.assertEquals( cond.toString().replaceAll( "\\s*", "" ),
                    expCond.replaceAll( "\\s*", "" ) );

            // count
            long count = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( count, 0 );

            runSuccess2 = true;
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( ( runSuccess1 && runSuccess2 ) || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.getInstance( ws, fileId ).delete( true );
                }
            }
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void readyScmFile() {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile scmfile = ScmFactory.File.createInstance( ws );
                scmfile.setFileName(
                        fileName + "_" + TestTools.getRandomString( 10 ) + i );
                scmfile.setAuthor( author );
                ScmId fileId = scmfile.save();
                fileIdList.add( fileId );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

}