package com.sequoiacm.lifecycle;

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
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;

/**
 * @descreption SCM-5713:不同用户修改站点中的阶段标签
 * @author YiPan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5713 extends TestScmBase {
    private ScmSession session;
    private ScmSession normalUserSession;
    private String username = "user5713";
    private String passwd = "pwd5713";
    private SiteWrapper rootSite;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagWarm = LifeCycleUtils.tagWarm.getName();
    private ScmLifeCycleConfig config;

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        ScmAuthUtils.createUser( session, username, passwd );
        normalUserSession = ScmSessionUtils.createSession( rootSite, username,
                passwd );

        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        LifeCycleUtils.cleanLifeCycleConfig( session );
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                tagHot );
    }

    @Test
    public void test() throws ScmException {
        // 普通用户修改
        try {
            ScmFactory.Site.alterSiteStageTag( normalUserSession,
                    rootSite.getSiteName(), tagWarm );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_UNAUTHORIZED ) ) {
                throw e;
            }
        }

        // 管理员用户修改
        ScmFactory.Site.alterSiteStageTag( session, rootSite.getSiteName(),
                tagWarm );
        String actTag = ScmFactory.Site.getSiteStageTag( session,
                rootSite.getSiteName() );
        Assert.assertEquals( actTag, tagWarm );
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmAuthUtils.deleteUser( session, username );
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
            normalUserSession.close();
        }
    }
}