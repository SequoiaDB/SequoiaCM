package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2319 :: 删除不存在的配置项
 * @author fanyu
 * @Date:2018年12月13日
 * @version:1.0
 */
public class DeleteConf2319 extends TestScmBase {
    private SiteWrapper site = null;
    private String fileName = "file2319";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getRootSite();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @Test
    private void test() throws Exception {
        deleteConf();
        deleteConf();
        //check
        ConfUtil.checkNotTakeEffect( site, fileName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    private void deleteConf() throws ScmException {
        ScmSession session = null;
        try {
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service( site.getSiteServiceName() )
                    .deleteProperty( ConfigCommonDefind.scm_audit_mask )
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
    }
}
