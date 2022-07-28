package com.sequoiacm.s3.object;

import com.amazonaws.ClientConfiguration;
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

import java.util.List;

/**
 * @Descreption SCM-4991 对网关发送s3请求，指定优先站点
 * @Author YiPan
 * @CreateDate 2022/7/25
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4991 extends TestScmBase {
    private String bucketName = "bucket4991";
    private String objectKey = "object4991";
    private String content = "object4991";
    private String wsName = "ws4991";
    private ScmWorkspace s3WS;
    private AmazonS3 s3Client;
    private ScmSession session;
    private List< SiteWrapper > branchSites;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        branchSites = ScmInfo.getBranchSites( 2 );

        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        s3WS = ScmWorkspaceUtil.createS3WS( session, wsName );
        // 设置分站点2为优先站点
        s3WS.updatePreferred( branchSites.get( 0 ).getSiteName() );

        String[] accessKeys = { TestScmBase.s3AccessKeyID,
                TestScmBase.s3SecretKey };

        ScmUser user = ScmFactory.User.getUser( session,
                TestScmBase.scmUserName );
        ScmAuthUtils.alterUser( session, wsName, user,
                ScmFactory.Role.getRole( session, "ROLE_AUTH_ADMIN" ),
                ScmPrivilegeType.ALL );
        ScmAuthUtils.checkPriorityByS3( accessKeys, wsName );

        ClientConfiguration config = new ClientConfiguration();
        config.setUseExpectContinue( false );
        config.setSocketTimeout( 300000 );
        // 请求指定优先站点为分站点1
        config.addHeader( "x-scm-preferred",
                branchSites.get( 1 ).getSiteName() );
        s3Client = S3Utils.buildS3Client( accessKeys[ 0 ], accessKeys[ 1 ],
                S3Utils.getS3Url(), config );
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        s3Client.createBucket( bucketName, wsName );

        s3Client.putObject( bucketName, objectKey, content );

        ScmId fileId = S3Utils.queryS3Object( s3WS, objectKey );

        SiteWrapper expSites[] = { branchSites.get( 1 ) };
        ScmFileUtils.checkMeta( s3WS, fileId, expSites );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                ScmWorkspaceUtil.deleteWs( wsName, session );
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            s3Client.shutdown();
        }
    }

}