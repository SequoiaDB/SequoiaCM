package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-5013:更新桶版本控制开启版本控制，SCM创建多版本文件，S3获取版本文件
 * @author wuyan
 * @Date 2022.07.14
 * @version 1.00
 */
public class Object5013 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket5013";
    private String keyName = "object5013";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 3;
    private int updateSize = 1024;
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
        session = ScmSessionUtils.createSession( site );
        S3Utils.clearBucket( session, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        fileId = ScmFileUtils.createFile( scmBucket, keyName, filePath );
        scmBucket.enableVersionControl();
        ScmFile file = scmBucket.getFile( keyName );
        file.updateContent( updatePath );
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void testCreateObject() throws Exception {
        String fileMd5_his = TestTools.getMD5( filePath );
        String fileMd5_cur = TestTools.getMD5( updatePath );
        String currentVersion = "2.0";
        String historyVersion = "-2.0";

        // testa：指定最新版本获取文件，检查属性和内容
        S3Object currObject = s3Client.getObject(
                new GetObjectRequest( bucketName, keyName, currentVersion ) );
        checkObjectAttributeInfo( currObject, currentVersion, updateSize,
                fileMd5_cur );
        String downfileMd5_cur = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, currentVersion );
        Assert.assertEquals( downfileMd5_cur, fileMd5_cur );
        int curVersion = 2;
        checkFileMetaFromSCM( curVersion );

        // testb：指定历史版本获取文件，检查属性和内容
        S3Object hisObject = s3Client.getObject(
                new GetObjectRequest( bucketName, keyName, historyVersion ) );
        checkObjectAttributeInfo( hisObject, historyVersion, fileSize,
                fileMd5_his );
        String downfileMd5_his = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, historyVersion );
        Assert.assertEquals( downfileMd5_his, fileMd5_his );
        int hisVersion = -2;
        checkFileMetaFromSCM( hisVersion );

        // 指定null版本删除
        s3Client.deleteVersion( bucketName, keyName, historyVersion );
        // 获取null版本已不存在
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                historyVersion );
        try {
            s3Client.getObject( request );
            Assert.fail( "get object  should be fail!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchVersion" );
        }
        // 当前版本存在未删除
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

    private void checkObjectAttributeInfo( S3Object objectInfo, String version,
            long size, String expMd5 ) {
        Assert.assertEquals( objectInfo.getBucketName(), bucketName );
        ObjectMetadata objectMeta = objectInfo.getObjectMetadata();
        if ( version.equals( "-2.0" ) ) {
            Assert.assertEquals( objectMeta.getVersionId(), "null" );
        } else {
            Assert.assertEquals( objectMeta.getVersionId(), version );
        }
        Assert.assertEquals( objectMeta.getContentLength(), size );
        Assert.assertEquals( objectMeta.getETag(), expMd5 );
    }

    private void checkFileMetaFromSCM( int version ) throws ScmException {
        ScmFile file = scmBucket.getFile( keyName, version, 0 );
        Assert.assertEquals( file.getMajorVersion(), version );
        if ( version == -2 ) {
            Assert.assertTrue( file.isNullVersion() );
            Assert.assertEquals( file.getVersionSerial().getMajorSerial(), 1 );
        } else {
            Assert.assertFalse( file.isNullVersion() );
            Assert.assertEquals( file.getVersionSerial().getMajorSerial(),
                    version );
        }

    }
}
