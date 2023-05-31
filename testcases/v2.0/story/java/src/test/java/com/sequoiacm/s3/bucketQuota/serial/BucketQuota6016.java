package com.sequoiacm.s3.bucketQuota.serial;

import java.io.File;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.quota.ScmEnableBucketQuotaConfig;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;

/**
 * @description SCM-6016:删除工作区，检查清理限额表和额度统计表
 * @author ZhangYanan
 * @createDate 2023.04.12
 * @updateUser ZhangYanan
 * @updateDate
 * @updateRemark
 * @version v1.0
 */

public class BucketQuota6016 extends TestScmBase {
    private boolean runSuccess = false;
    private String keyName = "object6016";
    private String bucketName = "bucket6016";
    private String wsName = "ws6016";
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private int maxObjectNum = 20;
    private int maxObjectSize = 20;
    private int objectNum = 20;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createDisEnableDirectoryWS( session, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );

        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test
    public void test() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        ScmEnableBucketQuotaConfig quotaConfig = ScmEnableBucketQuotaConfig
                .newBuilder( bucketName ).setMaxObjects( maxObjectNum )
                .setMaxSize( maxObjectSize + "m" ).build();
        ScmFactory.Quota.enableBucketQuota( session, quotaConfig );

        for ( int i = 0; i < objectNum; i++ ) {
            ScmFile file = bucket.createFile( keyName + i );
            file.setContent( filePath );
            file.setFileName( keyName + i );
            file.save();
        }
        ScmWorkspaceUtil.deleteWs( wsName, session );

        try {
            ScmFactory.Quota.getBucketQuota( session, bucketName );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_BAD_REQUEST
                    .getErrorCode() ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }
}
