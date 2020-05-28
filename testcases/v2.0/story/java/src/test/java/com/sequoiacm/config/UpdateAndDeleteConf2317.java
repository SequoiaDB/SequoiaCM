package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2317 ::更新和删除相同的配置项
 * @Date:2018年12月17日
 * @version:1.0
 */
public class UpdateAndDeleteConf2317 extends TestScmBase {
    private String fileName = "file2317";
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getSite();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @Test
    private void test() throws Exception {
        ScmSession session = null;
        try {
            List< String > properties = new ArrayList< String >();
            properties.add( ConfigCommonDefind.scm_audit_mask );
            properties.add( ConfigCommonDefind.scm_audit_userMask );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .deleteProperties( properties ).build();
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

        Map< String, String > map = new HashMap< String, String >();
        map.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        map.put( ConfigCommonDefind.scm_audit_mask, "ALL" );
        for ( NodeWrapper node : site.getNodes( site.getNodeNum() ) ) {
            ConfUtil.checkUpdatedConf( node.getUrl(), map );
        }

        ConfUtil.checkTakeEffect( site, fileName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }
}
