package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Descreption SCM-4265:创建SCM文件，重复关联文件
 * @Author YiPan
 * @CreateDate 2022/5/17
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4265 extends TestScmBase {
    private final String bucketNameA = "bucket4265a";
    private final String bucketNameB = "bucket4265b";
    private final String objectKey = "object4265";
    private ScmSession session;
    private ScmWorkspace ws;
    private ScmId fileId;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private AmazonS3 s3Client;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        // 清理环境
        S3Utils.clearBucket( session, bucketNameA );
        S3Utils.clearBucket( session, bucketNameB );
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void test() throws Exception {
        ScmFactory.Bucket.createBucket( ws, bucketNameA );
        ScmFactory.Bucket.createBucket( ws, bucketNameB );
        S3Utils.updateBucketVersionConfig( s3Client, bucketNameA,
                BucketVersioningConfiguration.ENABLED );
        S3Utils.updateBucketVersionConfig( s3Client, bucketNameB,
                BucketVersioningConfiguration.ENABLED );

        // 创建文件关联到桶A
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( objectKey );
        file.setContent( filePath );
        fileId = file.save();
        ScmFactory.Bucket.attachFile( session, bucketNameA, fileId );

        // 重复关联到桶A SEQUOIACM-839优化了这里，这里重复关联不再报错
        ScmFactory.Bucket.attachFile( session, bucketNameA, fileId );

        // 重复关联到桶B
        try {
            ScmFactory.Bucket.attachFile( session, bucketNameB, fileId );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.FILE_IN_ANOTHER_BUCKET ) ) {
                throw e;
            }
        }
        // 重新创建同名文件关联
        file = ScmFactory.File.createInstance( ws );
        file.setFileName( objectKey );
        file.setContent( filePath );
        fileId = file.save();
        try {
            ScmFactory.Bucket.attachFile( session, bucketNameA, fileId );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.FILE_EXIST ) ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
                S3Utils.clearBucket( session, bucketNameA );
                S3Utils.clearBucket( session, bucketNameB );
            }
        } finally {
            s3Client.shutdown();
            session.close();
        }
    }
}