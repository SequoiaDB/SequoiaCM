package com.sequoiacm.s3.bucket.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-4614:强制删除存在桶的工作区，再次获取桶
 * @Author YiPan
 * @CreateDate 2022/6/25
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Bucket4614 extends TestScmBase {
    private final String bucketName = "bucket4614";
    private final String objectKey = "file4614";
    private final String wsName = "ws4614";
    private ScmSession session;

    @BeforeClass
    public void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        // 创建新ws赋权
        createWs( wsName );
        ScmAuthUtils.alterUser( session, wsName,
                ScmFactory.User.getUser( session, TestScmBase.scmUserName ),
                ScmFactory.Role.getRole( session, "ROLE_AUTH_ADMIN" ),
                ScmPrivilegeType.ALL );
        ScmAuthUtils.checkPriorityByS3( session, wsName );
    }

    @Test
    public void test() throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmFactory.Bucket.createBucket( ws, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        ScmFile file = bucket.createFile( objectKey );
        file.save();

        ScmFactory.Workspace.deleteWorkspace( session, wsName, true );

        try {
            ScmFactory.Bucket.getBucket( session, bucketName );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.BUCKET_NOT_EXISTS ) {
                throw e;
            }
        }
    }

    @AfterClass
    public void tearDown() throws Exception {
        session.close();
    }

    private void createWs( String wsName ) throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations(
                ScmWorkspaceUtil.getDataLocationList( ScmInfo.getSiteNum() ) );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setEnableDirectory( false );
        conf.setName( wsName );
        ScmWorkspaceUtil.createWS( session, conf );
    }
}