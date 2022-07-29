package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4797 :: SCM创建多版本文件，S3指定版本删除版本文件
 * @author wuyan
 * @Date 2022.07.15
 * @version 1.00
 */
public class Object4797 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4797";
    private String keyName = "object4797";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 3;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private ScmSession session;
    private SiteWrapper site = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmId fileId = null;

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
        S3Utils.clearBucket( session, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = S3Utils.createFile( scmBucket, keyName, filePath );
        S3Utils.createFile( scmBucket, keyName, updatePath );
        S3Utils.createFile( scmBucket, keyName, filePath );
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void testCreateObject() throws Exception {
        String currentVersion = "3.0";
        String historyVersion1 = "2.0";
        String historyVersion2 = "1.0";

        // 场景a：指定最新版本删除
        s3Client.deleteVersion( bucketName, keyName, currentVersion );
        // 获取最新版本v3已不存在
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                currentVersion );
        try {
            s3Client.getObject( request );
            Assert.fail( "get object with deleteMarker should be fail!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
        }

        // 获取当前最新版本对象为原历史版本v2
        S3Object object = s3Client.getObject( bucketName, keyName );
        Assert.assertEquals( object.getObjectMetadata().getVersionId(),
                historyVersion1 );
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( updatePath ) );

        // 获取原历史版本v1存在
        s3Client.getObject(
                new GetObjectRequest( bucketName, keyName, historyVersion2 ) );

        // 场景 b：指定历史版本删除
        s3Client.deleteVersion( bucketName, keyName, historyVersion2 );
        // 获取最新版本v3已不存在
        GetObjectRequest request1 = new GetObjectRequest( bucketName, keyName,
                historyVersion2 );
        try {
            s3Client.getObject( request );
            Assert.fail( "get object  should be fail!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
        }
        // 校验最新版本对象还存在未删除
        Assert.assertTrue( s3Client.doesObjectExist( bucketName, keyName ) );
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
            if ( session != null ) {
                session.close();
            }
        }
    }
}
