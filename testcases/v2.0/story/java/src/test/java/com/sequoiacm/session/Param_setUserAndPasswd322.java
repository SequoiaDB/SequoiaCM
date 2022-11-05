package com.sequoiacm.session;

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
 * @FileName SCM-322: setUserAndPasswd无效参数校验
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、ScmConfigOption接口无效参数校验,覆盖： a.用户不存在； b.用户、密码不匹配； c.user：null、空串；
 * d.pwd：null、空串； 2、检查执行结果；
 */

public class Param_setUserAndPasswd322 extends TestScmBase {
    private static SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        site = ScmInfo.getSite();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        // inexistent user
        try {
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName() );
            scOpt.setUser( "thisuserisnotexist" );
            scOpt.setPasswd( TestScmBase.scmPassword );
            ScmFactory.Session.createSession( SessionType.AUTH_SESSION, scOpt );
            Assert.fail( "create session shouldn't succeed when user is "
                    + "inexistent" );
        } catch ( ScmException e ) {
            if ( 401 != e.getErrorCode() ) { // SCM_BUSINESS_LOGIN_FAILED
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        // wrong password
        try {
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName() );
            scOpt.setUser( TestScmBase.scmUserName );
            scOpt.setPasswd( TestScmBase.scmPassword + "_plus" );
            ScmFactory.Session.createSession( SessionType.AUTH_SESSION, scOpt );
            Assert.fail(
                    "create session shouldn't succeed when password is wrong" );
        } catch ( ScmException e ) {
            if ( 401 != e.getErrorCode() ) { // SCM_BUSINESS_LOGIN_FAILED
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        // user is null
        try {
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName() );
            scOpt.setUser( null );
            scOpt.setPasswd( TestScmBase.scmPassword );
            ScmFactory.Session.createSession( SessionType.AUTH_SESSION, scOpt );
            Assert.fail( "create session shouldn't succeed when user is null" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                    .getErrorCode() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        // user is ""
        try {
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName() );
            scOpt.setUser( "" );
            scOpt.setPasswd( TestScmBase.scmPassword );
            ScmFactory.Session.createSession( SessionType.AUTH_SESSION, scOpt );
            Assert.fail( "create session shouldn't succeed when user is empty "
                    + "string" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_UNAUTHORIZED.getErrorCode()
                    && e.getErrorCode() != ScmError.HTTP_NOT_FOUND
                            .getErrorCode() ) { // SCM_BUSINESS_LOGIN_FAILED
                throw e;
            }
        }
        // password is null
        try {
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName() );
            scOpt.setUser( TestScmBase.scmUserName );
            scOpt.setPasswd( null );
            ScmFactory.Session.createSession( SessionType.AUTH_SESSION, scOpt );
            Assert.fail(
                    "create session shouldn't succeed when password is null" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                    .getErrorCode() ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        // password is ""
        try {
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName() );
            scOpt.setUser( TestScmBase.scmUserName );
            scOpt.setPasswd( "" );
            ScmFactory.Session.createSession( SessionType.AUTH_SESSION, scOpt );
            Assert.fail(
                    "create session shouldn't succeed when password is empty string" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.HTTP_UNAUTHORIZED
                    .getErrorCode() ) { // SCM_BUSINESS_LOGIN_FAILED
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {

    }

}
