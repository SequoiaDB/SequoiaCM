package com.sequoiacm.config.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResource;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2330 :: 并发删除配置和增删改查用户
 * @Date:2018年12月04日
 * @version:1.0
 */
public class DeleteConf2330 extends TestScmBase {
    private ScmSession session = null;
    private String serviceName = "auth-server";
    private List< ScmServiceInstance > authList = null;
    private String username = "2330";
    private String rolename = "2330";
    private String passwd = "2330";

    private SiteWrapper site = null;
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        authList = ScmSystem.ServiceCenter.getServiceInstanceList( session,
                serviceName );
        try {
            ScmFactory.User.deleteUser( session, username );
        } catch ( ScmException e ) {
            System.out.println( "msg = " + e.getMessage() );
        }
        try {
            ScmFactory.Role.deleteRole( session, rolename );
        } catch ( ScmException e ) {
            System.out.println( "msg = " + e.getMessage() );
        }
        ConfUtil.deleteAuditConf( serviceName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        Delete dThraed = new Delete();
        CURDUser cThread = new CURDUser();
        dThraed.start( 3 );
        cThread.start();
        Assert.assertEquals( dThraed.isSuccess(), true, dThraed.getErrorMsg() );
        Assert.assertEquals( cThread.isSuccess(), true, cThread.getErrorMsg() );

        ConfUtil.checkNotTakeEffect( serviceName );

        List< String > list = new ArrayList< String >();
        list.add( ConfigCommonDefind.scm_audit_mask );
        list.add( ConfigCommonDefind.scm_audit_userMask );
        for ( ScmServiceInstance instance : authList ) {
            ConfUtil.checkDeletedConf(
                    instance.getIp() + ":" + instance.getPort(), list );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( serviceName );
        if ( session != null ) {
            session.close();
        }
    }

    private class Delete extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmUpdateConfResultSet actResult = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service( serviceName )
                        .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                        .deleteProperty( ConfigCommonDefind.scm_audit_userMask )
                        .build();
                actResult = ScmSystem.Configuration
                        .setConfigProperties( session, confProp );

                List< String > list = new ArrayList< String >();
                list.add( ConfigCommonDefind.scm_audit_mask );
                list.add( ConfigCommonDefind.scm_audit_userMask );
                for ( ScmServiceInstance instance : authList ) {
                    ConfUtil.checkDeletedConf(
                            instance.getIp() + ":" + instance.getPort(), list );
                }
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( "delete conf failed, actResult = "
                        + actResult.toString() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CURDUser extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            createUser();
            getUser();
            updateUser();
            deleteUser();
        }

        private void createUser() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmUser scmUser = ScmFactory.User.createUser( session, username,
                        ScmUserPasswordType.LOCAL, passwd );
                ScmRole role = ScmFactory.Role.createRole( session, rolename,
                        "" );
                ScmUserModifier modifier = new ScmUserModifier();
                ScmResource resource = ScmResourceFactory
                        .createWorkspaceResource( wsp.getName() );
                ScmFactory.Role.grantPrivilege( session, role, resource,
                        ScmPrivilegeType.ALL );
                modifier.addRole( rolename );
                ScmFactory.User.alterUser( session, scmUser, modifier );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        private void getUser() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmUser user = ScmFactory.User.getUser( session, username );
                Assert.assertEquals( user.getUsername(), username );
                ScmRole role = ScmFactory.Role.getRole( session, rolename );
                Assert.assertEquals( role.getRoleName(), "ROLE_" + rolename );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        private void updateUser() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmUser user = ScmFactory.User.getUser( session, username );
                ScmUserModifier modifier = new ScmUserModifier();
                modifier.delRole( rolename );
                ScmFactory.User.alterUser( session, user, modifier );
                Assert.assertEquals( user.getUsername(), username );
                ScmUser user1 = ScmFactory.User.getUser( session, username );
                Assert.assertFalse( user1.hasRole( rolename ) );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        private void deleteUser() throws ScmException {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmFactory.User.deleteUser( session, username );
                try {
                    ScmFactory.User.getUser( session, username );
                    Assert.fail( "user should be not exist" );
                } catch ( ScmException e ) {
                    if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                        e.printStackTrace();
                        Assert.fail( e.getMessage() );
                    }
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
