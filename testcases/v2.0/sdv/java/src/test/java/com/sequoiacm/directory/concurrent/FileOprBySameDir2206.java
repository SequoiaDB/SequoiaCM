package com.sequoiacm.directory.concurrent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmDirUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:create/delete/update files concurrently under the same dirctory
 * testlink-case:SCM-2206
 *
 * @author wuyan
 * @Date 2018.09.07
 * @version 1.00
 */
public class FileOprBySameDir2206 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private String fullPath =
            "/CreatefileWiteDir2206/2206_a/2206_b/2206_c/2206_e/2207_f/";
    private String authorName = "file2206";
    private ScmDirectory scmDir;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private byte[] writeData = new byte[ 1024 * 2 ];
    private byte[] updateData = new byte[ 1024 * 3 ];
    private LinkedBlockingDeque< ScmId > fileIdQue = new LinkedBlockingDeque<
            ScmId >();

    @BeforeClass()
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        wsp = ScmInfo.getWs();

        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        ScmDirUtils.deleteDir( ws, fullPath );

        scmDir = ScmDirUtils.createDir( ws, fullPath );
        new Random().nextBytes( writeData );
        int fileNums = 40;
        createFile( ws, writeData, scmDir, fileNums );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        List< CreateFileThread > createFiles = new ArrayList<>( 20 );
        UpdateFileThread updateFiles = new UpdateFileThread();
        DeleteFileThread deleteFiles = new DeleteFileThread();
        new Random().nextBytes( updateData );

        for ( int i = 0; i < 20; i++ ) {
            String fileName = "file2206a_" + i;
            createFiles.add( new CreateFileThread( fileName ) );
        }

        for ( CreateFileThread createFile : createFiles ) {
            createFile.start();
        }

        updateFiles.start( 20 );
        deleteFiles.start( 20 );

        for ( CreateFileThread createFile : createFiles ) {
            Assert.assertTrue( createFile.isSuccess(),
                    createFile.getErrorMsg() );
        }

        Assert.assertTrue( updateFiles.isSuccess(), updateFiles.getErrorMsg() );
        Assert.assertTrue( deleteFiles.isSuccess(), deleteFiles.getErrorMsg() );

        checkFileNumByDir( ws );
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                for ( int i = 0; i < fileIdQue.size(); i++ ) {
                    ScmId fileId = fileIdQue.take();
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                ScmDirUtils.deleteDir( ws, fullPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileContent( ScmWorkspace ws, ScmId fileId,
            byte[] filedata ) throws Exception {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        // down file
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        file.getContent( outputStream );
        byte[] downloadData = outputStream.toByteArray();

        // check results
        VersionUtils.assertByteArrayEqual( downloadData, filedata );
    }

    private void createFile( ScmWorkspace ws, byte[] data, ScmDirectory dir,
            int fileNums ) throws ScmException {
        String fileName = "file2206";
        for ( int i = 0; i < fileNums; i++ ) {
            String fileNameSub = fileName + "_" + i;
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( new ByteArrayInputStream( data ) );
            file.setFileName( fileNameSub );
            file.setAuthor( authorName );
            file.setTitle( "sequoiacm" );
            file.setMimeType( fileName + ".txt" );
            if ( dir != null ) {
                file.setDirectory( dir );
            }
            ScmId fileId = file.save();
            fileIdQue.offer( fileId );
        }
    }

    private void checkFileNumByDir( ScmWorkspace ws )
            throws ScmException, InterruptedException {
        int remainFileNum = 40;
        ScmDirectory dir = ScmFactory.Directory.getInstance( ws, fullPath );
        ScmCursor< ScmFileBasicInfo > cursor = dir.listFiles( null );
        int filenums = 0;
        while ( cursor.hasNext() ) {
            cursor.getNext();
            filenums++;
        }
        cursor.close();
        Assert.assertEquals( filenums, remainFileNum );
    }

    private void checkFileDir( ScmWorkspace ws, ScmId fileId,
            ScmDirectory scmDir ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getDirectory().toString(),
                scmDir.toString() );
    }

    private class CreateFileThread extends TestThreadBase {
        String fileName;

        public CreateFileThread( String fileName ) {
            this.fileName = fileName;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getSite() );
            try {
                ScmWorkspace wsTmp = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( wsTmp );
                file.setContent( new ByteArrayInputStream( writeData ) );
                file.setFileName( fileName );
                file.setAuthor( authorName );
                file.setTitle( "sequoiacm" );
                file.setMimeType( fileName + ".txt" );
                file.setDirectory( scmDir );
                ScmId fileId = file.save();

                //check file content
                checkFileContent( wsTmp, fileId, writeData );
                checkFileDir( wsTmp, fileId, scmDir );
                fileIdQue.offer( fileId );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getSite() );
            try {
                ScmId fileId = fileIdQue.take();
                ScmWorkspace wsTmp = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( wsTmp, fileId );
                file.updateContent( new ByteArrayInputStream( updateData ) );

                //check file content
                checkFileContent( wsTmp, fileId, updateData );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class DeleteFileThread extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getSite() );
            try {
                ScmId fileId = fileIdQue.take();
                ScmWorkspace wsTmp = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.File.deleteInstance( wsTmp, fileId, true );
                // the file is no exist
                try {
                    ScmFactory.File.getInstance( wsTmp, fileId );
                    Assert.fail( "get file must bu fail!" );
                } catch ( ScmException e ) {
                    //System.out.println("---remove file and get error is
                    // :"+e.getError());
                    if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                        Assert.fail( "expError:FILE NOT FOUND  actError:" +
                                e.getError() + e.getMessage() );
                    }
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
