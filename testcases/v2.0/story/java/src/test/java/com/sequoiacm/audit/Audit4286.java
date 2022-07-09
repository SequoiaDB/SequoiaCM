package com.sequoiacm.audit;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Descreption SCM-4286:S3节点配置S3_BUCKET_DML审计类型，执行对象dml操作
 * @Author YiPan
 * @CreateDate 2022/5/19
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Audit4286 extends TestScmBase {
    private final String S3_BUCKET_DML = "S3_BUCKET_DML";
    private final String bucketName = "bucket4286";
    private ScmSession session = null;
    private AmazonS3 s3Client = null;
    private List< String > s3ServiceNames;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        s3ServiceNames = S3Utils.getS3ServiceName( session );
        for ( String s3ServiceName : s3ServiceNames ) {
            ConfUtil.deleteS3AuditConf( s3ServiceName );
        }
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        Map< String, String > confMap = new HashMap<>();
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        confMap.put( ConfigCommonDefind.scm_audit_mask, S3_BUCKET_DML );
        for ( String s3ServiceName : s3ServiceNames ) {
            ConfUtil.updateConf( s3ServiceName, confMap );
        }

        // 创建桶
        s3Client.createBucket( bucketName );
        Assert.assertTrue(
                ConfUtil.checkAuditByType( session, "CREATE_S3_BUCKET",
                        "create s3 bucket: bucketName=" + bucketName ) );

        BucketVersioningConfiguration config = new BucketVersioningConfiguration()
                .withStatus( BucketVersioningConfiguration.SUSPENDED );
        s3Client.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest( bucketName,
                        config ) );
        // TODO:SEQUOIACM-952 存在问题，暂未实现校验

        // 删除桶
        s3Client.deleteBucket( bucketName );
        Assert.assertTrue(
                ConfUtil.checkAuditByType( session, "DELETE_S3_BUCKET",
                        "delete s3 bucket: bucketName=" + bucketName ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                for ( String s3ServiceName : s3ServiceNames ) {
                    ConfUtil.deleteS3AuditConf( s3ServiceName );
                }
            }
        } finally {
            s3Client.shutdown();
            session.close();
        }
    }
}