package com.sequoiacm.task;

import java.io.File;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-410: 主中心开始迁移任务
 * @Author fanyu
 * @Date 2017-06-17
 * @Version 1.00
 */

/*
 * 1、在主中心开始迁移任务； 2、检查执行结果；
 */
public class Transfer_inRootSite410 extends TestScmBase {

    private final int FILE_SIZE = new Random().nextInt( 1024 ) + 1;
    private boolean runSuccess = false;
    private File localPath = null;
    private String filePath = null;
    private String authorName = "TransferInMainCenter410";
    private BSONObject cond = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private ScmId taskId = null;

    private SiteWrapper rootSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + FILE_SIZE
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, FILE_SIZE );

            rootSite = ScmInfo.getRootSite();
            ws_T = ScmInfo.getWs();

            session = TestScmTools.createSession( rootSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );

            cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( authorName ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            fileId = createFile( ws, filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws ScmException {
        startTask();
        checkResult();
        runSuccess = true;
    }

    private ScmId createFile( ScmWorkspace ws, String filePath )
            throws ScmException {
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setContent( filePath );
        scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
        scmfile.setAuthor( authorName );
        fileId = scmfile.save();
        return fileId;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void startTask() throws ScmException {
        try {
            taskId = ScmSystem.Task.startTransferTask( ws, cond,
                    ScmType.ScopeType.SCOPE_CURRENT, rootSite.getSiteName() );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( ScmError.OPERATION_UNSUPPORTED != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteList = { rootSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + " fileId = " + fileId.get()
                    + " rootSite INFO " + rootSite.toString() );
        }
    }
}
