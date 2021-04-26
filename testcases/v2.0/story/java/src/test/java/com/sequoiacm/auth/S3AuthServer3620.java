package com.sequoiacm.auth;

import java.util.concurrent.atomic.AtomicInteger;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
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
 * @Description: SCM-3620 :: 指定Algorithm，其它参数正确，进行登录
 * @author fanyu
 * @Date:2020年04月20日
 * @version:1.0
 */
public class S3AuthServer3620 extends TestScmBase {
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private String username = "user3620";
    private String password = "user3620123456";
    private String[] accessKeys = null;
    private String[] stringData = { "1", "2", "3", "4", "5" };

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

    @DataProvider(name = "dataProvider", parallel = true)
    public Object[][] generateData() {
        return new Object[][] { { "HmacMD5" }, { "HmacSHA1" }, { "HmacSHA256" },
                { "HmacSHA384" }, { "HmacSHA512" } };
    }

    // SEQUOIACM-659
    @Test(dataProvider = "dataProvider", enabled = false)
    private void test( String algorithm ) throws Exception {
        // 计算签名
        String prefix = "prefix";
        byte[] kSecret = ( prefix + accessKeys[ 1 ] ).getBytes();
        byte[] kDate = ScmAuthUtils.sign( stringData[ 0 ], kSecret, algorithm );
        byte[] kRegion = ScmAuthUtils.sign( stringData[ 1 ], kDate, algorithm );
        byte[] kService = ScmAuthUtils.sign( stringData[ 2 ], kRegion,
                algorithm );
        byte[] kSigning = ScmAuthUtils.sign( stringData[ 3 ], kService,
                algorithm );
        byte[] ksignature = ScmAuthUtils.sign( stringData[ 4 ], kSigning,
                algorithm );
        String signatureClient = Base64.encodeAsString( ksignature );
        BSONObject signInfo = new BasicBSONObject();
        signInfo.put( "accesskey", accessKeys[ 0 ] );
        signInfo.put( "secretkey_prefix", prefix );
        signInfo.put( "algorithm", algorithm );
        signInfo.put( "signature", signatureClient );
        signInfo.put( "string_to_sign", stringData );
        // 登录
        String sessionId = ScmAuthUtils.login( null, null, signInfo );

        // 做业务操作
        ScmAuthUtils.getRootDir( sessionId, wsp.getName() );

        // 登出
        ScmAuthUtils.logout( sessionId );
        runSuccessCount.getAndIncrement();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccessCount.get() == generateData().length
                    || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, username );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
