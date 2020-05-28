package com.sequoiacm.net.version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:listInstance by the current /history version/ all scm file
 * testlink-case:SCM-1680/1681/1682
 *
 * @author wuyan
 * @Date 2018.06.12
 * @version 1.00
 */

public class ListInstanceByScope1680_1681_1682 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;

    private String fileName1 = "file1680";
    private String fileName2 = "file1681";
    private String authorName = "author1680";
    private byte[] writedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        fileId1 = VersionUtils.createFileByStream( ws, fileName1, writedata,
                authorName );
        fileId2 = VersionUtils.createFileByStream( ws, fileName2, writedata,
                authorName );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        VersionUtils.updateContentByStream( ws, fileId2, writedata );
        VersionUtils.updateContentByStream( ws, fileId2, updatedata );

        // testcase-1681b: no exist historyVersion file
        listInstanceByNoHistoryVersion( fileId1 );

        // testcase-1680:list current version file
        List< String > expectFileNames = new LinkedList< String >();
        expectFileNames.add( fileName1 );
        expectFileNames.add( fileName2 );
        listInstanceByCurrentVersion( expectFileNames );

        listInstanceByHistoryVersion( fileId2, fileName2 );

        listInstanceByAllVersion();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( ws, fileId1, true );
                ScmFactory.File.deleteInstance( ws, fileId2, true );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void listInstanceByNoHistoryVersion( ScmId fileId )
            throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_HISTORY, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            cursor.getNext();
            size++;
        }
        cursor.close();
        // no history version file
        Assert.assertEquals( size, 0 );
    }

    private void listInstanceByHistoryVersion( ScmId fileId, String fileName )
            throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_HISTORY, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            Assert.assertEquals( file.getFileName(), fileName );
            size++;
        }
        cursor.close();
        // exist 2 history version file
        Assert.assertEquals( size, 2 );
    }

    private void listInstanceByCurrentVersion( List< String > expectFileNames )
            throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        List< String > actFileNames = new LinkedList< String >();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            actFileNames.add( file.getFileName() );
            size++;
        }
        cursor.close();
        // exist 2 current version file
        int expFileNum = 2;
        Assert.assertEquals( size, expFileNum );
        Collections.sort( expectFileNames );
        Collections.sort( actFileNames );
        Assert.assertEquals( actFileNames, expectFileNames,
                "act fileName are :" + actFileNames.toString() );
    }

    private void listInstanceByAllVersion() throws ScmException {
        List< String > fileIdList = new ArrayList< String >();
        fileIdList.add( fileId1.get() );
        fileIdList.add( fileId2.get() );
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            cursor.getNext();
            size++;
        }
        cursor.close();
        // exist 2 current version file and 2 history version file
        int expFileNum = 4;
        Assert.assertEquals( size, expFileNum );
    }
}