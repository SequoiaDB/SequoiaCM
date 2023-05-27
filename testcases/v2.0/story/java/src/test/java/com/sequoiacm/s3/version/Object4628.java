package com.sequoiacm.s3.version;

import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @Description SCM-4628 :: 更新桶状态为禁用（enable->suspended），增加同名对象
 * @author wuyan
 * @Date 2022.07.07
 * @version 1.00
 */
public class Object4628 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "aa/bb/object4628";
    private String bucketName = "bucket4628";
    private AmazonS3 s3Client = null;
    private String createDatas = "create datas in the object4628";
    private String upateDatas = "update datas in the object4628------------";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
    }

    @Test
    public void testCreateObject() throws Exception {
        s3Client.putObject( bucketName, keyName, createDatas );
        Thread.sleep(1000);
        Date expFirstCreateTime = new Date();

        // 设置桶为禁用后创建同名对象
        S3Utils.setBucketVersioning( s3Client, bucketName, "Suspended" );
        s3Client.putObject( bucketName, keyName, upateDatas );
        Thread.sleep(1000);
        Date expSecondCreateTime = new Date();

        String historyVersionId = "1.0";
        String currentVersionId = "null";
        checkObjectResult( historyVersionId, expFirstCreateTime, createDatas );
        checkObjectResult( currentVersionId, expSecondCreateTime, upateDatas );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket(s3Client, bucketName);
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkObjectResult( String versionId, Date expDate,
            String expContent ) throws Exception {
        S3Object object = s3Client.getObject(
                new GetObjectRequest( bucketName, keyName, versionId ) );

        // check create time
        ObjectMetadata metadata = object.getObjectMetadata();
        Date actCreateDate = metadata.getLastModified();
        if ( actCreateDate.after( expDate ) ) {
            Assert.fail( "object create time is different! versionid is "
                    + metadata.getVersionId() + ",the actCreateDate is : "
                    + actCreateDate.toString() + ",the expDate is : "
                    + expDate.toString() );
        }

        // check object content by md5
        S3ObjectInputStream s3is = object.getObjectContent();
        byte[] bytes = new byte[ s3is.available() ];
        s3is.read( bytes );
        String actContent = new String( bytes );
        s3is.close();
        Assert.assertEquals( actContent, expContent );
    }
}
