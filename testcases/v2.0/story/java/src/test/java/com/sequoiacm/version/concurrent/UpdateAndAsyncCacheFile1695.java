package com.sequoiacm.version.concurrent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content: update Content by breakPointfile and asyncCache the same file
 * concurrently: a.update content by breakPointFile b.asyncCache the file
 * testlink-case:SCM-1695
 *
 * @author wuyan
 * @Date 2018.06.19
 * @version 1.00
 */

public class UpdateAndAsyncCacheFile1695 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "versionfile1695";
    private String authorName = "author1695";
    private byte[] writeData = new byte[ 1024 * 1024 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        System.out.println( "branSite=" + branSite.getSiteId() );
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByStream( wsM, fileName, writeData,
                authorName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int updateSize = 1024 * 900;
        byte[] updateData = new byte[ updateSize ];

        createBreakPointFile( wsM, updateData );
        AsyncCacheFile asyncCacheFile = new AsyncCacheFile();
        UpdateFile updateFile = new UpdateFile();
        asyncCacheFile.start();
        updateFile.start();

        Assert.assertTrue( updateFile.isSuccess(), updateFile.getErrorMsg() );
        Assert.assertTrue( asyncCacheFile.isSuccess(),
                asyncCacheFile.getErrorMsg() );

        checkUpdateAndAsyncCacheFileResult( wsM, updateData );
        runSuccess = true;

    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                // ScmFactory.File.deleteInstance(wsM, fileId, true);
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void checkUpdateAndAsyncCacheFileResult( ScmWorkspace ws,
            byte[] updateData ) throws Exception {
        int currentVersion = 2;
        int historyVersion = 1;

        // asyncCache only one file :current version file or history version
        // file
        ScmFile file = ScmFactory.File.getInstance( ws, fileId, currentVersion,
                0 );
        int actSiteNum = file.getLocationList().size();
        SiteWrapper[] expSiteList1 = { rootSite };
        SiteWrapper[] expSiteList2 = { rootSite, branSite };
        if ( actSiteNum == expSiteList1.length ) {
            VersionUtils.checkSite( ws, fileId, currentVersion, expSiteList1 );
            VersionUtils.checkSite( ws, fileId, historyVersion, expSiteList2 );
        } else if ( actSiteNum == expSiteList2.length ) {
            VersionUtils.checkSite( ws, fileId, currentVersion, expSiteList2 );
            VersionUtils.checkSite( ws, fileId, historyVersion, expSiteList1 );
        } else {
            Assert.fail( "check Sitelist error!" );
        }

        // check the update result
        VersionUtils.CheckFileContentByStream( ws, fileName, currentVersion,
                updateData );
        VersionUtils.CheckFileContentByStream( ws, fileName, historyVersion,
                writeData );

        // check the breakpoint is not exist
        try {
            ScmFactory.BreakpointFile.getInstance( ws, fileName );
            Assert.fail( "get breakpoint file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-262  actError:" + e.getError()
                        + e.getMessage() );
            }
        }
    }

    private void createBreakPointFile( ScmWorkspace ws, byte[] updateData )
            throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        new Random().nextBytes( updateData );
        breakpointFile.upload( new ByteArrayInputStream( updateData ) );
    }

    public class UpdateFile extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                        .getInstance( ws, fileName );
                file.updateContent( breakpointFile );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    public class AsyncCacheFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                int sitenums = 2;
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                int currentVersion = file.getMajorVersion();
                ScmFactory.File.asyncCache( ws, fileId, currentVersion, 0 );
                VersionUtils.waitAsyncTaskFinished( ws, fileId, currentVersion,
                        sitenums );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}