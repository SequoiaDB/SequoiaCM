package com.sequoiacm.statistics;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
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
 * test content: create file by breakpointfile, statistics upload file traffic,
 * and statistics upload file traffic again after delete file.
 * testlink-case:SCM-2227
 *
 * @author wuyan
 * @Date 2018.09.13
 * @version 1.00
 */

public class StatisticsFileTraffic2227 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "file12227";
    private String authorName = "author12227";
    private List< ScmId > fileIds = new ArrayList< ScmId >();
    private int fileNums = 10;
    private int fileSize = 1024 * 2;
    private byte[] fileData = new byte[ fileSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        // check env for breakpointfile
        List< SiteWrapper > sitelists = ScmInfo.getAllSites();
        int dbDataSoureCount = 0;
        for ( int i = 0; i < sitelists.size(); i++ ) {
            DatasourceType dataType = sitelists.get( i ).getDataType();
            if ( dataType.equals( DatasourceType.SEQUOIADB ) ) {
                site = sitelists.get( i );
                break;
            }
            dbDataSoureCount++;
        }
        if ( dbDataSoureCount == sitelists.size() ) {
            throw new SkipException(
                    "no site are connected to sequoiadb datasourse, skip!" );
        }

        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    // 问题单SEQUOIACM-1285未修改，暂时屏蔽用例
    @Test(groups = { GroupTags.base }, enabled = false)
    private void test() throws Exception {
        // get statisticUpload before create file
        HashMap< String, Long > firstmap = StatisticsUtils.statisticsFile( ws,
                session );
        long statisticUpload1 = firstmap.get( "file_upload" );

        // get statisticUpload after create file
        createFileByBreakpointfile( ws, fileNums );
        HashMap< String, Long > secondmap = StatisticsUtils.statisticsFile( ws,
                session );
        long statisticUpload2 = secondmap.get( "file_upload" );

        // get statisticUpload after delete
        deleteFile( ws );
        HashMap< String, Long > thirdmap = StatisticsUtils.statisticsFile( ws,
                session );
        long statisticUpload3 = thirdmap.get( "file_upload" );

        // check statisticUpload result
        long upFiles = statisticUpload2 - statisticUpload1;
        Assert.assertEquals( upFiles, fileNums,
                "traffic must be the same as filenums!" );
        Assert.assertEquals( statisticUpload3, statisticUpload2,
                "traffic should no change after delete file!" );
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

    private void deleteFile( ScmWorkspace ws ) throws ScmException {
        for ( ScmId fileId : fileIds ) {
            ScmFactory.File.deleteInstance( ws, fileId, true );
        }
    }

    private void createFileByBreakpointfile( ScmWorkspace ws, int fileNums )
            throws ScmException, IOException {
        new Random().nextBytes( fileData );
        for ( int i = 0; i < fileNums; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmBreakpointFile breakpointFile = createBreakpointFile( ws,
                    subfileName );

            // setcontent by breakfile
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( breakpointFile );
            file.setFileName( subfileName );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private ScmBreakpointFile createBreakpointFile( ScmWorkspace ws,
            String fileName ) throws ScmException, IOException {
        // create breakpointfile
        ScmChecksumType checksumType = ScmChecksumType.CRC32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, checksumType );
        new Random().nextBytes( fileData );
        breakpointFile.upload( new ByteArrayInputStream( fileData ) );
        return breakpointFile;
    }
}