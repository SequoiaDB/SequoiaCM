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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.infrastructure.crypto.ScmPasswordMgr;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @Description: SCM-3633 ::
 *               同时指定username、password、signature_info，在authserver端刷新accesskey
 * @author fanyu
 * @Date:2020年04月20日
 * @version:1.0
 */
public class S3AuthServer3633 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmSession adminSession = null;
    private String username = "user3633";
    private String password = "user3633123456";
    private String cryptPassword = null;
    private String[] accessKeys = null;
    private String algorithm = "HmacSHA256";
    private String prefix = "prefix";
    private String[] stringData = { "1", "2", "3", "4", "5" };
    private String signatureClient = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        adminSession = TestScmTools.createSession( site );
        ScmAuthUtils.createAdminUser( adminSession, wsp.getName(), username,
                password );
        session = TestScmTools.createSession( site, username, password );
        cryptPassword = ScmPasswordMgr.getInstance()
                .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username,
                cryptPassword, null );
        signatureClient = signatureClient( accessKeys );
    }

    @Test
    private void test3() throws Exception {
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "secretkey_prefix", prefix );
        signInfo.put( "algorithm", algorithm );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", stringData );

        // username、cryptPassword、signInfo都正确
        String cryptPassword = ScmPasswordMgr.getInstance()
                .encrypt( ScmPasswordMgr.SCM_CRYPT_TYPE_DES, password );
        try {
            ScmAuthUtils.refreshAccessKey( session, username, cryptPassword,
                    signInfo );
            Assert.fail( "exp failed but act success!!!" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.BAD_REQUEST ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( adminSession, username );
            }
        } finally {
            if ( adminSession != null ) {
                adminSession.close();
            }
            if ( session != null ) {
                session.close();
            }
        }
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
