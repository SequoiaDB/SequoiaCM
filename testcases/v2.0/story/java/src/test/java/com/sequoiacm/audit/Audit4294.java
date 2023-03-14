package com.sequoiacm.audit;

import com.sequoiacm.client.core.*;
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
 * @descreption SCM-4294 :: 认证服务配置删除的审计类型
 * @author Zhaoyujing
 * @Date 2020/6/2
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Audit4294 extends TestScmBase {
    private ScmSession session = null;
    private String newRoleName = "role4294";
    private String authServiceName = "auth-server";
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        SiteWrapper site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ConfUtil.deleteAuditConf( authServiceName );
    }

    @Test
    public void test() throws Exception {
        Map< String, String > roleDmlConfMap = new HashMap<>();
        roleDmlConfMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        roleDmlConfMap.put( ConfigCommonDefind.scm_audit_mask, "ROLE_DML" );
        roleDmlConfMap.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.TOKEN.name(), "ALL" );
        ConfUtil.updateConf( authServiceName, roleDmlConfMap );

        Map< String, String > roleDqlConfMap = new HashMap<>();
        roleDqlConfMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        roleDqlConfMap.put( ConfigCommonDefind.scm_audit_mask, "ROLE_DQL" );
        roleDqlConfMap.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.TOKEN.name(), "ALL" );
        ConfUtil.updateConf( authServiceName, roleDqlConfMap );

        Map< String, String > grantConfMap = new HashMap<>();
        grantConfMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        grantConfMap.put( ConfigCommonDefind.scm_audit_mask, "GRANT" );
        grantConfMap.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.TOKEN.name(), "ALL" );
        ConfUtil.updateConf( authServiceName, grantConfMap );

        // 增加角色
        ScmRole role = ScmFactory.Role.createRole( session, newRoleName, "" );
        Assert.assertFalse( ConfUtil.checkAuditByType( session, "CREATE_ROLE",
                "roleName=" + newRoleName ) );

        // 授权
        ScmResource rs = ScmResourceFactory
                .createWorkspaceResource( s3WorkSpaces );
        ScmFactory.Role.grantPrivilege( session, role, rs,
                ScmPrivilegeType.ALL );
        Assert.assertFalse( ConfUtil.checkAuditByType( session, "GRANT",
                "roleName=ROLE_" + newRoleName ) );

        // 查询角色
        ScmFactory.Role.getRole( session, newRoleName );
        Assert.assertFalse( ConfUtil.checkAuditByType( session, "ROLE_DQL",
                "find role by roleName=" + newRoleName ) );

        // 删除角色
        ScmFactory.Role.deleteRole( session, newRoleName );
        Assert.assertFalse( ConfUtil.checkAuditByType( session, "DELETE_ROLE",
                "roleName=" + newRoleName ) );

        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ConfUtil.deleteAuditConf( authServiceName );
            }
        } finally {
            session.close();
        }
    }
}
