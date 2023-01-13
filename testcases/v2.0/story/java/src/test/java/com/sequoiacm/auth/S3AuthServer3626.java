package com.sequoiacm.auth;

import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @Description: SCM-3626 :: 指定string_to_sign，进行登录
 * @author fanyu
 * @Date:2020年04月20日
 * @version:1.0
 */
public class S3AuthServer3626 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private String username = "user3626";
    private String password = "user3626123456";
    private String[] accessKeys = null;
    private String algorithm = "HmacSHA256";
    private String roleName = "role_3626";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ScmAuthUtils.createNormalUser( session, wsp.getName(), username,
                password, roleName, ScmPrivilegeType.ALL );
        accessKeys = ScmAuthUtils.refreshAccessKey( session, username, password,
                null );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        // 计算签名
        String prefix = "prefix";
        String[] stringData = { "测试", "!@#￥&^%…….&*<>=/\\",
                TestTools.getRandomString( 16 ), "测试4", "5" };
        byte[] kSecret = ( prefix + accessKeys[ 1 ] ).getBytes();
        byte[] kDate = ScmAuthUtils.sign( stringData[ 0 ], kSecret, algorithm );
        byte[] kRegion = ScmAuthUtils.sign( stringData[ 1 ], kDate, algorithm );
        byte[] kService = ScmAuthUtils.sign( stringData[ 2 ], kRegion,
                algorithm );
        byte[] kSigning = ScmAuthUtils.sign( stringData[ 3 ], kService,
                algorithm );
        byte[] kSignature = ScmAuthUtils.sign( stringData[ 4 ], kSigning,
                algorithm );
        String signatureClient = Base64.encodeAsString( kSignature );
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
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.User.deleteUser( session, username );
                ScmFactory.Role.deleteRole( session, roleName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
