package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4884 :: 禁用版本控制，并发更新和获取文件列表
 * @author Zhaoyujing
 * @Date 2020/7/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4884 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4884";
    private String keyName = "aa/bb/object4884";
    private int fileSize = 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private int updateSize = 1024 * 20;

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

        s3Client = S3Utils.buildS3Client();

        session = TestScmTools.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.suspendVersionControl();
        S3Utils.createFile( bucket, keyName, filePath );
    }

    @Test
    public void testCreateBucket() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new CreateFileThread() );
        te.addWorker( new GetObjectThread() );
        te.run();

        checkFileList();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }

            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkFileList() throws Exception {
        VersionListing versionListing = s3Client.listVersions( bucketName,
                null );
        Assert.assertEquals( versionListing.getVersionSummaries().size(), 1 );
        Assert.assertEquals(
                versionListing.getVersionSummaries().get( 0 ).getVersionId(),
                "null" );
        if ( !versionListing.getVersionSummaries().get( 0 ).isDeleteMarker() ) {
            String objectMD5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, keyName );
            String fileMD5 = TestTools.getMD5( updatePath );
            Assert.assertEquals( objectMD5, fileMD5 );
        }

    }

    private class CreateFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                    bucketName );
            S3Utils.createFile( bucket, keyName, updatePath );
        }
    }

    private class GetObjectThread extends ResultStore {
        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            try {
                S3Object object = s3Client.getObject( bucketName, keyName );
                long size = object.getObjectMetadata().getContentLength();
                S3ObjectInputStream s3is = object.getObjectContent();
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                S3Utils.inputStream2File( s3is, downloadPath );
                s3is.close();
                if ( size == fileSize ) {
                    // 读取第一次创建文件
                    Assert.assertEquals( TestTools.getMD5( downloadPath ),
                            TestTools.getMD5( filePath ) );
                } else {
                    // 读取第二次创建文件
                    Assert.assertEquals( size, updateSize );
                    Assert.assertEquals( TestTools.getMD5( downloadPath ),
                            TestTools.getMD5( updatePath ) );
                }
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError().getErrorType(),
                        "FILE_NOT_FOUND" );
            }
        }
    }
}
