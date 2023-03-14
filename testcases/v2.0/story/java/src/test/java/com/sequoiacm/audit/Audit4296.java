package com.sequoiacm.audit;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @descreption SCM-4296 :: 认证服务节点配置USER_DML审计类型
 * @author Zhaoyujing
 * @Date 2020/6/2
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Audit4296 extends TestScmBase {
    private String newUserName = "user4296";
    private String newRoleName = "role4296";
    private String serviceName = "auth-server";
    private ScmSession session = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );

        ConfUtil.deleteAuditConf( serviceName );
        ConfUtil.deleteUserAndRole( newUserName, newRoleName );
    }

    @Test
    public void test() throws Exception {
        Map< String, String > confMap = new HashMap<>();
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        confMap.put( ConfigCommonDefind.scm_audit_mask, "USER_DML" );
        confMap.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.TOKEN.name(), "ALL" );
        ConfUtil.updateConf( serviceName, confMap );

        ScmFactory.User.createUser( session, newUserName,
                ScmUserPasswordType.TOKEN, newUserName );
        Assert.assertTrue( ConfUtil.checkAuditByType( session, "CREATE_USER",
                "userName=" + newUserName ) );

        ScmRole role = ScmFactory.Role.createRole( session, newRoleName, "" );
        Assert.assertTrue( ConfUtil.checkAuditByType( session, "CREATE_ROLE",
                "roleName=" + newRoleName ) );

        ScmResource rs = ScmResourceFactory
                .createWorkspaceResource( s3WorkSpaces );
        ScmFactory.Role.grantPrivilege( session, role, rs,
                ScmPrivilegeType.ALL );
        Assert.assertTrue( ConfUtil.checkAuditByType( session, "GRANT",
                "roleName=ROLE_" + newRoleName ) );

        ConfUtil.deleteUserAndRole( newUserName, newRoleName );
        Assert.assertTrue( ConfUtil.checkAuditByType( session, "DELETE_USER",
                "userName=" + newUserName ) );
        Assert.assertTrue( ConfUtil.checkAuditByType( session, "DELETE_ROLE",
                "roleName=" + newRoleName ) );

        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ConfUtil.deleteAuditConf( serviceName );
            }
        } finally {
            session.close();
        }
    }
}
