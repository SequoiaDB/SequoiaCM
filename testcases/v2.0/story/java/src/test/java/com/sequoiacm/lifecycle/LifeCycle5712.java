package com.sequoiacm.lifecycle;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.exception.ScmError;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

/**
 * @descreption SCM-5712:修改站点中阶段标签
 * @author YiPan
 * @date 2023/1/16
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5712 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private SiteWrapper branchSite;
    private String tagHot = LifeCycleUtils.tagHot.getName();
    private String tagWarm = LifeCycleUtils.tagWarm.getName();
    private String tagCold = LifeCycleUtils.tagCold.getName();
    private ScmLifeCycleConfig config;

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = TestScmTools.createSession( rootSite );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        LifeCycleUtils.cleanLifeCycleConfig( session );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws ScmException {
        // 未设置全局标签修改表
        try {
            ScmFactory.Site.alterSiteStageTag( session, rootSite.getSiteName(),
                    tagHot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 在全局标签中存在、站点未设置阶段标签，修改标签
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
        try {
            ScmFactory.Site.alterSiteStageTag( session, rootSite.getSiteName(),
                    tagHot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 站点已设置阶段标签，修改标签
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                tagWarm );
        ScmFactory.Site.alterSiteStageTag( session, rootSite.getSiteName(),
                tagHot );
        String actTag = ScmFactory.Site.getSiteStageTag( session,
                rootSite.getSiteName() );
        Assert.assertEquals( actTag, tagHot );

        // 站点标签已被使用，修改标签
        ScmFactory.Site.setSiteStageTag( session, branchSite.getSiteName(),
                tagWarm );
        try {
            ScmFactory.Site.alterSiteStageTag( session,
                    branchSite.getSiteName(), tagHot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 站带标签未被使用，修改标签
        ScmFactory.Site.alterSiteStageTag( session, branchSite.getSiteName(),
                tagCold );
        actTag = ScmFactory.Site.getSiteStageTag( session,
                branchSite.getSiteName() );
        Assert.assertEquals( actTag, tagCold );
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