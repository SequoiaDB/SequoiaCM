package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-4992 对网关发送s3请求，不指定优先站点
 * @Author YiPan
 * @CreateDate 2022/7/25
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4992 extends TestScmBase {
    private String bucketName = "bucket4992";
    private String objectKey = "object4992";
    private String content = "object4992";
    private String wsName = "ws4992";
    private ScmWorkspace s3WS;
    private AmazonS3 s3Client;
    private ScmSession session;
    private SiteWrapper branchSite;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        branchSite = ScmInfo.getBranchSite();

        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        s3WS = ScmWorkspaceUtil.createS3WS( session, wsName );
        // 设置分站点为优先站点
        s3WS.updatePreferred( branchSite.getSiteName() );

        String[] accessKeys = { TestScmBase.s3AccessKeyID,
                TestScmBase.s3SecretKey };

        ScmUser user = ScmFactory.User.getUser( session,
                TestScmBase.scmUserName );
        ScmAuthUtils.alterUser( session, wsName, user,
                ScmFactory.Role.getRole( session, "ROLE_AUTH_ADMIN" ),
                ScmPrivilegeType.ALL );
        ScmAuthUtils.checkPriorityByS3( accessKeys, wsName );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        s3Client.createBucket( bucketName, s3WS.getName() );

        s3Client.putObject( bucketName, objectKey, content );

        ScmId fileId = S3Utils.queryS3Object( s3WS, objectKey );

        SiteWrapper expSites[] = { branchSite };
        ScmFileUtils.checkMeta( s3WS, fileId, expSites );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                ScmWorkspaceUtil.deleteWs( wsName, session );
            }
        } finally {
            s3Client.shutdown();
        }
    }

}