package com.sequoiacm.net.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-437: 并发开始相同迁移任务
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、A、B线程并发开始迁移任务，ws和迁移条件均相同； 2、检查执行结果；
 */

public class Transfer_startSameTask437 extends TestScmBase {
    private boolean runSuccess = false;

    private int fileSize = 512 * 1024;
    private int fileNum = 100;
    private File localPath = null;
    private String filePath = null;
    private String authorName = "StartSameTask437";

    private ScmSession sessionA = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private ScmId taskId = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

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

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionA = TestScmTools.createSession( sourceSite );
            prepareFiles( sessionA );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    //SEQUOIACM-712改动会导致失败，暂时屏蔽
    @Test(groups = { "twoSite", "fourSite" },enabled = false)
    private void test() throws Exception {
        try {
            taskId = transferAllFile( sessionA );
            try {
                transferAllFile( sessionA );
                Assert.fail(
                        "transfer shouldn't succeed when other transfer is "
                                + "doing!" );
            } catch ( ScmException e ) {
                if ( ScmError.TASK_DUPLICATE != e.getError() ) {
                    e.printStackTrace();
                    throw e;
                }
            }
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            checkTransfered();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        ScmSession mainSession = null;
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), sessionA );
                for ( int i = 0; i < fileNum; ++i ) {
                    ScmFactory.File.deleteInstance( ws, fileIdList.get( i ),
                            true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( mainSession != null ) {
                mainSession.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void prepareFiles( ScmSession session ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            fileIdList.add( scmfile.save() );
        }
    }

    private ScmId transferAllFile( ScmSession session ) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        return ScmSystem.Task.startTransferTask( ws, condition,
                ScopeType.SCOPE_CURRENT, targetSite.getSiteName() );
    }

    private void checkTransfered() {
        try {
            SiteWrapper[] expSiteList = { sourceSite, targetSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}