package com.sequoiacm.s3.partupload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.PartUploadUtils;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @description SCM-4365:非桶管理用户终止分段上传
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4365 extends TestScmBase {
    private boolean runSuccess = false;
    private AmazonS3 s3ClientA;
    private AmazonS3 s3ClientB;
    private final String bucketName = "bucket4365";
    private final String username = "user4365";
    private File localPath;
    private ScmSession session = null;
    private File file;
    private final long fileSize = 5 * 1024 * 1024;

    @BeforeClass
    private void setUp() throws Exception {
        this.initFile();

        s3ClientA = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3ClientA, bucketName );
        s3ClientA.createBucket( new CreateBucketRequest( bucketName ) );

        // 新建用户连接
        session = TestScmTools.createSession( ScmInfo.getSite() );
        String password = "user4365password";
        ScmAuthUtils.createUser( session, username, password );
        String[] accessKeys = ScmAuthUtils.refreshAccessKey( session, username,
                password, null );
        s3ClientB = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        // userA upload part
        String key = "/aa/bb/obj4365";
        String uploadId = PartUploadUtils.initPartUpload( s3ClientA, bucketName,
                key );
        int maxPartNumber = 5;
        List< PartETag > partETags = PartUploadUtils.partUpload( s3ClientA,
                bucketName, key, uploadId, file, fileSize / maxPartNumber );

        // userB abort upload
        try {
            s3ClientB.abortMultipartUpload( new AbortMultipartUploadRequest(
                    bucketName, key, uploadId ) );
        } catch ( AmazonServiceException e ) {
            if ( !e.getErrorCode().equals( "AccessDenied" ) ) {
                Assert.fail( e.getMessage() );
            }
        }
        PartUploadUtils.listPartsAndCheckPartNumbers( s3ClientA, bucketName,
                key, partETags, uploadId );

        // userA abort again
        s3ClientA.abortMultipartUpload(
                new AbortMultipartUploadRequest( bucketName, key, uploadId ) );
        PartUploadUtils.checkAbortMultipartUploadResult( s3ClientA, bucketName,
                key, uploadId );

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
            ScmFactory.User.deleteUser( session, username );
            session.close();
        }
    }

    private void initFile() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );
    }
}