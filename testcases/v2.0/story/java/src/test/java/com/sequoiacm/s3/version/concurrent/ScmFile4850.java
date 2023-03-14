package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.VersionUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description: SCM-4850 :: 相同文件并发关联到不同桶
 * @author wuyan
 * @Date 2022.07.22
 * @version 1.00
 */
public class ScmFile4850 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketNameA = "bucket4850a";
    private String bucketNameB = "bucket4850b";
    private String fileName = "scmfile4850";
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
    private ScmBucket scmBucketA = null;
    private ScmBucket scmBucketB = null;

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
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketNameA );
        S3Utils.clearBucket( session, bucketNameB );
        scmBucketA = ScmFactory.Bucket.createBucket( ws, bucketNameA );
        scmBucketA.enableVersionControl();
        scmBucketB = ScmFactory.Bucket.createBucket( ws, bucketNameB );
        scmBucketB.enableVersionControl();
        fileId = ScmFileUtils.create( ws, fileName, filePath );
        VersionUtils.updateContentByFile( ws, fileName, fileId, updatePath );
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        AttachFileInBucketA attachFile1 = new AttachFileInBucketA(
                bucketNameA );
        AttachFileInBucketB attachFile2 = new AttachFileInBucketB(
                bucketNameB );
        es.addWorker( attachFile1 );
        es.addWorker( attachFile2 );
        es.run();

        if ( attachFile1.getRetCode() == 0 ) {
            // FILE_IN_ANOTHER_BUCKET: -270
            Assert.assertEquals( attachFile2.getRetCode(), -270,
                    attachFile2.getThrowable().getMessage() );
            checkAttachFileResult( scmBucketA, scmBucketB );
        } else {
            Assert.assertEquals( attachFile2.getRetCode(), 0 );
            Assert.assertEquals( attachFile1.getRetCode(), -270,
                    attachFile1.getThrowable().getMessage() );
            checkAttachFileResult( scmBucketB, scmBucketA );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketNameA );
                S3Utils.clearBucket( session, bucketNameB );
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

    private class AttachFileInBucketA extends ResultStore {
        private String bucketName;

        private AttachFileInBucketA( String bucketName ) {
            this.bucketName = bucketName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmFactory.Bucket.attachFile( session, bucketName, fileId );

            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            } finally {
                session.close();
            }
        }
    }

    private class AttachFileInBucketB extends ResultStore {
        private String bucketName;

        private AttachFileInBucketB( String bucketName ) {
            this.bucketName = bucketName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmFactory.Bucket.attachFile( session, bucketName, fileId );

            } catch ( ScmException e ) {
                saveResult( e.getErrorCode(), e );
            } finally {
                session.close();
            }
        }
    }

    private void checkAttachFileResult( ScmBucket scmBucket,
            ScmBucket noFileBucket ) throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;
        ScmFile curFile = scmBucket.getFile( fileName );
        Assert.assertEquals( curFile.getMajorVersion(), currentVersion );
        Assert.assertEquals( curFile.getBucketId().longValue(),
                scmBucket.getId() );
        Assert.assertEquals( curFile.getFileId(), fileId );
        S3Utils.checkFileContent( curFile, updatePath, localPath );

        ScmFile hisFile = scmBucket.getFile( fileName, historyVersion, 0 );
        Assert.assertEquals( hisFile.getFileId(), fileId );
        Assert.assertEquals( hisFile.getBucketId().longValue(),
                scmBucket.getId() );
        S3Utils.checkFileContent( hisFile, filePath, localPath );

        // 未关联成功的桶查询文件数量为0
        Assert.assertEquals( noFileBucket.countFile( null ), 0 );
    }

}
