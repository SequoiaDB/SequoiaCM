package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.List;

/**
 * @description SCM-4366:初始化分段上传后查询分段列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */
public class MultipartUpload4366 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3Client;
    private String bucketName = "bucket4366";
    private String key = "/aa/bb/obj4366";
    private String uploadId;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();

        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() {
        uploadId = PartUploadUtils.initPartUpload( s3Client, bucketName, key );
        ListPartsRequest request = new ListPartsRequest( bucketName, key,
                uploadId );
        PartListing partList = s3Client.listParts( request );
        List< PartSummary > parts = partList.getParts();
        Assert.assertEquals( parts.size(), 0 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {

                s3Client.abortMultipartUpload( new AbortMultipartUploadRequest(
                        bucketName, key, uploadId ) );
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }
}