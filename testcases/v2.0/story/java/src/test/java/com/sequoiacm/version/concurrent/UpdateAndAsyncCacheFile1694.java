package com.sequoiacm.version.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @description SCM-1694:并发更新和异步缓存相同文件
 * @author wuyan
 * @createDate 2018.06.15
 * @updateUser ZhangYanan
 * @updateDate 2021.12.06
 * @updateRemark
 * @version v1.0
 */
public class UpdateAndAsyncCacheFile1694 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;
    private String fileName = "versionfile1694";
    private String authorName = "author1694";
    private byte[] writeData = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = ScmFileUtils.createFileByStream( wsM, fileName, writeData,
                authorName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int updateSize = 1024 * 180;
        byte[] updateData = new byte[ updateSize ];
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new AsyncCacheFileThread() );
        es.addWorker( new UpdateFileThread( updateData ) );
        es.run();
        checkUpdateAndAsyncCacheFileResult( wsM, updateData );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
            }
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
    }

    private class AsyncCacheFileThread extends ResultStore {

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( branSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                int sitenums = 2;
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                int currentVersion = file.getMajorVersion();
                ScmFactory.File.asyncCache( ws, fileId, currentVersion, 0 );
                ScmTaskUtils.waitAsyncTaskFinished( ws, fileId, currentVersion,
                        sitenums );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class UpdateFileThread extends ResultStore {
        private byte[] updateData;

        public UpdateFileThread( byte[] updateData ) {
            this.updateData = updateData;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                VersionUtils.updateContentByStream( ws, fileId, updateData );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}