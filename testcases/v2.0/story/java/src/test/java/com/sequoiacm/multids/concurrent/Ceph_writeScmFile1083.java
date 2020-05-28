package com.sequoiacm.multids.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-1083:多线程并发跨中心读取同一文件
 * @author huangxiaoni init
 * @date 2018.1.23
 */

public class Ceph_writeScmFile1083 extends TestScmBase {
    private static ScmSession sessionA = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmWorkspace wsA = null;

    private String fileName = "scm1083";
    private ScmId fileId = null;
    private int fileSize = 22 * 1024 * 1024;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            sessionA = TestScmTools.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            fileId = ScmFileUtils.create( wsA, fileName, filePath );
        } catch ( IOException | ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            ReadFileFromCenterB readFromB = new ReadFileFromCenterB();
            readFromB.start( 20 );
            if ( !( readFromB.isSuccess() ) ) {
                Assert.fail( readFromB.getErrorMsg() );
            }

            // check results
            SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                    branSites.get( 1 ) };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private class ReadFileFromCenterB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmInputStream sis = null;
            OutputStream fos = null;
            try {
                // login
                session = TestScmTools.createSession( branSites.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                sis = ScmFactory.File.createInputStream( file );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                fos = new FileOutputStream( downloadPath );
                sis.read( fos );

                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } finally {
                if ( null != fos ) {
                    fos.close();
                }
                if ( null != sis ) {
                    sis.close();
                }
                if ( null != session ) {
                    session.close();
                }
            }
        }
    }

}
