package com.sequoiacm.auth;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.Base64;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @Description: SCM-3634:指定是否清理用户会话，重复刷新accessKey
 * @author fanyu
 * @Date:2020年04月20日
 * @version:1.0
 */
public class S3AuthServer3634 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private String username = "user3634";
    private String password = "user3634123456";
    private String[] accessKeys = null;
    private String algorithm = "HmacSHA256";
    private String prefix = "prefix";
    private String[] stringData = { "1", "2", "3", "4", "5" };
    private String signatureClient = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ScmAuthUtils.createAdminUser( session, wsp.getName(), username,
                password );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, null,
                null );
        signatureClient = signatureClient( accessKeys );
    }

    @Test
    private void test() throws Exception {
        // 不清理该用户下会话，刷新accessKey
        testNoCleanSession();
        // 清理该用户下会话，刷新accessKey
        testCleanSession();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, username );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void testNoCleanSession() throws Exception {
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "secretkey_prefix", prefix );
        signInfo.put( "algorithm", algorithm );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", stringData );
        // 登录1
        String sessionId1 = ScmAuthUtils.login( null, null, signInfo );

        // 用户修改密码，不清理该用户下的会话
        String newPassword = password + "new";
        alterUser( username, password, newPassword, false );

        // 刷新accessKey
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, null,
                null );
        // 登录2
        signatureClient = signatureClient( accessKeys );
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "signature", signatureClient );
        String sessionId2 = ScmAuthUtils.login( null, null, signInfo );
        // 使用连接1操作业务
        ScmAuthUtils.getRootDir( sessionId1, wsp.getName() );
        // 使用连接2操作业务
        ScmAuthUtils.getRootDir( sessionId2, wsp.getName() );
        // 登出
        ScmAuthUtils.logout( sessionId1 );
        ScmAuthUtils.logout( sessionId2 );
    }

    private void testCleanSession() throws Exception {
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "secretkey_prefix", prefix );
        signInfo.put( "algorithm", algorithm );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", stringData );
        // 登录1
        String sessionId1 = ScmAuthUtils.login( null, null, signInfo );

        // 用户修改密码，清理该用户下的会话
        String oldPassword = password + "new";
        String newPassword = password + "new_new";
        alterUser( username, oldPassword, newPassword, true );

        // 刷新accessKey
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, null,
                null );
        // 登录2
        signatureClient = signatureClient( accessKeys );
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "signature", signatureClient );
        String sessionId2 = ScmAuthUtils.login( null, null, signInfo );

        // 使用连接1操作业务
        try {
            ScmAuthUtils.getRootDir( sessionId1, wsp.getName() );
            Assert.fail( "exp failed but act success!!!!" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.UNAUTHORIZED ) {
                throw e;
            }
        }
        // 使用连接2操作业务
        ScmAuthUtils.getRootDir( sessionId2, wsp.getName() );

        // 登出
        ScmAuthUtils.logout( sessionId2 );
    }

    private void alterUser( String username, String oldPassword,
            String newPassword, boolean cleanSession ) throws ScmException {
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setCleanSessions( cleanSession );
        modifier.setPassword( oldPassword, newPassword );
        ScmUser user = ScmFactory.User.getUser( session, username );
        ScmFactory.User.alterUser( session, user, modifier );
    }

    private String signatureClient( String[] accessKeys ) {
        // 计算签名
        byte[] kSecret = ( prefix + accessKeys[ 1 ] ).getBytes();
        byte[] kDate = ScmAuthUtils.sign( stringData[ 0 ], kSecret, algorithm );
        byte[] kRegion = ScmAuthUtils.sign( stringData[ 1 ], kDate, algorithm );
        byte[] kService = ScmAuthUtils.sign( stringData[ 2 ], kRegion,
                algorithm );
        byte[] kSigning = ScmAuthUtils.sign( stringData[ 3 ], kService,
                algorithm );
        byte[] ksignature = ScmAuthUtils.sign( stringData[ 4 ], kSigning,
                algorithm );
        return Base64.encodeAsString( ksignature );
    }
}
