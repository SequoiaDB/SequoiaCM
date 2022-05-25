package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Descreption SCM-4280:使用S3接口创建S3文件，文件名包含特殊字符
 * @Author YiPan
 * @CreateDate 2022/5/18
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4280 extends TestScmBase {
    private final String bucketName = "bucket4280";
    private final String baseObjectKey = "object4280";
    private AmazonS3 s3Client;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private AtomicInteger actSuccessTestCount = new AtomicInteger( 0 );

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        // 清理环境
        s3Client = S3Utils.buildS3Client( ScmInfo.getRootSite().getSiteName() );
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[] characters() {
        return new String[] { "/", "\\", "%", ";", ":", "*", "?", "\"", "<",
                ">", "|" };
    }

    @Test(dataProvider = "dataProvider")
    public void test( String character ) throws Exception {
        String downloadPath = localPath + File.separator + UUID.randomUUID()
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( downloadPath );

        // 生成带字符的ObjectKey
        String objectKey = baseObjectKey + character;

        // 上传对象
        s3Client.putObject( bucketName, objectKey, new File( filePath ) );

        // 下载校验
        S3Object object = s3Client.getObject( bucketName, objectKey );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        actSuccessTestCount.getAndIncrement();
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( actSuccessTestCount.get() == characters().length ) {
                TestTools.LocalFile.removeFile( localPath );
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }
}