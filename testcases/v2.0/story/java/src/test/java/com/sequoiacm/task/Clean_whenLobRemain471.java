package com.sequoiacm.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-471: 主中心和分中心均存在缓存，但是大小不一致
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 前提： 1、在分中心A写入多个文件； 2、模拟主中心某一个文件残留，即该文件元数据不存在主中心站点信息，
 * 但是LOB被残留在主中心，且LOB大小跟分中心A不一致； （模拟方法：直连主中心SDB写入一个不同大小的LOB，写入时指定LOB的Oid跟分中心A一致）
 * 步骤： 1、在分中心A开始清理任务，清理条件匹配清理多个文件， 其中包括主中心和分中心均存在缓存但是大小不一致的文件； 2、检查执行结果；
 */

public class Clean_whenLobRemain471 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final int fileNum = 5;
    private final String authorName = "CleanFileWhenLobRemain471";
    private final int randomIdx = 4; // lob of this index is remained
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private File localPath = null;
    private String filePath = null;
    private String remainedFilePath = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmId taskId = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        remainedFilePath =
                localPath + File.separator + "localFile_" + fileSize / 2 +
                        ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            TestTools.LocalFile.createFile( remainedFilePath, fileSize / 2 );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            prepareFiles();
        } catch ( Exception e ) {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            readAllFile( sessionM );

            ScmId randomId = fileIdList.get( randomIdx );
            TestSdbTools.Lob.removeLob( rootSite, ws_T, randomId );
            TestSdbTools.Lob
                    .putLob( rootSite, ws_T, randomId, remainedFilePath );

            taskId = cleanFile( sessionA );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            checkResult();
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
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), sessionM );
                for ( int i = 0; i < fileNum; ++i ) {
                    ScmFactory.File.getInstance( ws, fileIdList.get( i ) )
                            .delete( true );
                }
                TestSdbTools.Task.deleteMeta( taskId );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void prepareFiles() throws Exception {
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( wsA );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
        }
    }

    private void readAllFile( ScmSession session ) throws Exception {
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), session );
            for ( ScmId fileId : fileIdList ) {
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile
                        .initDownloadPath( localPath, TestTools.getMethodName(),
                                Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis = ScmFactory.File.createInputStream( scmfile );
                sis.read( fos );
            }
        } finally {
            if ( fos != null ) {
                fos.close();
            }
            if ( sis != null ) {
                sis.close();
            }
        }
    }

    private ScmId cleanFile( ScmSession session ) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( ws_T.getName(), session );
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        return ScmSystem.Task.startCleanTask( ws, condition );
    }

    private void checkResult() {
        try {
            // check file should be cleaned
            ScmId fileId = fileIdList.get( randomIdx );
            SiteWrapper[] expSiteList1 = { rootSite, branceSite };
            ScmFileUtils.checkMeta( wsM, fileId, expSiteList1 );
            ScmFileUtils.checkData( wsM, fileId, localPath, remainedFilePath );
            ScmFileUtils.checkData( wsA, fileId, localPath, filePath );

            SiteWrapper[] expSiteList2 = { rootSite };
            ScmFileUtils.checkMetaAndData( ws_T,
                    fileIdList.subList( 0, fileNum - 1 ), expSiteList2,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}