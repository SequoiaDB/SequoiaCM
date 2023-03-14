package com.sequoiacm.readcachefile.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmSiteCacheStrategy;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5283:修改工作区缓存配置与跨站点读并发
 * @Author YiPan
 * @CreateDate
 * @UpdateUser
 * @UpdateDate 2022/9/19
 * @UpdateRemark
 * @Version
 */
public class AcrossCenterReadFile5283 extends TestScmBase {
    private final String wsName = "ws_5283";
    private final String fileNameBase = "file5283_";
    private final String fileAuthor = "author5283";
    private List< ScmId > fileIds = new ArrayList<>();
    private ScmSession sessionM;
    private ScmWorkspace wsM;
    private SiteWrapper branchSite;
    private BSONObject cond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private final int fileNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        sessionM = ScmSessionUtils.createSession( ScmInfo.getRootSite() );
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        wsM = ScmWorkspaceUtil.createWS( sessionM, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsName );
        wsM.updateSiteCacheStrategy( ScmSiteCacheStrategy.NEVER );

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        branchSite = ScmInfo.getBranchSite();
        cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsName, cond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor t = new ThreadExecutor();
        t.addWorker( new ReadFile() );
        t.addWorker( new UpdateWs( ScmSiteCacheStrategy.ALWAYS ) );
        t.addWorker( new UpdateWs( ScmSiteCacheStrategy.NEVER ) );
        t.run();
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsName, cond );
                ScmWorkspaceUtil.deleteWs( wsName, sessionM );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                sessionM.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws ) throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            fileIds.add( file.save() );
        }
    }

    private class ReadFile {

        @ExecuteOrder(step = 1)
        private void run() throws ScmException, IOException {
            ScmSession session = ScmSessionUtils.createSession( branchSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                    session );
            try {
                for ( int i = 0; i < fileIds.size(); i++ ) {
                    String downloadPath = localPath + File.separator
                            + fileNameBase + i + "single.txt";
                    ScmFile file = ScmFactory.File.getInstance( ws,
                            fileIds.get( i ) );
                    file.getContent( downloadPath );
                    Assert.assertEquals( TestTools.getMD5( downloadPath ),
                            TestTools.getMD5( filePath ) );
                }
            } finally {
                session.close();
            }
        }
    }

    private class UpdateWs {
        private ScmSiteCacheStrategy siteCacheStrategy;

        public UpdateWs( ScmSiteCacheStrategy siteCacheStrategy ) {
            this.siteCacheStrategy = siteCacheStrategy;
        }

        @ExecuteOrder(step = 1)
        private void run() throws ScmException {
            ScmSession session = ScmSessionUtils.createSession( branchSite );
            try {
                ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName,
                        session );
                ws.updateSiteCacheStrategy( siteCacheStrategy );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.WORKSPACE_CACHE_EXPIRE ) {
                    throw e;
                }
            } finally {
                session.close();
            }
        }
    }
}