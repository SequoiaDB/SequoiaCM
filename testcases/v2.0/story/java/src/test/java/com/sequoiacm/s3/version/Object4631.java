package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4631 :: 非桶管理用户增加对象
 * @author wuyan
 * @Date 2022.07.07
 * @version 1.00
 */
public class Object4631 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "aa/bb/object4631";
    private String bucketName = "bucket4631";
    private AmazonS3 s3Client = null;
    private AmazonS3 s3ClientA = null;
    private String expContent = "object_file4631";
    private String updateContent = "object_update_file4631";
    private ScmSession session = null;
    private String[] accessKeys = null;
    private String username = "user4631";
    private String password = "user_pw4631";
    private File localPath = null;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        // 新建用户连接
        session = TestScmTools.createSession( ScmInfo.getSite() );
        ScmAuthUtils.createUser( session, username, password );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, password,
                null );
        s3ClientA = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ] );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName, "Enabled" );
    }

    @Test
    public void testCreateObject() throws Exception {
        s3Client.putObject( bucketName, keyName, expContent );
        try {
            s3ClientA.putObject( bucketName, keyName, updateContent );
            Assert.fail( "---update file should be failed!" );
        } catch ( AmazonS3Exception e ) {
            Assert.assertEquals( e.getErrorCode(), "AccessDenied", "errorMsg: "
                    + e.getMessage() + ", errorType=" + e.getErrorType() );
        }

        // 检查对象内容未更新
        S3Object object = s3Client.getObject( bucketName, keyName );
        Assert.assertEquals( object.getObjectMetadata().getVersionId(), "1.0" );
        String actMd5 = S3Utils.getMd5OfObject( s3Client, localPath, bucketName,
                keyName );
        String expMd5 = TestTools.getMD5( expContent.getBytes() );
        Assert.assertEquals( actMd5, expMd5,
                "The md5 value of the current version is different." );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.deleteObjectAllVersions( s3Client, bucketName,
                        keyName );
                s3Client.deleteBucket( bucketName );
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.User.deleteUser( session, username );

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
