package com.sequoiacm.auth;

import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.junit.Assert;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.Base64;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @Description: SCM-3621 :: 指定不正确的signature进行登录 SCM-3622 ::
 *               指定不正确的string_to_sign进行登录 SCM-3624 :: 用户不存在，进行登录
 *               SCM-3625:同时指定username、password、signature_info，进行登录
 * @author fanyu
 * @Date:2020年04月20日
 * @version:1.0
 */
public class S3AuthServer3621To3625 extends TestScmBase {
    private int expRunSuccessCount = 4;
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private String username = "user3621";
    private String password = "user3621123456";
    private String[] accessKeys = null;
    private String algorithm = "HmacSHA256";
    private String[] stringData = { "1", "2", "3", "4", "5" };
    private String roleName = "role_3621";
    private String signatureClient = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ScmAuthUtils.createNormalUser( session, wsp.getName(), username,
                password, roleName, ScmPrivilegeType.ALL );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, password,
                null );
        signatureClient = signatureClient();
    }

    @Test
    private void test3621() throws Exception {
        // 指定不正确的signature进行登录
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "algorithm", algorithm );
        signInfo.put( "signature", signatureClient + "test" );
        signInfo.put( "string_to_sign", stringData );
        try {
            ScmAuthUtils.login( null, null, signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.UNAUTHORIZED ) {
                throw e;
            }
        }
    }

    @Test
    private void test3622() throws Exception {
        // 指定不正确的string_to_sign进行登录
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "algorithm", algorithm );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign",
                new String[] { "1", "21", "3", "41", "5" } );
        try {
            ScmAuthUtils.login( null, null, signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.UNAUTHORIZED ) {
                throw e;
            }
        }
    }

    @Test
    private void test3624() throws Exception {
        // 用户不存在，进行登录
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] + "test" );
        signInfo.put( "algorithm", algorithm );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", stringData );
        try {
            ScmAuthUtils.login( null, null, signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.UNAUTHORIZED ) {
                throw e;
            }
        }
    }

    @Test
    private void test3625() throws Exception {
        // 同时指定username、password、signature_info，进行登录
        // (1)username、password、signature_info都正确
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "algorithm", algorithm );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", stringData );
        String sessionId = ScmAuthUtils.login( username, password, signInfo );
        ScmAuthUtils.getRootDir( sessionId, wsp.getName() );
        ScmAuthUtils.logout( sessionId );

        // (2)signature_info不正确，username、password正确
        signInfo.put( "string_to_sign",
                new String[] { "1", "21", "3", "41", "5" } );
        String sessionId1 = ScmAuthUtils.login( username, password, signInfo );
        ScmAuthUtils.getRootDir( sessionId1, wsp.getName() );
        ScmAuthUtils.logout( sessionId1 );

        // (3)username、signature_info正确,password不正确
        signInfo.put( "string_to_sign", stringData );
        try {
            ScmAuthUtils.login( username, password + "test", signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.UNAUTHORIZED ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccessCount.get() == expRunSuccessCount
                    || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, username );
                ScmFactory.Role.deleteRole( session, roleName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private String signatureClient() {
        byte[] kSecret = accessKeys[ 1 ].getBytes();
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
