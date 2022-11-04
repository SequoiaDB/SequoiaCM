package com.sequoiacm.testcommon.listener;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.IAlterSuiteListener;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;
import org.testng.xml.XmlSuite;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @descreption 用于对用例站点和网络模型分组
 * @author YiPan
 * @date 2022/8/17
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class GroupListener
        implements IAnnotationTransformer, IAlterSuiteListener {
    private static final org.apache.log4j.Logger logger = Logger
            .getLogger( GroupListener.class );
    private static String sites = null;
    private static String gateway = null;
    private static String scmUser = null;
    private static String scmPasswd = null;
    private static String rootSiteName = null;
    private static boolean runBaseTest;
    private static String ignoreStrategyTag = null;

    @Override
    public void alter( List< XmlSuite > suites ) {
        XmlSuite xml = suites.get( 0 );
        // 读取xml配置
        String[] gateways = xml.getParameter( "GATEWAYS" ).split( "," );
        gateway = gateways[ 0 ];
        sites = xml.getParameter( "SITES" );
        scmUser = xml.getParameter( "SCMUSER" );
        scmPasswd = xml.getParameter( "SCMPASSWD" );
        rootSiteName = xml.getParameter( "ROOTSITESVCNAME" );
        runBaseTest = Boolean.parseBoolean( xml.getParameter( "RUNBASETEST" ) );
        ignoreStrategyTag = chooseIgnoreStrategyTag( getStrategy() );
    }

    @Override
    public void transform( ITestAnnotation annotation, Class testClass,
            Constructor testConstructor, Method testMethod ) {
        List< String > groupTags = new ArrayList<>(
                Arrays.asList( annotation.getGroups() ) );
        if ( runBaseTest ) {
            runBaseTest( annotation, groupTags );
        } else {
            runNormalTest( annotation, groupTags );
        }
    }

    /**
     * @Descreption 执行基本功能用例
     * @param annotation
     * @param groupTags
     */
    private static void runBaseTest( ITestAnnotation annotation,
            List< String > groupTags ) {
        if ( !groupTags.contains( GroupTags.base ) ) {
            annotation.setEnabled( false );
        } else {
            runNormalTest( annotation, groupTags );
        }
    }

    /**
     * @Descreption 执行所有用例
     * @param annotation
     * @param groupTags
     */
    private static void runNormalTest( ITestAnnotation annotation,
            List< String > groupTags ) {
        groupTags.remove( GroupTags.base );
        if ( groupTags.size() != 0 ) {
            // 网络模型判断,筛选出所有用例指定的网络模型与SCM集群环境网络模型不同的用例屏蔽
            if ( groupTags.contains( ignoreStrategyTag ) ) {
                annotation.setEnabled( false );
            }
            // 站点数判断
            if ( !( groupTags.contains( sites ) ) ) {
                annotation.setEnabled( false );
            }
        }
    }

    /**
     * @descreption 获取SCM集群的网络模型
     * @return
     */
    private static String getStrategy() {
        String strategy = null;
        ScmSession session = null;
        try {
            session = ScmFactory.Session.createSession( new ScmConfigOption(
                    gateway + "/" + rootSiteName, scmUser, scmPasswd ) );
            ScmType.SiteStrategyType siteStrategy = ScmFactory.Site
                    .getSiteStrategy( session );
            if ( siteStrategy == ScmType.SiteStrategyType.Star ) {
                strategy = GroupTags.star;
            } else {
                strategy = GroupTags.net;
            }
        } catch ( ScmException e ) {
            logger.error( e );
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
        return strategy;
    }

    /**
     * @descreption 逻辑非判断，根据传参的集群环境判断出需要屏蔽的分组标签类型
     * @param strategy
     * @return
     */
    private static String chooseIgnoreStrategyTag( String strategy ) {
        String ignoreStrategyTag = null;
        if ( strategy.equals( GroupTags.net ) ) {
            ignoreStrategyTag = GroupTags.star;
        } else {
            ignoreStrategyTag = GroupTags.net;
        }
        return ignoreStrategyTag;
    }
}
