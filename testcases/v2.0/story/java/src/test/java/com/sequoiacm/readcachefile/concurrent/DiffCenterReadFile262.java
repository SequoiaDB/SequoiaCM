package com.sequoiacm.readcachefile.concurrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase:SCM-262 文件在主中心，多个分中心并发读取文件（A/B网络不通） 1、在主中心写文件； 2、多个分中心并发读取文件；
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6
 */

public class DiffCenterReadFile262 extends TestScmBase {
    private static ScmSession sessionM = null;
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmWorkspace wsM = null;

    private String fileName = "readcachefile262";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 3;
    private File localPath = null;
    private String filePath = null;
    private int times = 0;
    // readThreads涉及以下读取文件时，每个站点分配多少个读取线程同时读取。
    // 如果测试机器性能不理想，建议将这个值适当改小，保证大于3即可。
    private final int readThreads = 5;

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

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            sessionM = ScmSessionUtils.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

            fileId = ScmFileUtils.create( wsM, fileName, filePath );
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            ReadFileFromSubCenterA readFromA = new ReadFileFromSubCenterA();
            readFromA.start( readThreads );

            ReadFileFromSubCenterB readFromB = new ReadFileFromSubCenterB();
            readFromB.start( readThreads );

            if ( !( readFromA.isSuccess() && readFromB.isSuccess() ) ) {
                Assert.fail(
                        readFromA.getErrorMsg() + readFromB.getErrorMsg() );
            }

            checkMetadataAndLobs();

        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void checkMetadataAndLobs() throws Exception {
        List< ScmId > fileIdList = new ArrayList< ScmId >();
        fileIdList.add( fileId );
        // check meta data
        SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                branSites.get( 1 ) };
        ScmFileUtils.checkMetaAndData( wsp, fileIdList, expSites, localPath,
                filePath );
    }

    private class ReadFileFromSubCenterA extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            OutputStream fos = null;
            ScmInputStream sis = null;
            try {
                // login
                session = ScmSessionUtils.createSession( branSites.get( 0 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                times = times++;
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                sis = ScmFactory.File.createInputStream( file );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis.read( fos );

                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } finally {
                if ( fos != null )
                    fos.close();
                if ( sis != null )
                    sis.close();
                if ( session != null )
                    session.close();
            }
        }
    }

    private class ReadFileFromSubCenterB extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            OutputStream fos = null;
            ScmInputStream sis = null;
            try {
                // login
                session = ScmSessionUtils.createSession( branSites.get( 1 ) );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                // read
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                times = times++;
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                sis = ScmFactory.File.createInputStream( file );
                fos = new FileOutputStream( new File( downloadPath ) );
                sis.read( fos );

                // check content
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } finally {
                if ( fos != null )
                    fos.close();
                if ( sis != null )
                    sis.close();
                if ( session != null )
                    session.close();
            }
        }
    }
}
