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
 * @FileName SCM-291: 并发在不同中心查询文件
 * @Author linsuqiang
 * @Date 2017-05-24
 * @Version 1.00
 */

/*
 * 1、并发在不同中心查询文件，查询条件不同（覆盖多种匹配符组合查询）； 2、检查查询结果正确性；
 */

public class Count_onMultiCenter291 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private int totalSiteNum = 2 + 1;
    private ScmSession sessionM = null;
    private ScmSession sessionA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsM = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsB = null;

    private String fileName = "count291";
    private String author = fileName;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileNum = 30; // on each site

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

            sessionA = TestScmTools.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            sessionB = TestScmTools.createSession( branSites.get( 1 ) );
            wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );

            int flag = 0;
            prepareScmFile( wsM, 0 );
            prepareScmFile( wsA, flag + 1 );
            prepareScmFile( wsB, flag + 2 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            QueryThread qryThd1 = new QueryThread( rootSite );
            QueryThread qryThd2 = new QueryThread( branSites.get( 0 ) );
            QueryThread qryThd3 = new QueryThread( branSites.get( 1 ) );

            qryThd1.start();
            qryThd2.start();
            qryThd3.start();

            if ( !( qryThd1.isSuccess() && qryThd2.isSuccess() &&
                    qryThd3.isSuccess() ) ) {
                Assert.fail( qryThd1.getErrorMsg() + qryThd2.getErrorMsg() +
                        qryThd3.getErrorMsg() );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsA, fileId, true );
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null )
                sessionM.close();
            if ( sessionA != null )
                sessionA.close();
            if ( sessionB != null )
                sessionB.close();

        }
    }

    private void prepareScmFile( ScmWorkspace ws, int flag ) {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                String str = "291_" + flag + "_" + String.format( "%03d", i );
                ScmFile scmfile = ScmFactory.File.createInstance( ws );
                scmfile.setFileName( fileName + "_" + flag + "_" + i );
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
        private SiteWrapper site = null;

        public QueryThread( SiteWrapper site ) {
            this.site = site;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            int totalFileNum = fileNum * totalSiteNum;
            int countNum = totalFileNum - 1;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                String key = ScmAttributeName.File.TITLE;
                String startVal = ScmFactory.File
                        .getInstance( ws, fileIdList.get( 0 ) ).getTitle();
                String endVal = ScmFactory.File
                        .getInstance( ws, fileIdList.get( countNum ) )
                        .getTitle();

                BSONObject fileCond = ScmQueryBuilder.start().and( key )
                        .greaterThanEquals( startVal ).and( key )
                        .lessThan( endVal ).and( "notexistkey" ).exists( 0 )
                        .get();

                long actCount = ScmFactory.File
                        .countInstance( ws, ScopeType.SCOPE_CURRENT, fileCond );
                Assert.assertEquals( actCount, countNum,
                        "fileCond = " + fileCond.toString() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
