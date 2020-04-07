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

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
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
 * test content: create file, statistics  file numbers delta and size delta.
 * *
 * testlink-case:SCM-2231
 *
 * @author wuyan
 * @Date 2018.09.13
 * @version 1.00
 */

public class StatisticsFileTraffic2231 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;

    private String fileName = "file12231";
    private String authorName = "author12231";
    private List< ScmId > fileIds = new ArrayList< ScmId >();
    private int fileNums = 15;
    private int fileSize = 1024 * 2;
    private byte[] fileData = new byte[ fileSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        wsp = ScmInfo.getWs();
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        //get file_delta before create file
        HashMap< String, Long > firstmap = StatisticsUtils
                .statisticsFileDelta( ws, session );
        long count_delta1 = firstmap.get( "count_delta" );
        long size_delta1 = firstmap.get( "size_delta" );

        createFiles( ws, fileNums );

        //get file_delta after create file
        HashMap< String, Long > secondmap = StatisticsUtils
                .statisticsFileDelta( ws, session );
        long count_delta2 = secondmap.get( "count_delta" );
        long size_delta2 = secondmap.get( "size_delta" );

        //check statistic file_delta result
        Assert.assertEquals( count_delta2 - count_delta1, fileNums,
                "count_delta must be the filenums!" );
        Assert.assertEquals( size_delta2 - size_delta1, fileSize * fileNums,
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
            throws ScmException {
        new Random().nextBytes( fileData );
        for ( int i = 0; i < fileNums; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = StatisticsUtils.createFileByStream( ws, subfileName,
                    fileData, authorName );
            fileIds.add( fileId );
        }
    }

}