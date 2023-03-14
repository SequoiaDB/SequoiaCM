package com.sequoiacm.session;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4575:session池未满，获取所有session
 * @author YiPan
 * @date 2022/6/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4575 extends TestScmBase {
    private ScmSessionMgr sessionMgr;
    private int maxCacheSize;
    private List< String > sessionIds1 = new ArrayList<>();
    private List< String > sessionIds2 = new ArrayList<>();
    private List< ScmSession > sessions = new ArrayList<>();

    @BeforeClass
    private void setUp() throws ScmException {
        sessionMgr = ScmSessionUtils.createSessionMgr( ScmInfo.getRootSite() );
        ScmSessionPoolConf scmSessionPoolConf = ScmSessionPoolConf.builder()
                .get();
        maxCacheSize = scmSessionPoolConf.getMaxCacheSize();
    }

    @Test
    private void test() throws ScmException {
        // 获取池容量一半的session
        for ( int i = 0; i < maxCacheSize / 2; i++ ) {
            ScmSession session = sessionMgr.getSession();
            sessionIds1.add( session.getSessionId() );
            sessions.add( session );
        }

        // 回池
        for ( ScmSession session : sessions ) {
            session.close();
        }
        sessions.clear();

        // 再次获取
        for ( int i = 0; i < maxCacheSize / 2; i++ ) {
            ScmSession session = sessionMgr.getSession();
            sessionIds2.add( session.getSessionId() );
            sessions.add( session );
        }

        // 校验两次获取的sessionId列表
        Assert.assertEqualsNoOrder( sessionIds1.toArray(),
                sessionIds2.toArray() );

        // 使用第二获取的session并回池
        for ( ScmSession session : sessions ) {
            long count = ScmFactory.Workspace.count( session,
                    new BasicBSONObject() );
            Assert.assertNotEquals( count, 0 );
            session.close();
        }
    }

    @AfterClass
    private void tearDown() {
        sessionMgr.close();
    }
}
