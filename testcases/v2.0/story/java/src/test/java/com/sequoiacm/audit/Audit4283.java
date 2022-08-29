package com.sequoiacm.audit;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Descreption SCM-4283:S3节点配置S3_OBJECT_DML审计类型，执行对象dml操作
 * @Author YiPan
 * @CreateDate 2022/5/19
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Audit4283 extends TestScmBase {
    private final String auditType = "S3_OBJECT_DML";
    private final String bucketName = "bucket4283";
    private final String objectKey = "object4283";
    private SiteWrapper site = null;
    private ScmSession session = null;
    private AmazonS3 s3Client = null;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private List< String > s3ServiceNames;
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

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        s3ServiceNames = S3Utils.getS3ServiceName( session );
        for ( String s3ServiceName : s3ServiceNames ) {
            ConfUtil.deleteS3AuditConf( s3ServiceName );
        }
        s3Client = S3Utils.buildS3Client( );
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // s3服务配置S3_OBJECT_DML审计类型
        Map< String, String > confMap = new HashMap<>();
        confMap.put( ConfigCommonDefind.scm_audit_mask, auditType );
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        for ( String s3ServiceName : s3ServiceNames ) {
            ConfUtil.updateConf( s3ServiceName, confMap );
        }

        s3Client.putObject( bucketName, objectKey, new File( filePath ) );
        Assert.assertTrue( ConfUtil.checkAuditByType( session,
                "CREATE_S3_OBJECT", "key=" + objectKey ) );

        s3Client.putObject( bucketName, objectKey, new File( filePath ) );
        Assert.assertTrue( ConfUtil.checkAuditByType( session,
                "CREATE_S3_OBJECT", "key=" + objectKey ) );

        s3Client.deleteObject( bucketName, objectKey );
        Assert.assertTrue( ConfUtil.checkAuditByType( session,
                "DELETE_S3_OBJECT", "key=" + objectKey ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                TestTools.LocalFile.removeFile( localPath );
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