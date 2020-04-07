package com.sequoiacm.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
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
 * @FileName SCM-759 : 异步迁移到主中心并读取文件，清理分中心文件
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、在分中心A异步迁移单个文件； 2、在主中心读取该文件； 3、在分中心清理该文件； 4、检查执行结果正确性；
 */

public class AsyncTransThenReadThenClean759 extends TestScmBase {
    private final String author = "asyncTransfer759";
    private final int fileSize = 4 * 1024 * 1024;
    private boolean runSuccess = false;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmId taskId = null;
    private ScmId fileId = null;
    private String filePath = null;
    private File localPath = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
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

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            String random = author + "_" + UUID.randomUUID();
            fileId = ScmFileUtils.create( wsA, random, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            // asyncTransfer
            ScmFactory.File.asyncTransfer( wsA, fileId );
            int expSiteNum = 2;
            ScmTaskUtils.waitAsyncTaskFinished( wsA, fileId, expSiteNum );
            // check results
            SiteWrapper[] expSiteIdList1 = { rootSite, branceSite };
            ScmFileUtils
                    .checkMetaAndData( ws_T, fileId, expSiteIdList1, localPath,
                            filePath );

            readFileFromM();
            // check results
            SiteWrapper[] expSiteIdList2 = { rootSite, branceSite };
            ScmFileUtils
                    .checkMetaAndData( ws_T, fileId, expSiteIdList2, localPath,
                            filePath );

            // startCleanTasl
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                    .get();
            taskId = ScmSystem.Task.startCleanTask( wsA, condition );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            // check results
            SiteWrapper[] expSiteIdList3 = { rootSite };
            ScmFileUtils
                    .checkMetaAndData( ws_T, fileId, expSiteIdList3, localPath,
                            filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                TestSdbTools.Task.deleteMeta( taskId );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void readFileFromM() throws Exception {
        ScmSession ss = null;
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            // login
            ss = TestScmTools.createSession( rootSite );
            ScmWorkspace wks = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), ss );

            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( wks, fileId );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            fos = new FileOutputStream( new File( downloadPath ) );
            sis = ScmFactory.File.createInputStream( scmfile );
            sis.read( fos );

            // check content
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
            if ( ss != null )
                ss.close();
        }
    }
}