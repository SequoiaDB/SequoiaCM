package com.sequoiacm.s3.bucket;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @descreption SCM-5021 :: createBucket接口校验,
 *              长度3-63，有效规则"^[a-z0-9][a-z0-9._-]+[a-z0-9]$"
 * @author Zhaoyujing
 * @Date 2020/7/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Bucket5021 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket5021";
    private String bucketNameB = "5021bucket";
    private String bucketNameC = "cub";

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @DataProvider(name = "illegalBucketNameProvider")
    public Object[][] generateIllegalBucketName() {
        return new Object[][] {
                new Object[] { null },
                new Object[] { "" },
                new Object[] { "bu" },
                new Object[] {
                        "abcdefghigklmnopqrstuvwxyz-abcdefghigklmnopqrstuvwxyz-1234567890" },
                new Object[] { "-" + bucketName },
                new Object[] { "_" + bucketName },
                new Object[] { "." + bucketName },
                new Object[] { bucketName + "-" },
                new Object[] { bucketName + "_" },
                new Object[] { bucketName + "." } };
    }

    @DataProvider(name = "legalBucketNameProvider")
    public Object[][] generateLegalBucketName() {
        return new Object[][] { new Object[] { bucketNameC },
                new Object[] { bucketName },
                new Object[] { bucketName
                        + "abcdefghigklmnopqrstuvwxyz-abcdefghigklmnopqrstuvwxyz" },
                new Object[] { bucketName + "-" + bucketName + "_" + bucketName
                        + "." + bucketName },
                new Object[] { bucketNameB + "-" + bucketNameB + "_"
                        + bucketNameB + "." + bucketNameB }, };
    }

    @Test(dataProvider = "illegalBucketNameProvider")
    public void testIllegalBucketName( String illegalName ) {
        try {
            ScmFactory.Bucket.createBucket( ws, illegalName );
            Assert.fail(
                    "Create bucket: " + illegalName + " should be failed" );
        } catch ( ScmException e ) {
            System.out.println( "Create bucket: " + illegalName + ", error:"
                    + e.getError().getErrorDescription() );
        }

        runSuccess = true;
    }

    @Test(dataProvider = "legalBucketNameProvider")
    public void testLegalBucketName( String illegalName ) throws ScmException {
        ScmFactory.Bucket.createBucket( ws, illegalName );
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );
                S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketNameB );
                S3Utils.clearBucket( s3Client, bucketNameC );
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
