package com.sequoiacm.net.task;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-461: listTask参数校验
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * ScmSystem.Task.listTask接口参数校验： 1）有效参数： session: 存在
 * matcher：BSONObject、存在的任务属性、不存在的任务属性 2）无效参数： session: null matcher：null
 * 2、检查校验结果
 */

public class Transfer_param_listTask461 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final int fileNum = 10;
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "ListTaskWithInvalidArgs461";

    private File localPath = null;
    private String filePath = null;

    private ScmSession sessionA = null;
    private ScmId taskId = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            sessionA = TestScmTools.createSession( sourceSite );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), sessionA );

            prepareFiles( sessionA );

            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            taskId = ScmSystem.Task
                    .startTransferTask( ws, condition, ScopeType.SCOPE_CURRENT,
                            targetSite.getSiteName() );

            System.out.println( "taskId  = " + taskId );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
        } catch ( Exception e ) {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void matchInexistentTaskAttr() throws Exception {
        try {
            System.out.println( "  taskId = " + taskId );
            BSONObject cond = ScmQueryBuilder.start( "hhhWhatsthis" )
                    .is( taskId.get() ).get();
            ScmCursor< ScmTaskBasicInfo > cursor = ScmSystem.Task
                    .listTask( sessionA, cond );
            if ( cursor.hasNext() ) {
                Assert.fail(
                        "list shouldn't have result when matcher has " +
                                "inexistent attribute" );
            }
            cursor.close();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void listWhenSessionNull() throws Exception {
        try {
            BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.Task.ID )
                    .is( taskId ).get();
            ScmSystem.Task.listTask( null, cond );
            Assert.fail( "list shouldn't succeed when session is null" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void listWhenCondNull() throws Exception {
        try {
            ScmSystem.Task.listTask( sessionA, null );
            Assert.fail( "list shouldn't succeed when condition is null" );
        } catch ( ScmException e ) {
            if ( ScmError.INVALID_ARGUMENT != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), sessionA );
                for ( int i = 0; i < fileNum; ++i ) {
                    ScmFactory.File
                            .deleteInstance( ws, fileIdList.get( i ), true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void prepareFiles( ScmSession session ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( ws_T.getName(), session );
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
        }
    }
}