package com.sequoiacm.lifecycle;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleStageTag;
import com.sequoiacm.exception.ScmError;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.LifeCycleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-5740:移除全局阶段标签验证
 * @author YiPan
 * @date 2023/1/18
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class LifeCycle5740 extends TestScmBase {
    private ScmSession session;
    private SiteWrapper rootSite;
    private ScmLifeCycleConfig config;
    private static String hot = LifeCycleUtils.tagHot.getName();
    private static String newTag = "tag5740";
    private static String defaultHot = "Hot";

    @BeforeClass
    public void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        LifeCycleUtils.cleanLifeCycleConfig( session );
        config = LifeCycleUtils.getDefaultScmLifeCycleConfig();
        ScmSystem.LifeCycleConfig.setLifeCycleConfig( session, config );
    }

    @Test
    public void test() throws ScmException {
        ScmSystem.LifeCycleConfig.addStageTag( session, newTag, newTag );
        // 存在、且没有全局流和站点使用的非内置标签
        ScmSystem.LifeCycleConfig.removeStageTag( session, newTag );
        ScmLifeCycleConfig actConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session );
        LifeCycleUtils.checkScmLifeCycleConfigByBson( actConfig, config );

        // 存在、且有全局流使用
        try {
            ScmSystem.LifeCycleConfig.removeStageTag( session, hot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 存在、且有站点使用
        ScmSystem.LifeCycleConfig.addStageTag( session, newTag, newTag );
        ScmFactory.Site.setSiteStageTag( session, rootSite.getSiteName(),
                newTag );
        try {
            ScmSystem.LifeCycleConfig.removeStageTag( session, newTag );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_BAD_REQUEST ) ) {
                throw e;
            }
        }
        List< String > allTags = getAllTags( session );
        Assert.assertTrue( allTags.contains( newTag ) );

        // 存在、且为内置标签
        try {
            ScmSystem.LifeCycleConfig.removeStageTag( session, defaultHot );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }
        allTags = getAllTags( session );
        Assert.assertTrue( allTags.contains( defaultHot ) );

        // 不存在
        try {
            ScmSystem.LifeCycleConfig.removeStageTag( session, "test5740" );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
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

    private List< String > getAllTags( ScmSession session )
            throws ScmException {
        List< ScmLifeCycleStageTag > stageTagConfig = ScmSystem.LifeCycleConfig
                .getLifeCycleConfig( session ).getStageTagConfig();
        List< String > tags = new ArrayList<>();
        for ( ScmLifeCycleStageTag tag : stageTagConfig ) {
            tags.add( tag.getName() );
        }
        return tags;
    }
}