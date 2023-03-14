package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4796 :: SCM创建多版本文件，S3不指定版本删除版本文件
 * @author wuyan
 * @Date 2022.07.15
 * @version 1.00
 */
public class Object4796 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4796";
    private String keyName = "object4796";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private int updateSize = 1024 * 1024 * 2;
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
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = ScmFileUtils.createFile( scmBucket, keyName, filePath );
        ScmFileUtils.createFile( scmBucket, keyName, updatePath );
        s3Client = S3Utils.buildS3Client();
    }

    @Test(groups = { GroupTags.base })
    public void testCreateObject() throws Exception {
        s3Client.deleteObject( bucketName, keyName );

        // 获取当前版本对象已被删除
        boolean isExistObject = s3Client.doesObjectExist( bucketName, keyName );
        Assert.assertFalse( isExistObject, "the object should not exist!" );

        // 获取当前版本中新增删除标记对象失败
        String deleteMarkerVersion = "3.0";
        GetObjectRequest request = new GetObjectRequest( bucketName, keyName,
                deleteMarkerVersion );
        try {
            s3Client.getObject( request );
            Assert.fail( "get object with deleteMarker should be fail!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "MethodNotAllowed" );
        }

        // 原删除版本移到历史版本中，获取文件内容正确
        String deleteVersion = "2.0";
        s3Client.getObject(
                new GetObjectRequest( bucketName, keyName, deleteVersion ) );
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, deleteVersion );
        Assert.assertEquals( downfileMd5, TestTools.getMD5( updatePath ) );

        // 获取原历史版本文件存在
        String historyVersion = "1.0";
        s3Client.getObject(
                new GetObjectRequest( bucketName, keyName, historyVersion ) );
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
