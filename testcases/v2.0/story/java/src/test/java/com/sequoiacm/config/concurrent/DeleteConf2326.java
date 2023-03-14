package com.sequoiacm.config.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2326 :: 并发删除不同服务的配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class DeleteConf2326 extends TestScmBase {
    private String fileName = "file2326";
    private List< SiteWrapper > siteList = null;
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        siteList = ScmInfo.getAllSites();
        for ( SiteWrapper site : siteList ) {
            ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        List< Delete > list = new ArrayList< Delete >();
        for ( SiteWrapper site : siteList ) {
            list.add( new Delete( site ) );
        }

        for ( Delete d : list ) {
            d.start();
        }

        for ( Delete d : list ) {
            Assert.assertTrue( d.isSuccess(), d.getErrorMsg() );
        }

        List< String > deletedList = new ArrayList< String >();
        deletedList.add( ConfigCommonDefind.scm_audit_userMask );
        deletedList.add( ConfigCommonDefind.scm_audit_mask );

        for ( SiteWrapper site : siteList ) {
            ConfUtil.checkNotTakeEffect( site, fileName );
            for ( NodeWrapper node : site.getNodes( site.getNodeNum() ) ) {
                ConfUtil.checkDeletedConf( node.getUrl(), deletedList );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        for ( SiteWrapper site : siteList ) {
            ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        }
        TestTools.LocalFile.removeFile( localPath );
    }

    private class Delete extends TestThreadBase {
        private SiteWrapper site = null;

        public Delete( SiteWrapper site ) {
            this.site = site;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmUpdateConfResultSet actResult = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service( site.getSiteServiceName() )
                        .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                        .deleteProperty( ConfigCommonDefind.scm_audit_userMask )
                        .build();
                actResult = ScmSystem.Configuration
                        .setConfigProperties( session, confProp );
                List< String > expServiceNames = new ArrayList< String >();
                expServiceNames.add( site.getSiteServiceName() );
                ConfUtil.checkResultSet( actResult, site.getNodeNum(), 0,
                        expServiceNames, new ArrayList< String >() );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( "delete conf failed, actResult = "
                        + actResult.toString() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
