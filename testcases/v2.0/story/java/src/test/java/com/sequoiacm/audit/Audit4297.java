package com.sequoiacm.audit;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @Descreption SCM-4297:认证服务节点配置USER_DQL审计类型
 * @Author YiPan
 * @CreateDate 2022/5/18
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Audit4297 extends TestScmBase {
    private final String roleName = "ROLE_4297";
    private final String auditType = "USER_DQL";
    private ScmSession session = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        session = TestScmTools.createSession( ScmInfo.getSite() );
        ConfUtil.deleteAuditConf( ConfUtil.AUTH_SERVER_SERVICE_NAME );
        ScmAuthUtils.createRole( session, roleName );
    }

    @Test
    public void test() throws Exception {
        // 认证服务配置USER_DQL审计类型
        Map< String, String > confMap = new HashMap<>();
        confMap.put( com.sequoiacm.config.ConfigCommonDefind.scm_audit_mask,
                auditType );
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        ConfUtil.updateConf( ConfUtil.AUTH_SERVER_SERVICE_NAME, confMap );

        // 查询用户
        ScmFactory.User.getUser( session, TestScmBase.scmUserName );
        checkAuditLog( "find user by username=" + TestScmBase.scmUserName );
        ScmFactory.User.listUsers( session, new BasicBSONObject() );
        checkAuditLog( "find all users" );

        // 查询角色
        ScmFactory.Role.getRole( session, roleName );
        checkAuditLog( "find role by roleName=" + roleName );
        ScmFactory.Role.listRoles( session );
        checkAuditLog( "find all roles" );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ConfUtil.deleteAuditConf( ConfUtil.AUTH_SERVER_SERVICE_NAME );
                ScmFactory.Role.deleteRole( session, roleName );
            }
        } finally {
            session.close();
        }
    }

    private void checkAuditLog( String message ) throws ScmException {
        ConfUtil.checkAuditByType( session, auditType, message );
    }
}