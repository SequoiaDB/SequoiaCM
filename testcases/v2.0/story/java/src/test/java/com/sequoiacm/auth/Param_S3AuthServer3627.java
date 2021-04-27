package com.sequoiacm.auth;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.junit.Assert;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.Base64;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @Description: SCM-3627 :: 登录接口sign_info参数校验
 * @author fanyu
 * @Date:2020年04月20日
 * @version:1.0
 */
public class Param_S3AuthServer3627 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private String username = "user3627";
    private String password = "user3627123456";
    private String[] accessKeys = null;
    private String algorithm = "HmacSHA256";
    private String[] stringData = { "1", "2", "3", "4", "5" };
    private String signatureClient = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ScmAuthUtils.createAdminUser( session, wsp.getName(), username,
                password );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, password,
                null );
        signatureClient = signatureClient();
    }

    @Test
    private void test1() throws Exception {
        // （1）指定Algorithm为null
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "algorithm", null );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", stringData );
        // 登录
        String sessionId = ScmAuthUtils.login( null, null, signInfo );
        ScmAuthUtils.logout( sessionId );

        // （2）不填Algorithm
        signInfo.removeField( "algorithm" );
        sessionId = ScmAuthUtils.login( null, null, signInfo );
        ScmAuthUtils.logout( sessionId );

        // (3) Algorithm与签名算法不一致
        signInfo.put( "algorithm", "HmacSHA1" );
        try {
            ScmAuthUtils.login( null, null, signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.UNAUTHORIZED ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @Test
    private void test2() throws Exception {
        // （1）指定accesskey为null
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", null );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", stringData );
        try {
            ScmAuthUtils.login( null, null, signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpServerErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }

        // （2）不填accesskey
        signInfo.removeField( "accesskey" );
        try {
            ScmAuthUtils.login( null, null, signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpServerErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.INTERNAL_SERVER_ERROR ) {
                throw e;
            }
        }
    }

    @Test
    private void test3() throws Exception {
        // （1）prefix不一致
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "secretkey_prefix", "test" );
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
    private void test4() throws Exception {
        // （1）指定signature为null
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "signature", null );
        signInfo.put( "string_to_sign", stringData );
        try {
            ScmAuthUtils.login( null, null, signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.UNAUTHORIZED ) {
                throw e;
            }
        }

        signInfo.removeField( "signature" );
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
    private void test5() throws Exception {
        // （1）string_to_sign为null
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", null );
        try {
            ScmAuthUtils.login( null, null, signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.UNAUTHORIZED ) {
                throw e;
            }
        }

        signInfo.removeField( "string_to_sign" );
        try {
            ScmAuthUtils.login( null, null, signInfo );
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
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, username );
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
