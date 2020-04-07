package com.sequoiacm.config.concurrent;

import java.io.File;
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
 * @Description: SCM-2299 :: 服务下有多个节点，并发修改配置和操作业务
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateConf2299 extends TestScmBase {
    private String fileName = "file2299";
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private int fileSize = 1024 * 1;
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
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        ScmConfigProperties confProp1 = ScmConfigProperties.builder()
                .service( site.getSiteServiceName() )
                .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                .build();

        ScmConfigProperties confProp2 = ScmConfigProperties.builder()
                .service( site.getSiteServiceName() )
                .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                        "LOCAL" )
                .build();

        Update uThread1 = new Update( confProp1 );
        Update uThread2 = new Update( confProp2 );
        CreateFile cThread = new CreateFile();

        uThread1.start();
        uThread2.start();
        cThread.start( 10 );
        Assert.assertEquals( uThread1.isSuccess(), true,
                uThread1.getErrorMsg() );
        Assert.assertEquals( uThread2.isSuccess(), true,
                uThread2.getErrorMsg() );
        Assert.assertEquals( cThread.isSuccess(), true, cThread.getErrorMsg() );
        //check updated configuration take effect
        ConfUtil.checkTakeEffect( site, fileName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( site.getSiteServiceName() );
    }

    private class Update extends TestThreadBase {
        private ScmConfigProperties confProp = null;

        public Update( ScmConfigProperties confProp ) {
            this.confProp = confProp;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmUpdateConfResultSet result = ScmSystem.Configuration
                        .setConfigProperties( session, this.confProp );
                System.out.println( "result = " + result.toString() );
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
