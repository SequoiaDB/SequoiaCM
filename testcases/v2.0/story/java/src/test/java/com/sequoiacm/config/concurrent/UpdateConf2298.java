package com.sequoiacm.config.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @author fanyu
 * @Description: SCM-2298 :: 服务只有一个节点下，并发修改配置和操作业务
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateConf2298 extends TestScmBase {
    private String fileName = "file2298";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @Test(groups = { "oneSite" })
    private void test() throws Exception {
        Update uThraed = new Update();
        CreateFile cThread = new CreateFile();
        uThraed.start( 3 );
        cThread.start( 100 );
        Assert.assertEquals( uThraed.isSuccess(), true, uThraed.getErrorMsg() );
        Assert.assertEquals( cThread.isSuccess(), true, cThread.getErrorMsg() );
        //write file to check updated configuration take effect
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
                ScmUpdateConfResultSet actResults = ScmSystem.Configuration
                        .setConfigProperties( session, confProp );
                List< String > expServiceNames = new ArrayList< String >();
                expServiceNames.add( site.getSiteServiceName() );
                ConfUtil.checkResultSet( actResults, site.getNodeNum(), 0,
                        expServiceNames, new ArrayList< String >() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CreateFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmWorkspace ws = null;
            ScmId fileId = null;
            try {
                session = TestScmTools.createSession( site );
                ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName + "_" + UUID.randomUUID() );
                file.setContent( filePath );
                fileId = file.save();
                SiteWrapper[] expSites = { site };
                ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                        filePath );
            } finally {
                if ( fileId != null ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
