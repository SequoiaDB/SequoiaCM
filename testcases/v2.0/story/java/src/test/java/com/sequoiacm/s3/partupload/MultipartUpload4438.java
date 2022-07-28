package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @description SCM-4438:listMultipartUploads接口参数校验
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4438 extends TestScmBase {
    private AmazonS3 s3Client = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void testIllegalParameter() {
        // a.接口参数取值合法---已在功能测试中验证
        // b.接口参数取值非法校验，取值为null
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                null );
        try {
            s3Client.listMultipartUploads( request );
            Assert.fail( "when bucketName is null, it should fail." );
        } catch ( IllegalArgumentException e ) {
            Assert.assertEquals( e.getMessage(),
                    "The bucket name parameter must be specified when listing multipart uploads" );
        }
    }

    @AfterClass
    private void tearDown() {
        if ( s3Client != null ) {
            s3Client.shutdown();
        }
    }
}