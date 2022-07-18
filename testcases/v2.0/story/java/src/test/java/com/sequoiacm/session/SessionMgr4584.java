package com.sequoiacm.session;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4584:用户创建session池，指定SessionConfig参数
 * @author YiPan
 * @date 2022/6/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4584 extends TestScmBase {
    private ScmSessionMgr sessionMgr;
    private ScmSession session;

    @BeforeClass
    private void setUp() {
    }

    @Test
    private void test() throws ScmException {
        // 设置无效Url
        ScmSessionPoolConf conf = createSessionPoolConf( "123456",
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        try {
            ScmFactory.Session.createSessionMgr( conf );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.INVALID_ARGUMENT ) ) {
                throw e;
            }
        }

        // 设置无效用户名
        conf = createSessionPoolConf( getDefaultUrl(), "user4584",
                TestScmBase.scmPassword );
        ScmSessionMgr sessionMgr = ScmFactory.Session.createSessionMgr( conf );
        try {
            sessionMgr.getSession();
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_UNAUTHORIZED ) ) {
                throw e;
            }
        } finally {
            sessionMgr.close();
        }

        // 设置无效密码
        conf = createSessionPoolConf( getDefaultUrl(), TestScmBase.scmUserName,
                "passwd4584" );
        sessionMgr = ScmFactory.Session.createSessionMgr( conf );
        try {
            sessionMgr.getSession();
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_UNAUTHORIZED ) ) {
                throw e;
            }
        } finally {
            sessionMgr.close();
        }

        // 设置有效url、用户名、密码，获取session并使用
        conf = createSessionPoolConf( getDefaultUrl(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        this.sessionMgr = ScmFactory.Session.createSessionMgr( conf );
        session = this.sessionMgr.getSession();
        long count = ScmFactory.Workspace.count( session,
                new BasicBSONObject() );
        Assert.assertNotEquals( count, 0 );
    }

    @AfterClass
    private void tearDown() {
        session.close();
        sessionMgr.close();
    }

    private ScmSessionPoolConf createSessionPoolConf( String url,
            String username, String password ) throws ScmException {
        List< String > urlList = new ArrayList<>();
        urlList.add( url );
        ScmConfigOption scmConfigOption = new ScmConfigOption( urlList,
                username, password );
        return ScmSessionPoolConf.builder().setSessionConfig( scmConfigOption )
                .get();
    }

    private String getDefaultUrl() {
        return TestScmBase.gateWayList.get( 0 ) + "/"
                + ScmInfo.getSite().getSiteName();
    }
}
