package com.sequoiacm.directory.serial;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:specify directory to create multiple files,than transfer and
 * clean file mathching directory testlink-case:SCM-2060
 *
 * @author wuyan
 * @Date 2018.07.12
 * @version 1.00
 */

public class TransferAndCleanFile2060 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId transferTaskId = null;
    private ScmId cleanTaskId = null;

    private List< String > fileIdList = new ArrayList< String >();
    private int fileNum = 10;

    private ScmDirectory scmDir;
    private String dirBasePath = "/CreatefileWiteDir2060";
    private String fullPath = dirBasePath
            + "/2060_a/2060_b/2060_c/2060_e/2060_f/";
    private String authorName = "CreateFileWithDir2060";
    private String fileName = "filedir2060";
    private byte[] writeData1 = new byte[ 1024 * 100 ];
    private byte[] writeData2 = new byte[ 1024 * 200 ];
    private BSONObject condition = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        ScmDirUtils.deleteDir( wsA, fullPath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        scmDir = ScmDirUtils.createDir( wsA, fullPath );
        writeFileWithDir( wsA );

        startTransferTask( wsA, sessionA );
        checkTransferResult( wsA );

        startCleanTask( wsA, sessionA );
        checkCleanResult( wsM );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                TestSdbTools.Task.deleteMeta( transferTaskId );
                TestSdbTools.Task.deleteMeta( cleanTaskId );
                for ( String fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsM, new ScmId( fileId ),
                            true );
                }
                ScmDirUtils.deleteDir( wsA, fullPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void writeFileWithDir( ScmWorkspace ws ) throws ScmException {
        new Random().nextBytes( writeData1 );
        new Random().nextBytes( writeData2 );

        for ( int i = 0; i < fileNum; i++ ) {
            String subfileName = fileName + "_" + i;
            ScmId fileId = null;
            if ( i % 2 == 0 ) {
                fileId = createFileWithDir( ws, subfileName, writeData1,
                        authorName, scmDir );
            } else {
                fileId = createFileWithDir( ws, subfileName, writeData2,
                        authorName, scmDir );
            }
            fileIdList.add( fileId.get() );
        }
    }

    private void startTransferTask( ScmWorkspace ws, ScmSession session )
            throws Exception {
        condition = ScmQueryBuilder.start( ScmAttributeName.File.DIRECTORY_ID )
                .is( scmDir.getId() ).put( ScmAttributeName.File.SIZE )
                .is( writeData1.length ).put( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        transferTaskId = ScmSystem.Task.startTransferTask( ws, condition );

        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, transferTaskId );
    }

    private void startCleanTask( ScmWorkspace ws, ScmSession session )
            throws Exception {
        cleanTaskId = ScmSystem.Task.startCleanTask( ws, condition );
        // wait task finish
        ScmTaskUtils.waitTaskFinish( session, cleanTaskId );
    }

    private void checkTransferResult( ScmWorkspace ws ) throws Exception {
        // check the transfered file,check the sitelist and data
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        int size = 0;
        ScmFileBasicInfo file;
        List< ScmId > transferfileIdList = new ArrayList< ScmId >();
        SiteWrapper[] expCurSiteList = { rootSite, branSite };
        while ( cursor.hasNext() ) {
            file = cursor.getNext();
            ScmId fileId = file.getFileId();
            ScmFileUtils.checkMeta( ws, fileId, expCurSiteList );
            checkFileContentByStream( ws, fileId, writeData1 );
            transferfileIdList.add( fileId );
            size++;
        }
        cursor.close();
        int expFileNum = 5;
        Assert.assertEquals( size, expFileNum );

        // check the no transfer file by current version
        BSONObject condition1 = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID )
                .is( scmDir.getId() ).put( ScmAttributeName.File.SIZE )
                .is( writeData2.length ).put( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmCursor< ScmFileBasicInfo > cursor1 = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_CURRENT, condition1 );
        int size1 = 0;
        SiteWrapper[] expCurSiteList1 = { branSite };
        while ( cursor1.hasNext() ) {
            ScmFileBasicInfo file1 = cursor1.getNext();
            ScmId fileId1 = file1.getFileId();
            ScmFileUtils.checkMeta( ws, fileId1, expCurSiteList1 );
            size1++;
        }
        cursor1.close();
        Assert.assertEquals( size1, expFileNum );
    }

    private void checkCleanResult( ScmWorkspace ws ) throws Exception {
        // check the cleaned file,check the sitelist and data
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScopeType.SCOPE_CURRENT, condition );
        int size = 0;
        ScmFileBasicInfo file;
        List< ScmId > transferfileIdList = new ArrayList< ScmId >();
        SiteWrapper[] expCurSiteList = { rootSite };
        while ( cursor.hasNext() ) {
            file = cursor.getNext();
            ScmId fileId = file.getFileId();
            ScmFileUtils.checkMeta( ws, fileId, expCurSiteList );
            checkFileContentByStream( ws, fileId, writeData1 );
            transferfileIdList.add( fileId );
            size++;
        }
        cursor.close();
        int expFileNum = 5;
        Assert.assertEquals( size, expFileNum );

        // check the no clean file
        BSONObject condition1 = ScmQueryBuilder
                .start( ScmAttributeName.File.DIRECTORY_ID )
                .is( scmDir.getId() ).put( ScmAttributeName.File.SIZE )
                .is( writeData2.length ).put( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmCursor< ScmFileBasicInfo > cursor1 = ScmFactory.File
                .listInstance( ws, ScopeType.SCOPE_CURRENT, condition1 );
        int size1 = 0;
        SiteWrapper[] expCurSiteList1 = { branSite };
        while ( cursor1.hasNext() ) {
            ScmFileBasicInfo file1 = cursor1.getNext();
            ScmId fileId1 = file1.getFileId();
            ScmFileUtils.checkMeta( ws, fileId1, expCurSiteList1 );
            size1++;
        }
        cursor1.close();
        Assert.assertEquals( size1, expFileNum );
    }

    private void checkFileContentByStream( ScmWorkspace ws, ScmId fileId,
            byte[] filedata ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        // down file
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        byte[] downloadData = outputStream.toByteArray();

        // check results
        VersionUtils.assertByteArrayEqual( downloadData, filedata );
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
        // add tags
        ScmTags tags = new ScmTags();
        tags.addTag(
                "我是一个标签2060                                                  "
                        + "                                                    "
                        + "                                                    "
                        + "                            "
                        + "                                " );
        tags.addTag( "THIS IS TAG 2060!" );
        tags.addTag( "tag *&^^^^^*90234@#$%!~asf" );
        file.setTags( tags );
        ScmId fileId = file.save();
        return fileId;
    }

}