package com.sequoiacm.session;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4577:用户创建session池，指定池容量
 * @author YiPan
 * @date 2022/6/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4577 extends TestScmBase {
    private ScmSessionMgr sessionMgr;
    private int maxCacheSize = 20;
    private ScmSessionPoolConf scmSessionPoolConf;
    private List< String > sessionIds = new ArrayList<>();
    private List< ScmSession > sessions = new ArrayList<>();

    @BeforeClass
    private void setUp() throws ScmException {
        ScmConfigOption scmConfigOption = TestScmTools
                .getScmConfigOption( ScmInfo.getRootSite().getSiteName() );
        scmSessionPoolConf = ScmSessionPoolConf.builder()
                .setSessionConfig( scmConfigOption ).get();

    }

    @Test
    private void test() throws ScmException {
        // 设置maxCacheSize为无效值
        try {
            scmSessionPoolConf.setMaxCacheSize( 0 );
            Assert.fail("except fail but success");
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.INVALID_ARGUMENT ) ) {
                throw e;
            }
        }
        // 设置为有效值创建
        scmSessionPoolConf.setMaxCacheSize( maxCacheSize );
        sessionMgr = ScmFactory.Session.createSessionMgr( scmSessionPoolConf );

        // 获取session，记录第一次获取的sessionId列表
        for ( int i = 0; i < maxCacheSize; i++ ) {
            ScmSession session = sessionMgr.getSession();
            sessions.add( session );
            sessionIds.add( session.getSessionId() );
        }
        // 回池，占满池空间
        for ( ScmSession session : sessions ) {
            session.close();
        }
        sessions.clear();

        // 再次获取所有
        for ( int i = 0; i < maxCacheSize; i++ ) {
            ScmSession session = sessionMgr.getSession();
            sessions.add( session );
        }

        // 获取第21个session，验证之前的列表中不包含第21个sessionId(即第21个为新建的)
        ScmSession session = sessionMgr.getSession();
        Assert.assertFalse( sessionIds.contains( session.getSessionId() ) );
        sessions.add( session );
    }

    @AfterClass
    private void tearDown() {
        for ( ScmSession session : sessions ) {
            session.close();
        }
        sessionMgr.close();
    }
}
