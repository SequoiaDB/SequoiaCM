package com.sequoiacm.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Testcase: SCM-413:迁移在主中心被残留的文件,大小一致
 * @author huangxiaoni init
 * @date 2017.6.14
 */

public class Transfer_rootSiteRemainLob413 extends TestScmBase {
    private boolean runSuccess = false;

    private ScmSession sessionM = null; // mainCenter
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null; // subCenterA
    private ScmId taskId = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "transfer413";
    private int fileSize = 100;
    private int fileNum = 10;
    private int startNum = 0;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        for ( int i = 0; i < fileNum; i++ ) {
            String filePath = localPath + File.separator + "localFile_"
                    + fileSize + i + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize + i );
            filePathList.add( filePath );
        }

        rootSite = ScmInfo.getRootSite();
        branceSite = ScmInfo.getBranchSite();
        ws_T = ScmInfo.getWs();

        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( ws_T, cond );

        // login
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
        sessionA = TestScmTools.createSession( branceSite );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testTransfer() throws Exception {
        writeFileFromSubCenterA( sessionA );
        // lob remain in mainSite
        ScmId fileId = fileIdList.get( startNum );
        TestSdbTools.Lob.putLob( rootSite, ws_T, fileId,
                filePathList.get( startNum ) );
        startTaskFromSubCenterA( sessionA );
        ScmTaskUtils.waitTaskFinish( sessionA, taskId );
        // check results
        checkTaskAtt( sessionA );
        checkMetaAndLobs();
        readFileFromMainCenter( sessionM );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.getInstance( wsM, fileId ).delete( true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void writeFileFromSubCenterA( ScmSession ss ) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                ss );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePathList.get( i ) );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void startTaskFromSubCenterA( ScmSession ss )
            throws ScmException, InterruptedException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                ss );
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                .put( ScmAttributeName.File.SIZE )
                .greaterThanEquals( fileNum + startNum ).get();
        taskId = ScmSystem.Task.startTransferTask( ws, condition );
    }

    private void checkTaskAtt( ScmSession session ) throws ScmException {
        ScmTask taskInfo = ScmSystem.Task.getTask( session, taskId );
        Assert.assertEquals( taskInfo.getProgress(), 100 );
        Assert.assertEquals( taskInfo.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
        Assert.assertEquals( taskInfo.getType(),
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
        Assert.assertEquals( taskInfo.getWorkspaceName(), ws_T.getName() );
        Assert.assertNotNull( taskInfo.getStopTime() );
    }

    private void checkMetaAndLobs() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            String filePath = filePathList.get( i );
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
        }
    }

    private void readFileFromMainCenter( ScmSession ss ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                ss );
        for ( int i = startNum; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            String filePath = filePathList.get( i );
            TestSdbTools.Lob.removeLob( branceSite, ws_T, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            file.getContent( downloadPath );
            // check content
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        }
    }
}