package com.sequoiacm.statistics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
 * test content: statistics upload and download file traffic
 * testlink-case:SCM-2226
 *
 * @author wuyan
 * @Date 2018.09.12
 * @version 1.00
 */

public class StatisticsFileTraffic2226 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "file12226";
    private String authorName = "author12226";
    private List< ScmId > fileIds = new ArrayList< ScmId >();
    private int fileNums = 20;
    private int fileSize = 1024 * 10;
    private byte[] fileData = new byte[ fileSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        //get statisticDownload before create file
        HashMap< String, Long > firstmap = StatisticsUtils
                .statisticsFile( ws, session );
        long statisticDownload1 = firstmap.get( "file_download" );
        long statisticUpload1 = firstmap.get( "file_upload" );

        createFiles( ws, fileNums );
        downloadFile( ws );
        deleteFile( ws );

        //get statisticUpload after download
        HashMap< String, Long > secondmap = StatisticsUtils
                .statisticsFile( ws, session );
        long statisticDownload2 = secondmap.get( "file_download" );
        long statisticUpload2 = secondmap.get( "file_upload" );

        //check statistic file download and upload result
        long upFiles = statisticUpload2 - statisticUpload1;
        Assert.assertEquals( upFiles, fileNums );
        long downloadFiles = statisticDownload2 - statisticDownload1;
        Assert.assertEquals( downloadFiles, fileNums );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                        .get();
                ScmFileUtils.cleanFile( wsp, cond );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createFiles( ScmWorkspace ws, int fileNums )
            throws ScmException {
        for ( int i = 0; i < fileNums; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = StatisticsUtils
                    .createFileByStream( ws, subfileName, fileData,
                            authorName );
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

    private void deleteFile( ScmWorkspace ws ) throws ScmException {
        for ( ScmId fileId : fileIds ) {
            ScmFactory.File.deleteInstance( ws, fileId, true );
        }
    }

}