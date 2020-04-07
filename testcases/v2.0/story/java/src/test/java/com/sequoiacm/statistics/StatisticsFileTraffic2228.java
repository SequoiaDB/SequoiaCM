package com.sequoiacm.statistics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * test content: read file by other site, statistics download file traffic
 * testlink-case:SCM-2228
 *
 * @author wuyan
 * @Date 2018.09.12
 * @version 1.00
 */
public class StatisticsFileTraffic2228 extends TestScmBase {
    private static SiteWrapper siteA = null;
    private static SiteWrapper siteB = null;
    private static WsWrapper wsp = null;
    private static ScmSession sessionA = null;
    private static ScmSession sessionB = null;
    private boolean runSuccess = false;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsB = null;

    private String fileName = "file12228";
    private String authorName = "author12228";
    private List< ScmId > fileIds = new ArrayList< ScmId >();
    private int fileNums = 20;
    private int fileSize = 1024 * 10;
    private byte[] fileData = new byte[ fileSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        List< SiteWrapper > sitelists = ScmInfo.getAllSites();
        siteA = sitelists.get( 0 );
        siteB = sitelists.get( 1 );
        sessionA = TestScmTools.createSession( siteA );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( siteB );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        //get statisticDownload before create file
        HashMap< String, Long > firstmap = StatisticsUtils
                .statisticsFile( wsA, sessionA );
        long statisticDownload1 = firstmap.get( "file_download" );

        createFiles( wsA, fileNums );
        downloadFile( wsB );

        //get statisticDownload after download
        HashMap< String, Long > secondmap = StatisticsUtils
                .statisticsFile( wsA, sessionA );
        long statisticDownload2 = secondmap.get( "file_download" );

        //check statisticDownload result,statistic for two sites download files
        long downloadFiles = statisticDownload2 - statisticDownload1;
        Assert.assertEquals( downloadFiles, fileNums + fileNums );

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
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void createFiles( ScmWorkspace ws, int fileNums )
            throws ScmException {
        long currentTimestamp = new Date().getTime();
        for ( int i = 0; i < fileNums; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = null;
            if ( i < 10 ) {
                //test a: the setCreateTime interval within one day
                long timestamp = currentTimestamp;
                fileId = StatisticsUtils.createFileByStream( ws, subfileName,
                        fileData, authorName, timestamp );
            }
            if ( i >= 10 && i < 15 ) {
                //test b: the setCreateTime interval within 5 days
                long timestamp = currentTimestamp - 432000000;
                fileId = StatisticsUtils.createFileByStream( ws, subfileName,
                        fileData, authorName, timestamp );
            }
            if ( i >= 15 && i < fileNums ) {
                //test c: the setCreateTime interval within
                long timestamp = currentTimestamp - 2678400000L;
                fileId = StatisticsUtils.createFileByStream( ws, subfileName,
                        fileData, authorName, timestamp );
            }
            fileIds.add( fileId );
        }
    }

    private void downloadFile( ScmWorkspace ws ) throws ScmException {
        for ( ScmId fileId : fileIds ) {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            // down file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            file.getContent( outputStream );
        }
    }
}