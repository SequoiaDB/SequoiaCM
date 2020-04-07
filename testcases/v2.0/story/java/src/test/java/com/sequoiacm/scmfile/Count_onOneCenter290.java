package com.sequoiacm.scmfile;

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
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-290: 并发在同一个中心查询文件
 * @Author linsuqiang
 * @Date 2017-05-24
 * @Version 1.00
 */

/*
 * 1、并发在同一个中心查询文件，查询条件相同（覆盖多种匹配符组合查询）； 2、检查查询结果正确性；
 */

public class Count_onOneCenter290 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "count290";
    private String author = fileName;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileNum = 3;

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

            this.prepareScmFile( ws );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        ScmSession sessionB = null;
        try {
            QueryThread qryThd = new QueryThread();
            qryThd.start( 30 );

            if ( !qryThd.isSuccess() ) {
                Assert.fail( qryThd.getErrorMsg() );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void prepareScmFile( ScmWorkspace ws ) {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                String str = "290_" + i;
                ScmFile scmfile = ScmFactory.File.createInstance( ws );
                scmfile.setFileName( fileName + "_" + i );
                scmfile.setAuthor( author );
                scmfile.setTitle( str );
                scmfile.setMimeType( str );
                ScmId fileId = scmfile.save();
                fileIdList.add( fileId );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private class QueryThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                String key = ScmAttributeName.File.TITLE;
                String startVal = ScmFactory.File
                        .getInstance( ws, fileIdList.get( 0 ) ).getTitle();
                String endVal = ScmFactory.File
                        .getInstance( ws, fileIdList.get( fileNum - 1 ) )
                        .getTitle();

                BSONObject fileCond = ScmQueryBuilder.start().and( key )
                        .greaterThanEquals( startVal ).and( key )
                        .lessThan( endVal ).and( "notexistkey" ).exists( 0 )
                        .and( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();

                long actCount = ScmFactory.File
                        .countInstance( ws, ScopeType.SCOPE_CURRENT, fileCond );
                long expCount = fileNum - 1;
                Assert.assertEquals( actCount, expCount,
                        "fileCond = " + fileCond.toString() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
