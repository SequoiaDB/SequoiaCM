package com.sequoiacm.statistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * test content: transfer files, statistics download file traffic
 * testlink-case:SCM-2230
 *
 * @author wuyan
 * @Date 2018.09.12
 * @version 1.00
 */

public class StatisticsFileTraffic2230 extends TestScmBase {
    private static SiteWrapper sourceSite = null;
    private static SiteWrapper targetSite = null;
    private static WsWrapper wsp = null;
    private static ScmSession sessionA = null;
    private boolean runSuccess = false;
    private ScmWorkspace wsA = null;
    private ScmId taskId = null;
    private String fileName = "file12230";
    private String authorName = "author12230";
    private List< ScmId > fileIds = new ArrayList< ScmId >();
    private int fileNums = 5;
    private int fileSize = 1024 * 10;
    private byte[] fileData = new byte[ fileSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        List< SiteWrapper > siteList = ScmNetUtils.getSortSites( wsp );
        sourceSite = siteList.get( 0 );
        targetSite = siteList.get( 1 );

        sessionA = TestScmTools.createSession( sourceSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        createFiles( wsA, fileNums );

        // get statisticDownload after create file
        HashMap< String, Long > firstmap = StatisticsUtils.statisticsFile( wsA,
                sessionA );
        long statisticDownload1 = firstmap.get( "file_download" );
        transferFile( wsA, sessionA );

        // get statisticDownload after transfer file
        HashMap< String, Long > secondmap = StatisticsUtils.statisticsFile( wsA,
                sessionA );
        long statisticDownload2 = secondmap.get( "file_download" );

        // check statisticDownload result, statistic no download file
        Assert.assertEquals( statisticDownload2, statisticDownload1,
                "statistic traffic must be no change!" );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                for ( ScmId fileId : fileIds ) {
                    ScmFactory.File.deleteInstance( wsA, fileId, true );
                }
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void createFiles( ScmWorkspace ws, int fileNums )
            throws ScmException {
        new Random().nextBytes( fileData );
        for ( int i = 0; i < fileNums; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = StatisticsUtils.createFileByStream( ws, subfileName,
                    fileData, authorName );
            fileIds.add( fileId );
        }
    }

    private void transferFile( ScmWorkspace ws, ScmSession session )
            throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        taskId = ScmSystem.Task.startTransferTask( ws, condition,
                ScopeType.SCOPE_CURRENT, targetSite.getSiteName() );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, taskId );

    }

}