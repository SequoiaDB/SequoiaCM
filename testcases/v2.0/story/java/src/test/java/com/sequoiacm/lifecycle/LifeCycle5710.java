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
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5710:站点中设置阶段标签
 * @author YiPan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5710 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private SiteWrapper branchSite;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagWarm = LifeCycleUtils.tagWarm.getName();
    private ScmLifeCycleConfig config;

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = ScmSessionUtils.createSession( rootSite );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        LifeCycleUtils.cleanLifeCycleConfig( session );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws ScmException {
        // 全局配置中不存在
        try {
            ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                    tagHot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 全局配置中存在、主站点未设置标签
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                tagHot );
        String actTag = ScmFactory.Site.getSiteStageTag( session,
                rootSite.getSiteName() );
        Assert.assertEquals( actTag, tagHot );

        // 主站点已设置标签
        try {
            ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                    tagWarm );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 分站点使用标签，标签已被使用
        try {
            ScmFactory.Site.setSiteStageTag( session, branchSite.getSiteName(),
                    tagHot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 分站点使用标签，标签未使用
        ScmFactory.Site.setSiteStageTag( session, branchSite.getSiteName(),
                tagWarm );
        actTag = ScmFactory.Site.getSiteStageTag( session,
                branchSite.getSiteName() );
        Assert.assertEquals( actTag, tagWarm );

    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
        } finally {
            LifeCycleUtils.cleanLifeCycleConfig( session );
            session.close();
        }
    }

}