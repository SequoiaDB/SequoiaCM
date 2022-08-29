package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;

/**
 * @descreption SCM-5024:使用SCM API重命名桶内文件
 * @author YiPan
 * @date 2022/8/5
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object5024 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket5024";
    private String objectKey = "object5024";
    private String newObjectKey = "object5024new";
    private ScmId fileId = null;

    @BeforeClass
    private void setUp() throws Exception {
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.updateBucketVersionConfig( bucketName,
                BucketVersioningConfiguration.ENABLED );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws ScmException, IOException {
        s3Client.putObject( bucketName, objectKey, "test" );
        fileId = S3Utils.queryS3Object( ws, objectKey );
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );

        // 桶内文件重命名失败
        try {
            file.setFileName( newObjectKey );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                throw e;
            }
        }

        // 解除关联，重命名成功
        ScmFactory.Bucket.detachFile( session, bucketName, objectKey );
        file.setFileName( newObjectKey );

        // 再次关联,校验文件已重命名
        ScmFactory.Bucket.attachFile( session, bucketName, fileId );
        Assert.assertTrue(
                s3Client.doesObjectExist( bucketName, newObjectKey ) );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                S3Utils.clearBucket( s3Client, bucketName );
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
