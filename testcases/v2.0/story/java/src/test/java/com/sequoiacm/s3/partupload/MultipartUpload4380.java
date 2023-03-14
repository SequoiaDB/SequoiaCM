package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;

/**
 * @description SCM-4380:非桶管理用户查询分段上传列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4380 extends TestScmBase {
    private boolean runSuccess = false;
    private final String bucketName = "bucket4380";
    private final String userName = "User4380";
    private AmazonS3 s3ClientA = null;
    private AmazonS3 s3ClientB = null;
    private ScmSession session = null;
    private File localPath = null;
    private File file = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        int fileSize = 1024 * 1024 * 10;
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );

        // 新建用户连接
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        String password = "user4380password";
        ScmAuthUtils.createUser( session, userName, password );
        String[] accessKeys = ScmAuthUtils.refreshAccessKey( session, userName,
                password, null );
        s3ClientB = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );

        s3ClientA = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3ClientA, bucketName );
        s3ClientA.createBucket( new CreateBucketRequest( bucketName ) );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void uploadParts() {
        String keyName = "/aa/object4380";
        String uploadId = PartUploadUtils.initPartUpload( s3ClientA, bucketName,
                keyName );
        PartUploadUtils.partUpload( s3ClientA, bucketName, keyName, uploadId,
                file );

        // the userB is not the bucket management user
        try {
            ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(
                    bucketName );
            s3ClientB.listMultipartUploads( request );
            Assert.fail( "exp fail but found success" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "AccessDenied",
                    "errorCode is " + e.getErrorCode() + "  statusCode:"
                            + e.getStatusCode() );
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3ClientA, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            s3ClientA.shutdown();
            s3ClientB.shutdown();
            ScmFactory.User.deleteUser( session, userName );
            session.close();
        }
    }
}
