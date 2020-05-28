package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2316:正常删除一个配置项，所有服务一个节点下
 * @author fanyu
 * @Date:2018年12月13日
 * @version:1.0
 */
public class DeleteConf2316 extends TestScmBase {
    private String fileName = "file2316";
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getSite();
        ConfUtil.restore( null );
    }

    @Test(groups = { "oneSite" })
    private void test() throws Exception {
        testAllInstnace();
        // null is allInstance
        ConfUtil.restore( null );

        testInstnace();
        ConfUtil.restore( null );

        testInstnaces();
        ConfUtil.restore( null );

        testServiceList();
        ConfUtil.restore( null );

        testService();
        ConfUtil.restore( null );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    private void testAllInstnace() throws Exception {
        ScmSession session = null;
        try {
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .allInstance()
                    .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                    .deleteProperty( ConfigCommonDefind.scm_audit_userMask )
                    .build();
            session = TestScmTools.createSession( site );
            ScmUpdateConfResultSet actResult = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            Assert.assertTrue( actResult.getFailures().size() >= 0,
                    actResult.getFailures().toString() );

            List< String > list = new ArrayList< String >();
            list.add( ConfigCommonDefind.scm_audit_mask );
            list.add( ConfigCommonDefind.scm_audit_userMask );

            // check content-server
            for ( NodeWrapper node : ScmInfo.getAllNodes() ) {
                ConfUtil.checkDeletedConf( node.getUrl(), list );
            }
            ConfUtil.checkNotTakeEffect( site, fileName );

            // check schedule-server
            List< ScmServiceInstance > scheList = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, "schedule-server" );
            for ( ScmServiceInstance instance : scheList ) {
                ConfUtil.checkDeletedConf(
                        instance.getIp() + ":" + instance.getPort(), list );
            }

            // check auth-server
            List< ScmServiceInstance > authList = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, "auth-server" );
            for ( ScmServiceInstance instance : authList ) {
                ConfUtil.checkDeletedConf(
                        instance.getIp() + ":" + instance.getPort(), list );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void testInstnace() throws Exception {
        ScmSession session = null;
        NodeWrapper node = null;
        try {
            node = site.getNode();
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .instance( node.getUrl() )
                    .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                    .deleteProperty( ConfigCommonDefind.scm_audit_userMask )
                    .build();
            session = TestScmTools.createSession( site );
            ScmUpdateConfResultSet actResult = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            List< String > okServices = new ArrayList< String >();
            okServices.add( site.getSiteServiceName() );
            ConfUtil.checkResultSet( actResult, 1, 0, okServices,
                    new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

        List< String > list = new ArrayList< String >();
        list.add( ConfigCommonDefind.scm_audit_mask );
        list.add( ConfigCommonDefind.scm_audit_userMask );
        ConfUtil.checkDeletedConf( node.getUrl(), list );
    }

    private void testInstnaces() throws Exception {
        ScmSession session = null;
        try {
            List< String > instances = new ArrayList< String >();
            for ( NodeWrapper node : site.getNodes( site.getNodeNum() ) ) {
                instances.add( node.getUrl() );
            }
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .instances( instances )
                    .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                    .deleteProperty( ConfigCommonDefind.scm_audit_userMask )
                    .build();
            session = TestScmTools.createSession( site );
            ScmUpdateConfResultSet actResult = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            List< String > okServices = new ArrayList< String >();
            okServices.add( site.getSiteServiceName() );
            ConfUtil.checkResultSet( actResult, site.getNodeNum(), 0,
                    okServices, new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

        List< String > list = new ArrayList< String >();
        list.add( ConfigCommonDefind.scm_audit_mask );
        list.add( ConfigCommonDefind.scm_audit_userMask );
        for ( NodeWrapper node : site.getNodes( site.getNodeNum() ) ) {
            ConfUtil.checkDeletedConf( node.getUrl(), list );
        }
        ConfUtil.checkNotTakeEffect( site, fileName );
    }

    private void testServiceList() throws Exception {
        ScmSession session = null;
        try {
            List< String > serviceList = new ArrayList< String >();
            serviceList.add( site.getSiteServiceName() );

            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .services( serviceList )
                    .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                    .deleteProperty( ConfigCommonDefind.scm_audit_userMask )
                    .build();
            session = TestScmTools.createSession( site );
            ScmUpdateConfResultSet actResult = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            List< String > okServices = new ArrayList< String >();
            okServices.add( site.getSiteServiceName() );
            ConfUtil.checkResultSet( actResult, site.getNodeNum(), 0,
                    okServices, new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

        List< String > list = new ArrayList< String >();
        list.add( ConfigCommonDefind.scm_audit_mask );
        list.add( ConfigCommonDefind.scm_audit_userMask );
        for ( NodeWrapper node : site.getNodes( site.getNodeNum() ) ) {
            ConfUtil.checkDeletedConf( node.getUrl(), list );
        }
        ConfUtil.checkNotTakeEffect( site, fileName );
    }

    private void testService() throws Exception {
        ScmSession session = null;
        try {
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                    .deleteProperty( ConfigCommonDefind.scm_audit_userMask )
                    .build();
            session = TestScmTools.createSession( site );
            ScmUpdateConfResultSet actResult = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );
            List< String > okServices = new ArrayList< String >();
            okServices.add( site.getSiteServiceName() );
            ConfUtil.checkResultSet( actResult, site.getNodeNum(), 0,
                    okServices, new ArrayList< String >() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }

        List< String > list = new ArrayList< String >();
        list.add( ConfigCommonDefind.scm_audit_mask );
        list.add( ConfigCommonDefind.scm_audit_userMask );
        for ( NodeWrapper node : site.getNodes( site.getNodeNum() ) ) {
            ConfUtil.checkDeletedConf( node.getUrl(), list );
        }
        ConfUtil.checkNotTakeEffect( site, fileName );
    }
}
