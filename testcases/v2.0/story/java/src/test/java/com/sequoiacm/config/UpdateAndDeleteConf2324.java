package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2324 :: 多次删除和更新配置项
 * @Date:2018年12月17日
 * @version:1.0
 */
public class UpdateAndDeleteConf2324 extends TestScmBase {
    private String fileName = "file2324";
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getSite();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        ScmSession session = null;
        try {
            ScmConfigProperties deleteConfProp = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                    .build();

            ScmConfigProperties updateConfProp = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();

            session = ScmSessionUtils.createSession( site );
            for ( int i = 0; i < 3; i++ ) {
                ScmUpdateConfResultSet actDelResult = ScmSystem.Configuration
                        .setConfigProperties( session, deleteConfProp );
                List< String > okServices1 = new ArrayList< String >();
                okServices1.add( site.getSiteServiceName() );
                ConfUtil.checkResultSet( actDelResult, site.getNodeNum(), 0,
                        okServices1, new ArrayList< String >() );

                ScmUpdateConfResultSet actUpResult = ScmSystem.Configuration
                        .setConfigProperties( session, updateConfProp );
                List< String > okServices2 = new ArrayList< String >();
                okServices2.add( site.getSiteServiceName() );
                ConfUtil.checkResultSet( actUpResult, site.getNodeNum(), 0,
                        okServices2, new ArrayList< String >() );
            }
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
