package com.sequoiacm.net.task.concurrent;

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
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-750 : 并发清理文件、单个异步缓存相同文件
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、并发清理文件、单个异步缓存相同文件； 2、检查执行结果正确性；
 */

public class CleanAndAsyncCache750 extends TestScmBase {
    private final int fileSize = 5 * 1024 * 1024;
    private final String author = "clean750";
    private boolean runSuccess = false;
    private ScmId fileId = null;
    private File localPath = null;
    private String filePath = null;
    private ScmId taskId = null;

    private ScmSession sessionM = null;
    private ScmSession sessionA = null;
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
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getCleanSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            sessionA = TestScmTools.createSession( sourceSite );
            sessionM = TestScmTools.createSession( targetSite );

            writeScmFile( sessionA );
            readScmFile( sessionM );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), sessionA );
            ScmFactory.File.asyncCache( ws, fileId );

            taskId = cleanScmFile( ws );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );

            checkMetaAndLob();
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
                ScmFactory.File.getInstance( ws, fileId ).delete( true );
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

    private void writeScmFile( ScmSession session ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( ws_T.getName(), session );
        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setFileName( author + UUID.randomUUID() );
        scmfile.setAuthor( author );
        scmfile.setContent( filePath );
        fileId = scmfile.save();
    }

    private void readScmFile( ScmSession session ) throws Exception {
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), session );
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            fos = new FileOutputStream( new File( downloadPath ) );
            sis = ScmFactory.File.createInputStream( scmfile );
            sis.read( fos );
        } finally {
            if ( fos != null ) {
                fos.close();
            }
            if ( sis != null ) {
                sis.close();
            }
        }
    }

    private ScmId cleanScmFile( ScmWorkspace ws ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
        return ScmSystem.Task.startCleanTask( ws, condition );
    }

    private void checkMetaAndLob() {
        try {
            SiteWrapper[] expSiteList = { targetSite };
            List< ScmId > fileIdList = new ArrayList< ScmId >();
            fileIdList.add( fileId );
            ScmFileUtils
                    .checkMetaAndData( ws_T, fileIdList, expSiteList, localPath,
                            filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}