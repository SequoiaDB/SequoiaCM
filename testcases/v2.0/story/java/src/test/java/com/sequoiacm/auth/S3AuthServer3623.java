package com.sequoiacm.auth;

import com.amazonaws.util.Base64;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
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
 * @Descreption SCM-3623:用户被禁用，进行登录
 * @Author YiPan
 * @Date 2021/4/21
 */
public class S3AuthServer3623 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private String username = "user3623";
    private String password = "user3623123456";
    private String[] accessKeys = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ScmAuthUtils.createAdminUser( session, wsp.getName(), username,
                password );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, password,
                null );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws ScmException {
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

        // 禁止用账号
        ScmUserModifier modifier = new ScmUserModifier();
        modifier.setEnabled( false );
        ScmUser user = ScmFactory.User.getUser( session, username );
        ScmFactory.User.alterUser( session, user, modifier );

        try {
            ScmAuthUtils.login( null, null, signInfo );
            Assert.fail( "except fail but success" );
        } catch ( HttpClientErrorException e ) {
            if ( e.getStatusCode() != HttpStatus.UNAUTHORIZED ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                ScmFactory.User.deleteUser( session, username );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
