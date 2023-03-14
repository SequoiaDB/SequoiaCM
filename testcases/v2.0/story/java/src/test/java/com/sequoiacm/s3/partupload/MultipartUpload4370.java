package com.sequoiacm.s3.partupload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartETag;
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
import java.util.List;

/**
 * @description SCM-4370:非桶管理用户查询分段列表
 * @author ZhangYanan
 * @createDate 2022.05.17
 * @updateUser ZhangYanan
 * @updateDate 2022.05.17
 * @updateRemark
 * @version v1.0
 */

public class MultipartUpload4370 extends TestScmBase {
    private boolean runSuccess = false;
    private String username = "user4370";
    private String password = "user4370password";
    private String bucketName = "bucket4370";
    private String keyName = "key4370";
    private AmazonS3 s3ClientA = null;
    private AmazonS3 s3ClientB = null;
    private ScmSession session = null;
    private long fileSize = 10 * 1024 * 1024;
    private File localPath = null;
    private File file = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        file = new File( filePath );

        // 创建用户 A
        s3ClientA = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3ClientA, bucketName );
        s3ClientA.createBucket( new CreateBucketRequest( bucketName ) );

        // 创建用户 B
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );

        ScmAuthUtils.createUser( session, username, password );
        String[] accessKeys = ScmAuthUtils.refreshAccessKey( session, username,
                password, null );
        s3ClientB = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );
    }

    @Test
    public void testListParts() throws Exception {
        // 用户A分段上传对象
        String uploadId = PartUploadUtils.initPartUpload( s3ClientA, bucketName,
                keyName );
        List< PartETag > partEtags = PartUploadUtils.partUpload( s3ClientA,
                bucketName, keyName, uploadId, file );

        // 用户B执行查询分段列表
        ListPartsRequest request = new ListPartsRequest( bucketName, keyName,
                uploadId );
        try {
            s3ClientB.listParts( request );
            Assert.fail( "list parts by other user should fail." );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "AccessDenied" );
        }

        PartUploadUtils.completeMultipartUpload( s3ClientA, bucketName, keyName,
                uploadId, partEtags );

        // 检查结果
        String expMd5 = TestTools.getMD5( filePath );
        String downloadMd5 = S3Utils.getMd5OfObject( s3ClientA, localPath,
                bucketName, keyName );
        Assert.assertEquals( downloadMd5, expMd5 );
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
            ScmFactory.User.deleteUser( session, username );
            s3ClientA.shutdown();
            s3ClientB.shutdown();
            session.close();
        }
    }
}
