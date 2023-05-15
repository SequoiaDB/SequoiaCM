package com.sequoiacm.s3.bucketQuota;

import java.io.File;

import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.client.element.quota.ScmUpdateBucketQuotaConfig;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @description SCM-6097:更新限额输入异常验证 SCM-6099:禁用桶限额输入异常验证 SCM-6101:限额同步输入异常验证
 *              SCM-6103:获取桶限额信息输入异常验证 SCM-6105:取消限额同步输入异常验证
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota6097_6099_6101_6103_6105 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket6097";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private int maxObjectNum = 10;
    private int maxObjectSize = 10;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        SiteWrapper rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        ScmEnableBucketQuotaConfig quotaConfig = ScmEnableBucketQuotaConfig
                .newBuilder( bucketName ).setMaxObjects( maxObjectNum )
                .setMaxSize( maxObjectSize + "m" ).build();
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 指定不存在的桶
        test1();

        // 指定不存在的参数
        test2();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            s3Client.shutdown();
        }
    }

    public void test1() throws ScmException {
        ScmUpdateBucketQuotaConfig quotaConfig = ScmUpdateBucketQuotaConfig
                .newBuilder( bucketName + "inExistence" )
                .setMaxObjects( maxObjectNum ).setMaxSize( maxObjectSize + "m" )
                .build();
        try {
            ScmFactory.Quota.updateBucketQuota( session, quotaConfig );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }

        try {
            ScmFactory.Quota.disableBucketQuota( session,
                    bucketName + "inExistence" );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }

        try {
            ScmFactory.Quota.syncBucketQuota( session,
                    bucketName + "inExistence" );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }

        try {
            ScmFactory.Quota.getBucketQuota( session,
                    bucketName + "inExistence" );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }

        try {
            ScmFactory.Quota.cancelSyncBucketQuota( session,
                    bucketName + "inExistence" );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }

    }

    public void test2() throws ScmException {
        try {
            ScmUpdateBucketQuotaConfig.newBuilder( bucketName )
                    .setMaxObjects( maxObjectNum )
                    .setMaxSize( maxObjectSize + "T" ).build();
            Assert.fail( "预期失败，实际成功！" );
        } catch ( IllegalArgumentException e ) {
            if ( !e.getMessage().contains( "unsupported unit" ) ) {
                throw e;
            }
        }
    }

}
