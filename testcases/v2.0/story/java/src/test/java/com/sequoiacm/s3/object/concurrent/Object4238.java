package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
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
 * @Descreption SCM-4238:S3接口和SCM API并发更新S3文件
 * @Author YiPan
 * @CreateDate 2022/5/16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4238 extends TestScmBase {
    private final String bucketName = "bucket4238";
    private final String objectKey = "object4238";
    private AmazonS3 s3Client;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private String s3UpdateFilePath = null;
    private String scmUpdateFilePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        s3UpdateFilePath = localPath + File.separator + "s3UpdateFile_"
                + fileSize + ".txt";
        scmUpdateFilePath = localPath + File.separator + "scmUpdateFile_"
                + fileSize + ".txt";
        downloadPath = localPath + File.separator + "downloadPath" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( s3UpdateFilePath, fileSize );
        TestTools.LocalFile.createFile( scmUpdateFilePath, fileSize );

        s3Client = S3Utils.buildS3Client( ScmInfo.getRootSite().getSiteName() );
        S3Utils.clearBucket( s3Client, bucketName );
    }

    // TODO：SEQUOIACM-847 s3更新文件会修改fileId
    @Test(enabled = false)
    public void test() throws Exception {
        // 创建桶，创建s3文件
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, objectKey, new File( filePath ) );

        // 并发更新文件内容
        ThreadExecutor te = new ThreadExecutor();
        te.addWorker( new ScmUpdateFile() );
        te.addWorker( new S3UpdateObject() );
        te.run();

        // 下载文件校验数据
        S3Object object = s3Client.getObject( bucketName, objectKey );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        String actMd5 = TestTools.getMD5( downloadPath );
        if ( !( actMd5.equals( TestTools.getMD5( scmUpdateFilePath ) ) ) ) {
            Assert.assertEquals( actMd5, TestTools.getMD5( s3UpdateFilePath ) );
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

    private class ScmUpdateFile extends ResultStore {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            ScmSession session = TestScmTools
                    .createSession( ScmInfo.getRootSite() );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces,
                    session );
            try {
                ScmId scmId = S3Utils.queryS3Object( ws, objectKey );
                ScmFile file = ScmFactory.File.getInstance( ws, scmId );
                file.updateContent( scmUpdateFilePath );
            } finally {
                session.close();
            }
        }
    }

    private class S3UpdateObject extends ResultStore {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.putObject( bucketName, objectKey,
                        new File( s3UpdateFilePath ) );
            } finally {
                s3Client.shutdown();
            }
        }
    }
}