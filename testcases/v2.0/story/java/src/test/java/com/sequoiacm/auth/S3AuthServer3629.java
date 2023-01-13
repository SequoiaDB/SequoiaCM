package com.sequoiacm.auth;

import com.amazonaws.util.Base64;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

/**
 * @Descreption SCM-3629:管理员用户在authserver端刷新accesskey
 * @Author YiPan
 * @Date 2021/4/21
 */
public class S3AuthServer3629 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmSession sessionAdmin = null;
    private String username1 = "user3629a";
    private String password1 = "user3629123456a";
    private String username2 = "user3629b";
    private String password2 = "user3629123456b";
    private String roleName = "user3629_role";
    private String[] accessKeys = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // 创建admin账户
        ScmAuthUtils.createAdminUser( session, wsp.getName(), username1,
                password1 );
        String cryptPassword = ScmPasswordMgr.getInstance()
                .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password1 );

        // 刷新普通用户
        ScmAuthUtils.createNormalUser( session, wsp.getName(), username2,
                password2, roleName, ScmPrivilegeType.ALL );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username2, null,
                null );
        checkSign();

        // 使用signInfo刷新普通用户
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, null, null,
                signInfo );
        checkSign();

        // 刷新自己
        sessionAdmin = TestScmTools.createSession( site, username1, password1 );
        accessKeys = ScmAuthUtils.refreshAccessKey( sessionAdmin, username1,
                cryptPassword, null );
        checkSign();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmFactory.User.deleteUser( session, username1 );
                ScmFactory.User.deleteUser( session, username2 );
                ScmFactory.Role.deleteRole( session, roleName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
            if ( sessionAdmin != null ) {
                sessionAdmin.close();
            }
        }
    }

    private void checkSign() throws UnsupportedEncodingException {
        // 计算签名
        String algorithm = "HmacSHA256";
        String[] data = { "1", "2", "3", "4", "5" };
        byte[] kSecret = accessKeys[ 1 ].getBytes();
        byte[] kDate = ScmAuthUtils.sign( data[ 0 ], kSecret, algorithm );
        byte[] kRegion = ScmAuthUtils.sign( data[ 1 ], kDate, algorithm );
        byte[] kService = ScmAuthUtils.sign( data[ 2 ], kRegion, algorithm );
        byte[] kSigning = ScmAuthUtils.sign( data[ 3 ], kService, algorithm );
        byte[] ksignature = ScmAuthUtils.sign( data[ 4 ], kSigning, algorithm );
        String signatureClient = Base64.encodeAsString( ksignature );
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", data );
        // 登录
        String sessionId = ScmAuthUtils.login( null, null, signInfo );
        // 做业务操作
        ScmAuthUtils.getRootDir( sessionId, wsp.getName() );
        // 登出
        ScmAuthUtils.logout( sessionId );
    }
}
