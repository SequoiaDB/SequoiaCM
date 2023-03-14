package com.sequoiacm.statistics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: statistics upload and download file traffic, when create file
 * and down file testlink-case:SCM-2236
 *
 * @author wuyan
 * @Date 2018.09.13
 * @version 1.00
 */

public class StatisticsFileTraffic2236 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "file12236";
    private String authorName = "author12236";
    private LinkedBlockingDeque< ScmId > fileIdQue = new LinkedBlockingDeque< ScmId >();
    private int fileNums = 20;
    private int fileSize = 1024 * 10;
    private byte[] fileData = new byte[ fileSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        List< CreateAndReadFile > createAndReadFiles = new ArrayList<>( 20 );
        RefreshAndStatistics refreshAndStatistics = new RefreshAndStatistics();
        new Random().nextBytes( fileData );
        for ( int i = 0; i < fileNums; i++ ) {
            String subfileName = fileName + "_" + i;
            createAndReadFiles.add( new CreateAndReadFile( subfileName ) );
        }

        for ( CreateAndReadFile createAndReadFile : createAndReadFiles ) {
            createAndReadFile.start();
        }
        refreshAndStatistics.start( 20 );

        for ( CreateAndReadFile createAndReadFile : createAndReadFiles ) {
            Assert.assertTrue( createAndReadFile.isSuccess(),
                    createAndReadFile.getErrorMsg() );
        }
        Assert.assertTrue( refreshAndStatistics.isSuccess(),
                refreshAndStatistics.getErrorMsg() );

        // check the create file nums
        Assert.assertEquals( fileIdQue.size(), fileNums );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                for ( int i = 0; i < fileNums; i++ ) {
                    ScmId fileId = fileIdQue.take();
                    System.out.println( "---file=" + fileId + "  i=" + i );
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

    public class CreateAndReadFile extends TestThreadBase {
        String fileName;

        public CreateAndReadFile( String fileName ) {
            this.fileName = fileName;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( new ByteArrayInputStream( fileData ) );
                file.setFileName( fileName );
                file.setAuthor( authorName );
                ScmId fileId = file.save();
                fileIdQue.offer( fileId );

                // down file
                ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                file1.getContent( outputStream );

                // check the file content
                VersionUtils.CheckFileContentByStream( ws, fileName, 1,
                        fileData );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    public class RefreshAndStatistics extends TestThreadBase {
        @SuppressWarnings("rawtypes")
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                HashMap trafficInfo = StatisticsUtils.statisticsFile( ws,
                        session );
                long statisticDownload = ( long ) trafficInfo
                        .get( "file_download" );
                long statisticUpload = ( long ) trafficInfo
                        .get( "file_upload" );

                Assert.assertNotEquals( statisticDownload, 0,
                        "download file cannot be 0!" );
                Assert.assertNotEquals( statisticUpload, 0,
                        "upload file cannot be 0!" );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}