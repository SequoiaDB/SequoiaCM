package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: SCM-4848 :: 开启版本控制，并发使用物理方式和非物理方式删除相同文件
 * @author wuyan
 * @Date 2022.07.21
 * @version 1.00
 */
public class ScmFile4848 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4848";
    private String fileName = "key4848";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = S3Utils.createFile( scmBucket, fileName, filePath );
        S3Utils.createFile( scmBucket, fileName, updatePath );
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        DeleteFileIsPhysical deleteFileIsPhysical = new DeleteFileIsPhysical(
                fileName );
        DeleteFile deleteFile = new DeleteFile( fileName );
        es.addWorker( deleteFileIsPhysical );
        es.addWorker( deleteFile );
        es.run();

        checkDeleteResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class DeleteFile {
        private String fileName;

        private DeleteFile( String fileName ) {
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                bucket.deleteFile( fileName, false );
            } finally {
                session.close();
            }
        }
    }

    private class DeleteFileIsPhysical {
        private String fileName;

        private DeleteFileIsPhysical( String fileName ) {
            this.fileName = fileName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( ScmInfo.getSite() );
                ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                bucket.deleteFile( fileName, true );
            } finally {
                session.close();
            }
        }
    }

    private void checkDeleteResult() throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        List< BSONObject > actFileNames = new ArrayList<>();
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            BSONObject keyInfo = new BasicBSONObject();
            keyInfo.put( file.getFileName(), file.getFileId().toString() );
            actFileNames.add( keyInfo );
            size++;
        }
        cursor.close();
        Assert.assertEquals( size, 0, actFileNames.toString() );
    }

}
