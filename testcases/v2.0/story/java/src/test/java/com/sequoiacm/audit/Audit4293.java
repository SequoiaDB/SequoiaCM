package com.sequoiacm.audit;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @descreption SCM-4293 :: 内容服务节点配置多个审计类型，指定审计用户类型
 * @author Zhaoyujing
 * @Date 2020/6/2
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Audit4293 extends TestScmBase {
    private final String bucketName = "bucket4293";
    private final String key = "object4293";
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private AmazonS3 s3Client = null;
    private String serviceName = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        serviceName = site.getSiteServiceName();
        ConfUtil.deleteAuditConf( serviceName );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws Exception {
        Map< String, String > confMap = new HashMap<>();
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        confMap.put( ConfigCommonDefind.scm_audit_mask,
                "SCM_BUCKET_DML|FILE_DML" );
        ConfUtil.updateConf( serviceName, confMap );

        // 创建桶
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        Assert.assertTrue( ConfUtil.checkAuditByType( session,
                "CREATE_SCM_BUCKET", "bucketName=" + bucketName ) );

        // 创建文件
        ScmFile file = bucket.createFile( key );
        file.save();
        Assert.assertTrue( ConfUtil.checkAuditByType( session, "CREATE_FILE",
                "fileName=" + key ) );

        // 删除文件
        bucket.deleteFile( key, true );
        Assert.assertTrue( ConfUtil.checkAuditByType( session, "DELETE_FILE",
                "fileName=" + key ) );

        // 删除桶
        ScmFactory.Bucket.deleteBucket( session, bucketName );
        Assert.assertTrue( ConfUtil.checkAuditByType( session,
                "DELETE_SCM_BUCKET", "bucketName=" + bucketName ) );

        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                ConfUtil.deleteAuditConf( serviceName );
            }
        } finally {
            session.close();
            if (s3Client != null) {
                s3Client.shutdown();
            }
        }
    }
}
