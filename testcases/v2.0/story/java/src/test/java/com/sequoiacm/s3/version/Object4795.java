package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
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
 * @Description SCM-4795 :: SCM创建多版本文件，S3获取版本文件
 * @author wuyan
 * @Date 2022.07.14
 * @version 1.00
 */
public class Object4795 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4795";
    private String keyName = "object4795";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024 * 3;
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
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        fileId = ScmFileUtils.createFile( scmBucket, keyName, filePath );
        ScmFileUtils.createFile( scmBucket, keyName, updatePath );
        s3Client = S3Utils.buildS3Client();
    }

    @Test(groups = { GroupTags.base })
    public void testCreateObject() throws Exception {
        String fileMd5_his = TestTools.getMD5( filePath );
        String fileMd5_cur = TestTools.getMD5( updatePath );
        String currentVersion = "2.0";
        String historyVersion = "1.0";

        // testa：不指定版本获取文件，检查属性和内容
        S3Object objectInfo = s3Client.getObject( bucketName, keyName );
        checkObjectAttributeInfo( objectInfo, currentVersion, updateSize,
                fileMd5_cur );
        // 检查对象内容
        String downfileMd5 = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName );
        Assert.assertEquals( downfileMd5, fileMd5_cur );

        // testb：指定最新版本获取文件，检查属性和内容
        S3Object currObject = s3Client.getObject(
                new GetObjectRequest( bucketName, keyName, currentVersion ) );
        checkObjectAttributeInfo( currObject, currentVersion, updateSize,
                fileMd5_cur );
        String downfileMd5_cur = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, currentVersion );
        Assert.assertEquals( downfileMd5_cur, fileMd5_cur );

        // testc：指定历史版本获取文件，检查属性和内容
        S3Object hisObject = s3Client.getObject(
                new GetObjectRequest( bucketName, keyName, historyVersion ) );
        checkObjectAttributeInfo( hisObject, historyVersion, fileSize,
                fileMd5_his );
        String downfileMd5_his = S3Utils.getMd5OfObject( s3Client, localPath,
                bucketName, keyName, historyVersion );
        Assert.assertEquals( downfileMd5_his, fileMd5_his );
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
        Assert.assertEquals( objectMeta.getVersionId(), version );
        Assert.assertEquals( objectMeta.getContentLength(), size );
        Assert.assertEquals( objectMeta.getETag(), expMd5 );    }
}
