package com.sequoiacm.lifecycle.concurrent;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5723:修改站点阶段标签与删除站点标签操作并发
 * @author YiPan
 * @date 2023/1/17
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5723 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagWarm = LifeCycleUtils.tagWarm.getName();
    private ScmLifeCycleConfig config;

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        LifeCycleUtils.cleanLifeCycleConfig( session );
    }

    @Test
    public void test() throws Exception {
        // 设置全局标签
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );

        // 设置标签为hot
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                tagHot );

        ThreadExecutor t = new ThreadExecutor();
        AlterTag alterTag = new AlterTag();
        t.addWorker( new UnSetTag() );
        t.addWorker( alterTag );
        t.run();

        String actTag = ScmFactory.Site.getSiteStageTag( session,
                rootSite.getSiteName() );
        Assert.assertEquals( actTag, "" );
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }

    private class UnSetTag extends ResultStore {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = ScmSessionUtils.createSession( rootSite );
            try {
                ScmFactory.Site.unsetSiteStageTag( session,
                        rootSite.getSiteName() );
            } finally {
                session.close();
            }
        }
    }

    private class AlterTag extends ResultStore {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = ScmSessionUtils.createSession( rootSite );
            try {
                ScmFactory.Site.alterSiteStageTag( session,
                        rootSite.getSiteName(), tagWarm );
            } catch ( ScmException e ) {
                if ( !e.getError()
                        .equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                    throw e;
                }
            } finally {
                session.close();
            }
        }
    }
}