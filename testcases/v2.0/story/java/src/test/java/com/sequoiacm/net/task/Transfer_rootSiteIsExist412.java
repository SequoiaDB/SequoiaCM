package com.sequoiacm.net.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
 * @FileName SCM-412: 迁移在主中心已存在的文件
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、在分中心A写入多个文件； 2、在主中心读取其中某一个文件； 3、在分中心A迁移该部分文件（多个）；
 * 4、检查执行结果、主中心、分中心A文件内容及元数据正确性；
 */

public class Transfer_rootSiteIsExist412 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final int fileNum = 100;
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "TransferExistFile412 ";
    private File localPath = null;
    private String filePath = null;

    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
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
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            sessionM = TestScmTools.createSession( targetSite );
            wsM = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionM );
            sessionA = TestScmTools.createSession( sourceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            prepareFiles( wsA );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            int readBegin = fileNum / 4;
            int readEnd = fileNum / 2;
            readPartFile( wsM, readBegin, readEnd );

            int transferBegin = fileNum / 4;
            int transferEnd = fileNum;
            taskId = transferPartFile( wsA, transferBegin, transferEnd );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            checkTransfered( transferBegin, transferEnd );
            checkNotTransfered( 0, transferBegin );
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

                for ( int i = 0; i < fileNum; ++i ) {
                    ScmFactory.File.deleteInstance( wsM, fileIdList.get( i ),
                            true );
                    ;
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
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

    private void prepareFiles( ScmWorkspace ws ) throws Exception {
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            scmfile.setTitle( String.format( "%03d", i ) );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
        }
    }

    private void readPartFile( ScmWorkspace ws, int readBegin, int readEnd )
            throws Exception {
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            for ( int i = readBegin; i < readEnd; ++i ) {
                ScmId fileId = fileIdList.get( i );
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis = ScmFactory.File.createInputStream( scmfile );
                sis.read( fos );
            }
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
        }
    }

    private ScmId transferPartFile( ScmWorkspace ws, int transferBegin,
            int transferEnd ) throws ScmException {
        String title = ScmAttributeName.File.TITLE;
        String begin = String.format( "%03d", transferBegin );
        String end = String.format( "%03d", transferEnd );
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                .and( title ).greaterThan( begin ).and( title ).lessThan( end )
                .get();
        return ScmSystem.Task.startTransferTask( ws, condition,
                ScopeType.SCOPE_CURRENT, targetSite.getSiteName() );
    }

    private void checkTransfered( int checkBegin, int checkEnd )
            throws IOException, ScmException {
        ScmSession session = null;
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            // login
            session = TestScmTools.createSession( targetSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );

            // read content
            for ( int i = checkBegin; i < checkEnd; ++i ) {
                ScmId fileId = fileIdList.get( i );
                ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis = ScmFactory.File.createInputStream( scmfile );
                sis.read( fos );

                // check content on main center
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            }
            SiteWrapper[] expSiteList = { sourceSite, targetSite };
            ScmFileUtils.checkMetaAndData( ws_T,
                    fileIdList.subList( checkBegin, checkEnd ), expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
            if ( session != null )
                session.close();
        }
    }

    private void checkNotTransfered( int checkBegin, int checkEnd ) {
        try {
            SiteWrapper[] expSiteList = { sourceSite };
            ScmFileUtils.checkMetaAndData( ws_T,
                    fileIdList.subList( checkBegin, checkEnd ), expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}