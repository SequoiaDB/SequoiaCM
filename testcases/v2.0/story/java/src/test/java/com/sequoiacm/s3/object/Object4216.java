package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Descreption SCM-4216:SCM API创建S3文件，S3接口删除文件
 * @Author YiPan
 * @CreateDate 2022/5/12
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4216 extends TestScmBase {
    private final String bucketName = "bucket4216";
    private final String objectKey = "object4224";
    private ScmSession session;
    private AmazonS3 s3Client;
    private int fileSize = 1024 * 300;
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
        session = ScmSessionUtils.createSession( rootSite );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        // SCM API 创建文件
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        ScmFile file = bucket.createFile( objectKey );
        file.setContent( filePath );
        file.save();

        // 删除文件
        s3Client.deleteObject( bucketName, objectKey );

        // 通过s3和SCM API校验删除成功
        Assert.assertFalse( s3Client.doesObjectExist( bucketName, objectKey ) );
        Assert.assertEquals( bucket.countFile( null ), 0 );
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

}