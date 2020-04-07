package com.sequoiacm.reloadconf.serial;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-309:有业务在运行，刷新配置
 * @author fanyu init
 * @date 2018.01.31
 */

public class ReloadConfAndWrite309 extends TestScmBase {
    private static WsWrapper wsp = null;
    private String fileName = "ReloadConfAndWrite309";
    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private List< ScmId > fileIdList = new CopyOnWriteArrayList< ScmId >();
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            wsp = ScmInfo.getWs();

        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        Write w = new Write();
        ReloadConf r = new ReloadConf();
        w.start( 20 );
        r.start( 3 );
        Assert.assertEquals( w.isSuccess(), true, w.getErrorMsg() );
        Assert.assertEquals( r.isSuccess(), true, r.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        ScmSession session = null;
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class Write extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                for ( int i = 0; i < 20; i++ ) {
                    ScmFile file = ScmFactory.File.createInstance( ws );
                    file.setContent( filePath );
                    file.setFileName( fileName + "_" + UUID.randomUUID() );
                    fileIdList.add( file.save() );
                }
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class ReloadConf extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                String user = TestScmBase.scmUserName;
                String passwd = TestScmBase.scmPassword;
                ScmConfigOption scOpt = new ScmConfigOption(
                        TestScmBase.gateWayList.get( 0 ) + "/" +
                                rootSite.getSiteServiceName(), user, passwd );
                session = ScmFactory.Session
                        .createSession( SessionType.NOT_AUTH_SESSION, scOpt );
                ScmSystem.Configuration.reloadBizConf( ServerScope.ALL_SITE,
                        ScmInfo.getRootSite().getSiteId(), session );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}