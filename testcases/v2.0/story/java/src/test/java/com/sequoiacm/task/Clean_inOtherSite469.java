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
 * @FileName SCM-469: 分中心A和主中心存在缓存，在分中心B清理文件
 * @Author linsuqiang
 * @Date 2017-06-23
 * @Version 1.00
 */

/*
 * 1、在分中心B开始清理任务，清理条件匹配清理分中心A和主中心均存在但分中心B不存在的文件； 2、检查执行结果正确性；
 */

public class Clean_inOtherSite469 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final int fileNum = 100;
    private final String authorName = "CleanInOtherSite469";
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private File localPath = null;
    private String filePath = null;

    private ScmSession sessionM = null;
    private ScmSession sessionA = null;
    private ScmSession sessionB = null;

    private ScmId taskId = null;

    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSiteList = new ArrayList< SiteWrapper >();
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSiteList = ScmInfo.getBranchSites( 2 );
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionM = TestScmTools.createSession( rootSite );
            sessionA = TestScmTools.createSession( branceSiteList.get( 0 ) );
            sessionB = TestScmTools.createSession( branceSiteList.get( 1 ) );
            prepareFiles( sessionA );
        } catch ( Exception e ) {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        try {
            readAllFile( sessionM );
            taskId = cleanFile( sessionB );
            ScmTaskUtils.waitTaskFinish( sessionB, taskId );
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
                    ScmFactory.File.deleteInstance( ws, fileIdList.get( i ),
                            true );
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
            if ( sessionB != null ) {
                sessionB.close();
            }

        }
    }

    private void prepareFiles( ScmSession session ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
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
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            for ( ScmId fileId : fileIdList ) {
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
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
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        return ScmSystem.Task.startCleanTask( ws, condition );
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteList = { rootSite, branceSiteList.get( 0 ) };
            ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}