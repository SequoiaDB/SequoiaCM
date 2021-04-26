package com.sequoiacm.auth;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Descreption SCM-3628:普通用户在authserver端刷新accesskey
 * @Author YiPan
 * @Date 2021/4/21
 */
public class S3AuthServer3628 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession sessionAdmin = null;
    private ScmSession sessionUser = null;
    private String username = "user3628";
    private String password = "user3628123456";
    private String username1 = "user3628a";
    private String password1 = "user3628123456a";
    private String username2 = "user3628b";
    private String password2 = "user3628123456b";
    private String roleName = "user3628_role";
    private String[] accessKeys = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        sessionAdmin = TestScmTools.createSession( site );
        ScmAuthUtils.createNormalUser( sessionAdmin, wsp.getName(), username,
                password, roleName, ScmPrivilegeType.ALL );
        ScmAuthUtils.refreshAccessKey( sessionAdmin, username, password, null );
        sessionUser = TestScmTools.createSession( site, username, password );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // 刷新管理员账户
        ScmAuthUtils.createAdminUser( sessionAdmin, wsp.getName(), username1,
                password1 );
        try {
            String cryptPassword = ScmPasswordMgr.getInstance()
                    .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password1 );
            accessKeys = ScmAuthUtils.refreshAccessKey( sessionUser, username1,
                    cryptPassword, null );
            Assert.fail( "except fail but success" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.FORBIDDEN ) {
                throw e;
            }
        }

        // 刷新普通用户
        try {
            ScmAuthUtils.createNormalUser( sessionAdmin, wsp.getName(),
                    username2, password2, roleName, ScmPrivilegeType.ALL );
            String cryptPassword = ScmPasswordMgr.getInstance()
                    .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password2 );
            // 生成accesskeys
            accessKeys = ScmAuthUtils.refreshAccessKey( sessionAdmin, username2,
                    cryptPassword, null );
            BSONObject signInfo = new BasicBSONObject();
            signInfo.put( "accesskey", accessKeys[ 0 ] );
            // 使用accesskey刷
            accessKeys = ScmAuthUtils.refreshAccessKey( sessionUser, null, null,
                    signInfo );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.FORBIDDEN ) {
                throw e;
            }
        }

        // 刷新自身
        try {
            String cryptPassword = ScmPasswordMgr.getInstance()
                    .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password );
            accessKeys = ScmAuthUtils.refreshAccessKey( sessionUser, username,
                    cryptPassword, null );
            Assert.fail( "except fail but success" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.FORBIDDEN ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess = true ) {
                ScmFactory.User.deleteUser( sessionAdmin, username );
                ScmFactory.User.deleteUser( sessionAdmin, username1 );
                ScmFactory.User.deleteUser( sessionAdmin, username2 );
                ScmFactory.Role.deleteRole( sessionAdmin, roleName );
            }
        } finally {
            if ( sessionAdmin != null ) {
                sessionAdmin.close();
            }
            if ( sessionUser != null ) {
                sessionUser.close();
            }
        }
    }
}
