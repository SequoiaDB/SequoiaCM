package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.util.List;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmContentLocation;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @description SCM-4135:迁移文件时，使用ScmFile.getContentLocaltions()接口获取文件数据源信息
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class FileContentLocaltions4135 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private WsWrapper wsp;
    private boolean isTransferFileThreadSuccess = false;
    private String fileName = "file_4135";
    private ScmWorkspace ws;
    private ScmId fileId = null;
    private ScmId taskID = null;
    private BSONObject queryCond;

    @BeforeClass
    private void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        site = ScmInfo.getBranchSite();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();

        session = TestScmTools.createSession( rootSite );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        fileId = ScmFileUtils.create( ws, fileName, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadGetContentLocations() );
        es.addWorker( new ThreadTransferFile() );
        es.run();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskID );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class ThreadGetContentLocations extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession(rootSite);
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace(wsp.getName(), session);
                ScmFile file = ScmFactory.File.getInstance(ws, fileId);
                List<ScmContentLocation> fileContentLocationsInfo = file
                        .getContentLocations();
                if (isTransferFileThreadSuccess) {
                    fileContentLocationsInfo = file.getContentLocations();
                    List<ScmContentLocation> subLocationsInfo = fileContentLocationsInfo.subList(1, 2);
                    ScmFileUtils.checkContentLocation(subLocationsInfo,
                            site, fileId, ws);
                }
                ScmFileUtils.checkContentLocation(fileContentLocationsInfo,
                        rootSite, fileId, ws);
            }finally {
                session.close();
            }
        }
    }

    private class ThreadTransferFile extends ResultStore {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            taskID = ScmSystem.Task.startTransferTask( ws, queryCond,
                    ScmType.ScopeType.SCOPE_CURRENT, site.getSiteName() );
            ScmTaskUtils.waitTaskFinish( session, taskID );
            isTransferFileThreadSuccess = true;
        }
    }
}