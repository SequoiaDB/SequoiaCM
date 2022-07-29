package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description: SCM-4869 :: 开启版本控制，并发创建相同文件
 * @author wuyan
 * @Date 2022.07.20
 * @version 1.00
 */
public class Object4869 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4869";
    private String keyName = "key4869";
    private int fileSize = 1024 * 500;
    private int updateSize = 1024 * 300;
    private ScmId fileId = null;
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
        s3Client = S3Utils.buildS3Client();

    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        S3CreateObject s3CreateObject = new S3CreateObject( keyName );
        SCMCreateObject scmCreateObject = new SCMCreateObject( keyName );
        es.addWorker( s3CreateObject );
        es.addWorker( scmCreateObject );
        es.run();

        checkResult();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private class S3CreateObject {
        private String keyName;

        private S3CreateObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            s3Client.putObject( bucketName, keyName, new File( filePath ) );
        }
    }

    private class SCMCreateObject {
        String keyName;

        private SCMCreateObject( String keyName ) {
            this.keyName = keyName;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            fileId = S3Utils.createFile( scmBucket, keyName, updatePath );
        }
    }

    private void checkResult() throws Exception {
        String currentVersion = "2.0";
        String historyVersion = "1.0";
        int scmCurVersion = 2;
        int scmHisVersion = 1;
        String file1Md5 = TestTools.getMD5( filePath );
        String file2Md5 = TestTools.getMD5( updatePath );

        S3Object object = s3Client.getObject( bucketName, keyName );
        ObjectMetadata objectMeta = object.getObjectMetadata();
        String newVersion = objectMeta.getVersionId();
        Assert.assertEquals( newVersion, currentVersion );
        long actSize = objectMeta.getContentLength();
        if ( actSize == fileSize ) {
            Assert.assertEquals( objectMeta.getETag(), file1Md5 );
            Assert.assertEquals( object.getBucketName(), bucketName );
            // 检查对象内容
            String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, keyName );
            Assert.assertEquals( downfileMd5, file1Md5 );

            // 获取历史版本信息,校验结果
            getObjectAndCheckResult( historyVersion, file2Md5, updateSize );
            getFileAttrAndCheckResult( scmHisVersion, updateSize,
                    object.getKey() );
        } else {
            Assert.assertEquals( actSize, updateSize );
            Assert.assertEquals( objectMeta.getETag(), file2Md5 );
            Assert.assertEquals( object.getBucketName(), bucketName );
            // 检查对象内容
            String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                    bucketName, keyName );
            Assert.assertEquals( downfileMd5, file2Md5 );

            // 获取历史版本信息,校验结果
            getObjectAndCheckResult( historyVersion, file1Md5, fileSize );
            getFileAttrAndCheckResult( scmHisVersion, fileSize, "" );
        }
    }

    private void getObjectAndCheckResult( String version, String fileMd5,
            long size ) throws Exception {
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                version );
        S3Object object = s3Client.getObject( request );
        ObjectMetadata objectMeta = object.getObjectMetadata();
        Assert.assertEquals( objectMeta.getContentLength(), size );
        Assert.assertEquals( objectMeta.getETag(), fileMd5 );
        Assert.assertEquals( object.getBucketName(), bucketName );
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, version );
        Assert.assertEquals( downfileMd5, fileMd5 );
    }

    private void getFileAttrAndCheckResult( int version, long size,
            String title ) throws ScmException {
        ScmFile file = scmBucket.getFile( keyName, version, 0 );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getSize(), size );
        Assert.assertEquals( file.getTitle(), title );
    }
}
