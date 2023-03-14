package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
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
 * @Descreption SCM-4241:S3接口下载文件和SCM API更新文件并发
 * @Author YiPan
 * @CreateDate 2022/5/13
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4241 extends TestScmBase {
    private final String bucketName = "bucket4241";
    private final String objectKey = "object4241";
    private AmazonS3 s3Client;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private String updateFilePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updateFilePath = localPath + File.separator + "updateFile_" + fileSize
                + ".txt";
        downloadPath = localPath + File.separator + "downloadFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updateFilePath, fileSize * 2 );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        // 创建桶
        s3Client.createBucket( bucketName );

        // 上传对象
        s3Client.putObject( bucketName, objectKey, new File( filePath ) );

        // 并发更新和下载对象
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new ScmUpdateContent() );
        te.addWorker( new S3GetObject() );
        te.run();

        // 校验s3接口下载的对象
        if ( !TestTools.getMD5( downloadPath )
                .equals( TestTools.getMD5( filePath ) ) ) {
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( updateFilePath ) );
        }
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
            s3Client.shutdown();
        }
    }

    private class ScmUpdateContent {

        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = ScmSessionUtils
                    .createSession( ScmInfo.getRootSite() );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces,
                    session );
            try {
                ScmId scmId = S3Utils.queryS3Object( ws, objectKey );
                ScmFile file = ScmFactory.File.getInstance( ws, scmId );
                file.updateContent( updateFilePath );
            } finally {
                session.close();
            }
        }
    }

    private class S3GetObject {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                S3Object object = s3Client.getObject( bucketName, objectKey );
                S3Utils.inputStream2File( object.getObjectContent(),
                        downloadPath );
            } finally {
                s3Client.shutdown();
            }
        }
    }
}