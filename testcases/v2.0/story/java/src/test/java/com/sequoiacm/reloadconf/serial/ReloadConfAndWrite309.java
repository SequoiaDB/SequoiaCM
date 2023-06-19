package com.sequoiacm.reloadconf.serial;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;

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
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        rootSite = ScmInfo.getRootSite();
        branceSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( rootSite );
            ScmSystem.Configuration.reloadBizConf( ServerScope.ALL_SITE,
                    ScmInfo.getBranchSite().getSiteId(), session );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    //SEQUOIACM-1364
    @Test(groups = { "twoSite", "fourSite" },enabled = false)
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
                session = ScmSessionUtils.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
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
                session = ScmSessionUtils.createSession( branceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                for ( int i = 0; i < 20; i++ ) {
                    ScmFile file = ScmFactory.File.createInstance( ws );
                    file.setContent( filePath );
                    file.setFileName( fileName + "_" + UUID.randomUUID() );
                    fileIdList.add( file.save() );
                }
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
                        TestScmBase.gateWayList.get( 0 ) + "/"
                                + rootSite.getSiteServiceName(),
                        user, passwd );
                session = ScmFactory.Session
                        .createSession( SessionType.NOT_AUTH_SESSION, scOpt );
                ScmSystem.Configuration.reloadBizConf( ServerScope.ALL_SITE,
                        ScmInfo.getRootSite().getSiteId(), session );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}