package com.sequoiacm.lifecycle.concurrent;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @descreption SCM-5725:设置站点标签操作与列取站点标签操作并发
 * @author YiPan
 * @date 2023/1/17
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5725 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private String tagHot = LifeCycleUtils.tagHot.getName();
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
        ThreadExecutor t = new ThreadExecutor();
        ListTag listTag = new ListTag();
        t.addWorker( new SetTag() );
        t.addWorker( listTag );
        t.run();

        // 校验list线程结果
        if ( !listTag.getRootSiteTag().equals( tagHot ) ) {
            Assert.assertEquals( listTag.getRootSiteTag(), "" );
        }

        // 校验最终结果
        String siteStageTag = ScmFactory.Site.getSiteStageTag( session,
                rootSite.getSiteName() );
        if ( !siteStageTag.equals( tagHot ) ) {
            Assert.assertEquals( siteStageTag, "" );
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }

    private class SetTag extends ResultStore {
        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = ScmSessionUtils.createSession( rootSite );
            try {
                ScmFactory.Site.setSiteStageTag( session,
                        rootSite.getSiteName(), tagHot );
            } finally {
                session.close();
            }
        }
    }

    private class ListTag extends ResultStore {
        private String rootSiteTag;

        public String getRootSiteTag() {
            return rootSiteTag;
        }

        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = ScmSessionUtils.createSession( rootSite );
            try {
                ScmCursor< ScmSiteInfo > cursor = ScmFactory.Site
                        .listSite( session );
                while ( cursor.hasNext() ) {
                    ScmSiteInfo siteInfo = cursor.getNext();
                    if ( siteInfo.getName().equals( rootSite.getSiteName() ) ) {
                        rootSiteTag = siteInfo.getStageTag();
                    }
                }
                cursor.close();
            } finally {
                session.close();
            }
        }
    }
}