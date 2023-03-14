package com.sequoiacm.s3.object.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
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
import java.util.HashMap;
import java.util.Map;

/**
 * @Descreption SCM-4248:SCM API更新自由标签，S3接口并发删除文件
 * @Author YiPan
 * @CreateDate 2022/5/13
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4248 extends TestScmBase {
    private final String bucketName = "bucket4248";
    private final String objectKey = "object4248";
    private ScmSession session;
    private AmazonS3 s3Client;
    private ScmWorkspace ws;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private ScmFile file;
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
        session = ScmSessionUtils.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        // 创建桶
        s3Client.createBucket( bucketName );

        // 创建自由标签元数据,创建对象
        Map< String, String > map = new HashMap<>();
        map.put( "test", "sequoiadb" );
        map.put( "num", "100" );
        ObjectMetadata meta = new ObjectMetadata();
        meta.setUserMetadata( map );
        PutObjectRequest putObjectRequest = new PutObjectRequest( bucketName,
                objectKey, new File( filePath ) ).withMetadata( meta );
        s3Client.putObject( putObjectRequest );

        // 并发更新和删除对象
        ScmId fileId = S3Utils.queryS3Object( ws, objectKey );
        file = ScmFactory.File.getInstance( ws, fileId );
        ThreadExecutor te = new ThreadExecutor();
        ScmUpdateCustomMetadata update = new ScmUpdateCustomMetadata();
        te.addWorker( update );
        te.addWorker( new S3DeleteObject() );
        te.run();

        // 再次上传同名对象
        s3Client.putObject( bucketName, objectKey, new File( filePath ) );

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

    private class ScmUpdateCustomMetadata {

        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            try {
                Map< String, String > map = new HashMap<>();
                map.put( "update", "new" );
                map.put( "num", "111" );
                file.setCustomMetadata( map );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND
                        .getErrorCode() ) {
                    throw e;
                }
            }
        }
    }

    private class S3DeleteObject {

        @ExecuteOrder(step = 1)
        private void run() throws Exception {
            AmazonS3 s3Client = S3Utils.buildS3Client();
            try {
                s3Client.deleteObject( bucketName, objectKey );
            } finally {
                s3Client.shutdown();
            }
        }
    }
}