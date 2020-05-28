package com.sequoiacm.directory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:create a file under a dir,than update Content of the file,
 * delete file testlink-case:SCM-2064
 *
 * @author wuyan
 * @Date 2018.07.12
 * @version 1.00
 */

public class DeleteFileWithDir2064 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private List< String > fileIdList = new ArrayList< String >();
    private int fileNum = 10;

    private ScmDirectory scmDir;
    private String dirBasePath = "/CreatefileWiteDir2064";
    private String fullPath = dirBasePath
            + "/2064_a/2064_b/2064_c/2064_e/2064_f/";
    private String authorName = "CreateFileWithDir2064";
    private String fileName = "filedir2064";
    private byte[] writeData = new byte[ 1024 * 100 ];
    private byte[] updateData = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();

        session = TestScmTools.createSession( branSite );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        ScmDirUtils.deleteDir( ws, fullPath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        scmDir = ScmDirUtils.createDir( ws, fullPath );
        writeandUpdateFileWithDir( ws );
        checkWriteAndUpdateResult( ws );
        deleteFileAndCheckResult( ws );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmDirUtils.deleteDir( ws, fullPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void writeandUpdateFileWithDir( ScmWorkspace ws )
            throws ScmException {
        new Random().nextBytes( writeData );
        new Random().nextBytes( updateData );
        for ( int i = 0; i < fileNum; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = createFileWithDir( ws, subfileName, writeData,
                    authorName, scmDir );
            if ( i % 2 == 0 ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                file.updateContent( new ByteArrayInputStream( updateData ) );
            }
            fileIdList.add( fileId.get() );
        }
    }

    private void checkWriteAndUpdateResult( ScmWorkspace ws ) throws Exception {
        // check the current file,check the filesize
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            String fileName = file.getFileName();
            int majorVersion = file.getMajorVersion();
            // update file version is 2, no update file version is 1
            if ( majorVersion == 1 ) {
                VersionUtils.CheckFileContentByStream( ws, fullPath + fileName,
                        majorVersion, writeData );
            } else if ( majorVersion == 2 ) {
                VersionUtils.CheckFileContentByStream( ws, fullPath + fileName,
                        majorVersion, updateData );
            } else {
                Assert.fail( "the file majorVersion error!" );
            }
            size++;
        }
        cursor.close();
        int expFileNum = 10;
        Assert.assertEquals( size, expFileNum );

        // check the history file ,the history version is 1
        BSONObject condition1 = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();
        ScmCursor< ScmFileBasicInfo > cursor1 = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_HISTORY, condition1 );
        int size1 = 0;
        while ( cursor1.hasNext() ) {
            ScmFileBasicInfo file1 = cursor1.getNext();
            String fileName1 = file1.getFileName();
            VersionUtils.CheckFileContentByStream( ws, fullPath + fileName1, 1,
                    writeData );
            size1++;
        }
        cursor1.close();

        int expFileNum1 = 5;
        Assert.assertEquals( size1, expFileNum1 );
    }

    private void deleteFileAndCheckResult( ScmWorkspace ws )
            throws ScmException {
        for ( String fileId : fileIdList ) {
            ScmFactory.File.deleteInstance( ws, new ScmId( fileId ), true );
        }

        BSONObject fileCondition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).in( fileIdList ).get();

        long currentCount = ScmFactory.File.countInstance( ws,
                ScopeType.SCOPE_CURRENT, fileCondition );
        Assert.assertEquals( currentCount, 0 );
        long historyCount = ScmFactory.File.countInstance( ws,
                ScopeType.SCOPE_HISTORY, fileCondition );
        Assert.assertEquals( historyCount, 0 );
    }

    private ScmId createFileWithDir( ScmWorkspace ws, String fileName,
            byte[] data, String authorName, ScmDirectory dir )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( new ByteArrayInputStream( data ) );
        file.setFileName( fileName );
        file.setAuthor( authorName );
        file.setTitle( "sequoiacm" );
        if ( dir != null ) {
            file.setDirectory( dir );
        }
        file.setMimeType( fileName + ".txt" );
        ScmId fileId = file.save();
        return fileId;
    }
}