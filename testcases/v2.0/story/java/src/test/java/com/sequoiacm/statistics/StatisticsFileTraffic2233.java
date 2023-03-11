package com.sequoiacm.statistics;

import java.io.ByteArrayInputStream;
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

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType.DatasourceType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;
import com.sequoiacm.testresource.SkipTestException;

/**
 * test content: create file, update file by breakpointfile, statistics file
 * delta testlink-case:SCM-2233
 * 
 * @author wuyan
 * @Date 2018.09.13
 * @version 1.00
 */

public class StatisticsFileTraffic2233 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "file12233";
    private String authorName = "author12233";
    private List< ScmId > fileIds = new ArrayList< ScmId >();
    private int fileNums = 10;
    private int fileSize = 1024 * 2;
    private int updateSize = 1024 * 3;
    private byte[] fileData = new byte[ fileSize ];
    private byte[] updateData = new byte[ updateSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
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
            throw new SkipTestException(
                    "no site are connected to sequoiadb datasourse, skip!" );
        }

        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        // get file_delta before create file
        HashMap< String, Long > firstmap = StatisticsUtils
                .statisticsFileDelta( ws, session );
        long count_delta1 = firstmap.get( "count_delta" );
        long size_delta1 = firstmap.get( "size_delta" );

        createFiles( ws, fileNums );
        updateFileByBreakpointfile( ws );

        // get file_delta after create file
        HashMap< String, Long > secondmap = StatisticsUtils
                .statisticsFileDelta( ws, session );
        long count_delta2 = secondmap.get( "count_delta" );
        long size_delta2 = secondmap.get( "size_delta" );

        // check statistics result
        Assert.assertEquals( count_delta2 - count_delta1, fileNums,
                "count_delta must be the filenums!" );
        Assert.assertEquals( size_delta2 - size_delta1, updateSize * fileNums,
                "size_delta is error!" );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                for ( ScmId fileId : fileIds ) {
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

    private void createFiles( ScmWorkspace ws, int fileNums )
            throws ScmException, IOException {
        new Random().nextBytes( fileData );
        for ( int i = 0; i < fileNums; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmBreakpointFile breakpointFile = createBreakpointFile( ws,
                    subfileName, fileData );
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( breakpointFile );
            file.setAuthor( authorName );
            file.setFileName( subfileName );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private void updateFileByBreakpointfile( ScmWorkspace ws )
            throws ScmException, IOException {
        for ( int i = 0; i < fileIds.size(); i++ ) {
            String subfileName = fileName + "_update_" + i;
            ScmId fileId = fileIds.get( i );
            ScmBreakpointFile breakpointFile = createBreakpointFile( ws,
                    subfileName, updateData );

            // update file by breakpointfile
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            file.updateContent( breakpointFile );
        }
    }

    private ScmBreakpointFile createBreakpointFile( ScmWorkspace ws,
            String fileName, byte[] data ) throws ScmException, IOException {
        // create breakpointfile
        ScmChecksumType checksumType = ScmChecksumType.CRC32;
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, checksumType );
        new Random().nextBytes( data );
        breakpointFile.upload( new ByteArrayInputStream( data ) );
        return breakpointFile;
    }

}