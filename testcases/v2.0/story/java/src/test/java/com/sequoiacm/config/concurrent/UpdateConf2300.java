package com.sequoiacm.config.concurrent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @author fanyu
 * @Description: SCM-2300 :: 并发修改配置和创建ws操作业务
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateConf2300 extends TestScmBase {
    private String fileName = "file2300";
    private SiteWrapper site = null;
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
        site = ScmInfo.getSite();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        Update uThraed = new Update();
        CreateWsAndFile cThread = new CreateWsAndFile();
        uThraed.start();
        cThread.start();
        Assert.assertEquals( uThraed.isSuccess(), true, uThraed.getErrorMsg() );
        Assert.assertEquals( cThread.isSuccess(), true, cThread.getErrorMsg() );
        // check local configuration
        Map< String, String > map = new HashMap< String, String >();
        map.put( ConfigCommonDefind.scm_audit_mask, "ALL" );
        map.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        for ( NodeWrapper node : site.getNodes( site.getNodeNum() ) ) {
            ConfUtil.checkUpdatedConf( node.getUrl(), map );
        }
        // check updated configuration take effect
        ConfUtil.checkTakeEffect( site, fileName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
        TestTools.LocalFile.removeFile( localPath );
    }

    private class Update extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service( site.getSiteServiceName() )
                        .updateProperty( ConfigCommonDefind.scm_audit_mask,
                                "ALL" )
                        .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                                "LOCAL" )
                        .build();
                ScmUpdateConfResultSet result = ScmSystem.Configuration
                        .setConfigProperties( session, confProp );
                System.out.println( "result = " + result.toString() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CreateWsAndFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            String wsName = "ws2300_" + UUID.randomUUID();
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspaceUtil.createWS( session, wsName,
                        ScmInfo.getSiteNum() );
                ScmWorkspaceUtil.wsSetPriority( session, wsName );
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName + "_" + UUID.randomUUID() );
                file.setContent( filePath );
                ScmId fileId = file.save();
                SiteWrapper[] expSites = { site };
                ScmFileUtils.checkMeta( ws, fileId, expSites );
                ScmFileUtils.checkData( ws, fileId, localPath, filePath );
            } finally {
                ScmWorkspaceUtil.deleteWs( wsName, session );
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
