package com.sequoiacm.task;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTaskBasicInfo;
import com.sequoiacm.client.exception.ScmException;
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
 * @FileName SCM-425: 重复close
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、获取任务列表游标，任务列表至少包含1条记录； 2、重复多次close游标； 3、检查执行结果正确性；
 */

public class Transfer_repeatCloseCursor425 extends TestScmBase {
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private int FILE_SIZE = new Random().nextInt( 1024 ) + 1024;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId taskId = null;

    private String authorName = "CreateMultiTasks409";
    private ScmId fileId = null;
    private BSONObject cond = null;

    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + FILE_SIZE
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, FILE_SIZE );

        branceSite = ScmInfo.getBranchSite();
        ws_T = ScmInfo.getWs();

        session = TestScmTools.createSession( branceSite );
        ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );

        cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( ws_T, cond );

        createFile( ws, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        taskId = ScmSystem.Task.startTransferTask( ws, cond,
                ScmType.ScopeType.SCOPE_CURRENT,
                ScmInfo.getRootSite().getSiteName() );
        ScmTaskUtils.waitTaskFinish( session, taskId );
        checkRepeatCloseCursor();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void createFile( ScmWorkspace ws, String filePath )
            throws ScmException {
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setContent( filePath );
        scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( authorName );
        fileId = scmfile.save();
    }

    private void checkRepeatCloseCursor() throws ScmException {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.Task.WORKSPACE ).lessThanEquals( "ws" )
                .get();
        ScmCursor< ScmTaskBasicInfo > cursor = ScmSystem.Task.listTask( session,
                cond );
        if ( cursor != null ) {
            for ( int i = 0; i < 3; i++ ) {
                cursor.close();
            }
        }
    }
}
