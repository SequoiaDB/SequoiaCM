package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
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
 * @Descreption SCM-4236:S3接口和SCM API并发创建文件
 * @Author YiPan
 * @CreateDate 2022/5/16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4236 extends TestScmBase {
    private final String bucketName = "bucket4236";
    private final String objectKey = "object4236";
    private ScmSession session;
    private AmazonS3 s3Client;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        SiteWrapper rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    // SEQUOIACM-1007
    @Test(enabled = false)
    public void test() throws Exception {
        // 创建桶
        s3Client.createBucket( bucketName );

        // 并发上传相同文件
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new ScmCreateFile() );
        te.addWorker( new S3CreateObject() );
        te.run();

        // 校验数据
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        S3Object object = s3Client.getObject( bucketName, objectKey );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            session.close();
            s3Client.shutdown();
        }
    }

    private class ScmCreateFile {

        @ExecuteOrder(step = 1)
        private void run() {
            try {
                ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile file = bucket.createFile( objectKey );
                file.setContent( filePath );
                file.save();
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_EXIST.getErrorCode() ) {
                }
            }
        }
    }

    private class S3CreateObject {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.putObject( bucketName, objectKey,
                        new File( filePath ) );
            } finally {
                s3Client.shutdown();
            }
        }
    }
}