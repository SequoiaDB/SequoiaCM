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
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2355:所有有关于审计类型的配置项都配置，服务为authserver
 * @author fanyu
 * @Date:2018年12月25日
 * @version:1.0
 */
public class Audit2355 extends TestScmBase {
    private String serviceName = "auth-server";
    private String username1 = "token2355A";
    private String username2 = "token2355B";
    private String username3 = "local2355A";
    private String newUsername = "test2355";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( serviceName );
        ConfUtil.deleteUserAndRole( username1, username1 );
        ConfUtil.deleteUserAndRole( username2, username2 );
        ConfUtil.deleteUserAndRole( username3, username3 );
        ConfUtil.createUser( wsp, username1, ScmUserPasswordType.TOKEN,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        ConfUtil.createUser( wsp, username2, ScmUserPasswordType.TOKEN,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        ConfUtil.createUser( wsp, username3, ScmUserPasswordType.LOCAL,
                new ScmPrivilegeType[] { ScmPrivilegeType.ALL } );
        session = TestScmTools.createSession( site );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, InterruptedException {
        Map< String, String > confMap = new HashMap< String, String >();
        confMap.put( ConfigCommonDefind.scm_audit_user + username1,
                "USER_DML" );
        confMap.put( ConfigCommonDefind.scm_audit_userType
                + ScmUserPasswordType.TOKEN.name(), "USER_DQL" );
        confMap.put( ConfigCommonDefind.scm_audit_userMask, "TOKEN" );
        confMap.put( ConfigCommonDefind.scm_audit_mask, "USER_DQL" );
        ConfUtil.updateConf( serviceName, confMap );

        // check
        checkAudit( username1, username1, true, false );
        checkAudit( username2, username2, false, true );
        checkAudit( username3, username3, false, false );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ConfUtil.deleteAuditConf( serviceName );
        ConfUtil.deleteUserAndRole( username1, username1 );
        ConfUtil.deleteUserAndRole( username2, username2 );
        ConfUtil.deleteUserAndRole( username3, username3 );
        if ( session != null ) {
            session.close();
        }
    }

    private void checkAudit( String username, String password,
            boolean isLogged1, boolean isLogged2 )
            throws ScmException, InterruptedException {
        ScmUser user = null;
        try {
            user = createAndQueryUser( username, password );
            BSONObject bson1 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "CREATE_USER" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            BSONObject bson2 = new BasicBSONObject()
                    .append( ScmAttributeName.Audit.TYPE, "USER_DQL" )
                    .append( ScmAttributeName.Audit.USERNAME, username );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bson1, newUsername ),
                    isLogged1,
                    "Has the configuration been updated? newUsername = "
                            + newUsername );
            Assert.assertEquals(
                    ConfUtil.checkAudit( session, bson2, newUsername ),
                    isLogged2,
                    "Has the configuration been updated? newUsername = "
                            + newUsername );
        } finally {
            if ( user != null ) {
                ScmFactory.User.deleteUser( session, newUsername );
            }
        }
    }

    private ScmUser createAndQueryUser( String username, String password )
            throws ScmException {
        ScmSession session = null;
        ScmUser user = null;
        try {
            session = TestScmTools.createSession( site, username, password );
            user = ScmFactory.User.createUser( session, newUsername,
                    ScmUserPasswordType.LOCAL, "123456" );
            ScmFactory.User.getUser( session, newUsername );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return user;
    }
}
