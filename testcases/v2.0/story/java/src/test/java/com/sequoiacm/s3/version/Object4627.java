package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * @Description SCM-4627 :: 桶禁用版本控制，增加同名对象
 * @author wuyan
 * @Date 2022.07.07
 * @version 1.00
 */
public class Object4627 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "aa/bb/object4627";
    private String bucketName = "";
    private AmazonS3 s3Client = null;
    private String createDatas = "create datas in the object4627";
    private String upateDatas = "update datas in the object4627------------";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        bucketName = TestScmBase.susVerBucketName;
        s3Client.putObject( bucketName, keyName, createDatas );
    }

    @Test(groups = { GroupTags.base })
    public void testCreateObject() throws Exception {
        s3Client.putObject( bucketName, keyName, createDatas );
        Date expDate1 = new Date();
        checkPutObjectResult( expDate1, createDatas );
        s3Client.putObject( bucketName, keyName, upateDatas );
        Date expDate2 = new Date();
        checkPutObjectResult( expDate2, upateDatas );

        try {
            GetObjectRequest request = new GetObjectRequest( bucketName,
                    keyName, "1.0" );
            s3Client.getObject( request );
            Assert.fail( "get object should be fail!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "NoSuchVersion",
                    e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        keyName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
        }
    }

    private void checkPutObjectResult( Date expDate, String expContent )
            throws Exception {
        S3Object object = s3Client.getObject( bucketName, keyName );
        // check object update by create time
        ObjectMetadata metadata = object.getObjectMetadata();
        Date actCreateDate = metadata.getLastModified();
        if ( actCreateDate.after( expDate ) ) {
            Assert.fail( "create time is different! the actCreateDate is : "
                    + actCreateDate.toString() + ",the expDate is : "
                    + expDate.toString() );
        }
        Assert.assertEquals( metadata.getVersionId(), "null" );

        // check object content by md5
        S3ObjectInputStream s3is = object.getObjectContent();
        byte[] bytes = new byte[ s3is.available() ];
        s3is.read( bytes );
        String actContent = new String( bytes );
        s3is.close();
        Assert.assertEquals( actContent, expContent );
    }
}
