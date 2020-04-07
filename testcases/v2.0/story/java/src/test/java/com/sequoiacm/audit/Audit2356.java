package com.sequoiacm.audit;

import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2356:指定审计日志所有配置项，对authserver的各个接口进行测试
 * @author fanyu
 * @Date:2019年01月03日
 * @version:1.0
 */
public class Audit2356 extends TestScmBase {
    private String serviceName = "auth-server";
    private String username1 = "token2356";
    private String username2 = "local2356";
    private String username3 = null;
    private String newUsername = "user2356";
    private String newRolename = "role2356";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        username3 = TestScmBase.ldapUserName;
        ConfUtil.deleteAuditConf( serviceName );
        ConfUtil.deleteUserAndRole( username1, username1 );
        ConfUtil.deleteUserAndRole( username2, username2 );
        ConfUtil.deleteUserAndRole( username3, username3 );
        ConfUtil.createUser( wsp, username1, ScmUserPasswordType.TOKEN,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        ConfUtil.createUser( wsp, username2, ScmUserPasswordType.LOCAL,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        ConfUtil.createUser( wsp, username3, ScmUserPasswordType.LDAP,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        session = TestScmTools.createSession( site );
    }

    //bug:442
    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        Map< String, String > confMap = new HashMap< String, String >();
        confMap.put( ConfigCommonDefind.scm_audit_user + username3, "ALL" );
        confMap.put( ConfigCommonDefind.scm_audit_userType +
                ScmUserPasswordType.TOKEN.name(), "ALL" );
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        confMap.put( ConfigCommonDefind.scm_audit_mask, "ALL" );
        ConfUtil.updateConf( serviceName, confMap );
        //check
        checkAudit( username1, username1 );
        checkAudit( username2, username2 );
        checkAudit( username3, TestScmBase.ldapPassword );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteAuditConf( serviceName );
        ConfUtil.deleteUserAndRole( username1, username1 );
        ConfUtil.deleteUserAndRole( username2, username2 );
        ConfUtil.deleteUserAndRole( TestScmBase.ldapUserName,
                TestScmBase.ldapUserName );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit( String username, String password )
            throws ScmException, InterruptedException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            doAll( session );
            BSONObject[] bsonArray = {
                    //USER CRUD
                    new BasicBSONObject().append( ScmAttributeName.Audit.TYPE,
                            "CREATE_USER" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ),
                    new BasicBSONObject()
                            .append( ScmAttributeName.Audit.TYPE, "USER_DQL" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ),
                    new BasicBSONObject().append( ScmAttributeName.Audit.TYPE,
                            "DELETE_USER" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ),
                    new BasicBSONObject().append( ScmAttributeName.Audit.TYPE,
                            "UPDATE_USER" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ),
                    //ROLE CRD
                    new BasicBSONObject().append( ScmAttributeName.Audit.TYPE,
                            "CREATE_ROLE" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ),
                    new BasicBSONObject()
                            .append( ScmAttributeName.Audit.TYPE, "ROLE_DQL" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ),
                    new BasicBSONObject().append( ScmAttributeName.Audit.TYPE,
                            "DELETE_ROLE" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ),
                    //grant revoke
                    new BasicBSONObject()
                            .append( ScmAttributeName.Audit.TYPE, "GRANT" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ),
                    new BasicBSONObject()
                            .append( ScmAttributeName.Audit.TYPE, "REVOKE" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ),
                    //login
                    new BasicBSONObject()
                            .append( ScmAttributeName.Audit.TYPE, "LOGIN" )
                            .append( ScmAttributeName.Audit.USERNAME,
                            username ) };

            //USER CRUD
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bsonArray[ 0 ], newUsername ),
                    true, "Has the configuration been updated? newUsername = " +
                            newUsername );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bsonArray[ 1 ], newUsername ),
                    true, "Has the configuration been updated? newUsername = " +
                            newUsername );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bsonArray[ 2 ], newUsername ),
                    true, "Has the configuration been updated? newUsername = " +
                            newUsername );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bsonArray[ 3 ], newRolename ),
                    true, "Has the configuration been updated? newRolename = " +
                            newRolename );

            //ROLE CRD
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bsonArray[ 4 ], newRolename ),
                    true, "Has the configuration been updated? newRolename = " +
                            newRolename );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bsonArray[ 5 ], newRolename ),
                    true, "Has the configuration been updated? newRolename = " +
                            newRolename );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bsonArray[ 6 ], newRolename ),
                    true, "Has the configuration been updated? newRolename = " +
                            newRolename );

            //grant revoke and login
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bsonArray[ 7 ], newRolename ),
                    true, "Has the configuration been updated? newRolename = " +
                            newRolename );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bsonArray[ 8 ], newRolename ),
                    true, "Has the configuration been updated? newRolename = " +
                            newRolename );
            Assert.assertEquals( ConfUtil.checkAudit( session, bsonArray[ 9 ],
                    session.getSessionId() ), true,
                    "Has the configuration been updated? newUsername = " +
                            newUsername );
        } finally {
            //logout
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void doAll( ScmSession session ) throws ScmException {
        // USER_DML
        ScmUser user = ScmFactory.User.createUser( session, newUsername,
                ScmUserPasswordType.LOCAL, newUsername );
        // USER_DQL
        ScmFactory.User.getUser( session, newUsername );

        // ROLE_DML
        ScmRole role = ScmFactory.Role
                .createRole( session, newRolename, newRolename );
        // ROLE _DQL
        ScmFactory.Role.getRole( session, newRolename );

        // GRANT
        ScmResource resource = ScmResourceFactory
                .createWorkspaceResource( wsp.getName() );
        ScmFactory.Role.grantPrivilege( session, role, resource,
                ScmPrivilegeType.ALL );

        //REVOKE
        ScmFactory.Role.revokePrivilege( session, role, resource,
                ScmPrivilegeType.ALL );

        // USER_DML、ROLE_DML
        ScmFactory.User.alterUser( session, user,
                new ScmUserModifier().addRole( role ) );
        ScmFactory.User.deleteUser( session, user );
        ScmFactory.Role.deleteRole( session, role );
    }
}
