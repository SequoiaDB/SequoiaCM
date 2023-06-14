package com.sequoiacm.session;

import com.sequoiacm.client.core.ScmSession;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @descreption SCM-6369:使用错误格式的url，创建会话
 * @author ZhangYanan
 * @date 2023/06/09
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */

public class Session6369 extends TestScmBase {
    private static SiteWrapper site = null;

    @BeforeClass
    private void setUp() {
        site = ScmInfo.getSite();
    }

    @Test
    private void test() throws ScmException {
        // url包含特殊字符"/"
        try {
            new ScmConfigOption(
                    gateWayList.get( 0 ) + "/" + site.getSiteName() + ","
                            + gateWayList.get( 0 ) + "/" + site.getSiteName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            Assert.fail( "create session with wrong url should fail" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                    .getErrorCode() ) {
                throw e;
            }
        }

        // url包含特殊字符"%"
        try {
            new ScmConfigOption(
                    gateWayList.get( 0 ) + "/" + site.getSiteName() + "%"
                            + gateWayList.get( 0 ) + "/" + site.getSiteName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            Assert.fail( "create session with wrong url should fail" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                    .getErrorCode() ) {
                throw e;
            }
        }

        // url包含特殊字符";"
        try {
            new ScmConfigOption(
                    gateWayList.get( 0 ) + "/" + site.getSiteName() + ";"
                            + gateWayList.get( 0 ) + "/" + site.getSiteName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            Assert.fail( "create session with wrong url should fail" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                    .getErrorCode() ) {
                throw e;
            }
        }

        // url包含特殊字符"\"
        try {
            new ScmConfigOption(
                    gateWayList.get( 0 ) + "/" + site.getSiteName() + "\\"
                            + gateWayList.get( 0 ) + "/" + site.getSiteName(),
                    TestScmBase.scmUserName, TestScmBase.scmPassword );
            Assert.fail( "create session with wrong url should fail" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                    .getErrorCode() ) {
                throw e;
            }
        }

        // 不存在的站点名
        ScmConfigOption option = new ScmConfigOption(
                gateWayList.get( 0 ) + "/" + "notExistSite",
                TestScmBase.scmUserName, TestScmBase.scmPassword );

        try ( ScmSession session = ScmFactory.Session
                .createSession( SessionType.AUTH_SESSION, option )) {
            ScmFactory.Workspace.getWorkspace( ScmInfo.getWs().getName(),
                    session );
            Assert.fail( "create session with wrong url should fail" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_NOT_FOUND.getErrorCode()
                    && !e.getMessage().contains( "notExistSite" ) ) {
                throw e;
            }
        }

        // 不存在的网关
        option = new ScmConfigOption(
                "192.168.266.266:9999999999" + "/" + site.getSiteName(),
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        try {
            ScmFactory.Session.createSession( SessionType.AUTH_SESSION,
                    option );
            Assert.fail( "create session with wrong url should fail" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.NETWORK_IO.getErrorCode() ) {
                throw e;
            }
        }

    }

    @AfterClass
    private void tearDown() {
    }
}
