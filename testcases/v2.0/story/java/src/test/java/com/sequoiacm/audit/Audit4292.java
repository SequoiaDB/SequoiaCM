package com.sequoiacm.audit;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @descreption SCM-4292 :: S3节点配置多个审计类型，指定审计用户类型
 * @author Zhaoyujing
 * @Date 2020/6/2
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Audit4292 extends TestScmBase {
    private final String bucketName = "bucket4292";
    private final String key = "object4292";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private AmazonS3 s3Client = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private List< String > s3ServiceNames;

    @BeforeClass
    public void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        s3ServiceNames = S3Utils.getS3ServiceName( session );
        for ( String s3ServiceName : s3ServiceNames ) {
            ConfUtil.deleteS3AuditConf( s3ServiceName );
        }

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws Exception {
        Map< String, String > confMap = new HashMap<>();
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        confMap.put( ConfigCommonDefind.scm_audit_mask,
                "S3_BUCKET_DML|S3_OBJECT_DML" );
        for ( String s3ServiceName : s3ServiceNames ) {
            ConfUtil.updateConf( s3ServiceName, confMap );
        }

        // 创建桶
        s3Client.createBucket( bucketName );
        Assert.assertTrue( ConfUtil.checkAuditByType( session,
                "CREATE_S3_BUCKET", "bucketName=" + bucketName ) );

        s3Client.putObject( bucketName, key, "aaa" );
        Assert.assertTrue( ConfUtil.checkAuditByType( session,
                "CREATE_S3_OBJECT", "key=" + key ) );

        s3Client.deleteObject( bucketName, key );
        Assert.assertTrue( ConfUtil.checkAuditByType( session,
                "DELETE_S3_OBJECT", "key=" + key ) );

        // 删除桶
        s3Client.deleteBucket( bucketName );
        Assert.assertTrue( ConfUtil.checkAuditByType( session,
                "DELETE_S3_BUCKET", "bucketName=" + bucketName ) );

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
            session.close();
            if (s3Client != null) {
                s3Client.shutdown();
            }
        }
    }
}
