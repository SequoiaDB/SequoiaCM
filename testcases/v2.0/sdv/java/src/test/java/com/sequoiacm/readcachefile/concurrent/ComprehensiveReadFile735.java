package com.sequoiacm.readcachefile.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-735 : 场景四（跨中心读文件）、场景五并发
 * @Author linsuqiang
 * @Date 2017-08-08
 * @Version 1.00
 */

/*
 * 1、场景四、场景五并发，不同文件； 场景四：文件在A中心，在B中心读取文件；
 * 场景五：文件在A中心，B中心读取文件后写入新的文件，分中心A再次读取所有文件； 2、检查场景并发后数据正确性；
 */

public class ComprehensiveReadFile735 extends TestScmBase {
    private final int branSitesNum = 2;
    private final int fileSize = 200 * 1024;
    private final String author = "file735";
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private SiteWrapper[] expSites = null;
    private WsWrapper wsp = null;
    private File localPath = null;
    private String filePath = null;
    private List< String > filePathList4 = new ArrayList< String >();
    private List< String > filePathList5 = new ArrayList< String >();
    private List< ScmId > fileIdList4 = new ArrayList< ScmId >();
    private List< ScmId > fileIdList5 = new ArrayList< ScmId >();

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

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { GroupTags.fourSite, GroupTags.star })
    private void testStar() throws Exception {
        expSites = new SiteWrapper[] { rootSite, branSites.get( 0 ),
                branSites.get( 1 ) };
        SceneThread4 sThd4 = new SceneThread4();
        SceneThread5 sThd5 = new SceneThread5();
        sThd4.start();
        sThd5.start();
        Assert.assertTrue( sThd4.isSuccess(), sThd4.getErrorMsg() );
        Assert.assertTrue( sThd5.isSuccess(), sThd5.getErrorMsg() );
        runSuccess = true;
    }

    @Test(groups = { GroupTags.fourSite, GroupTags.net })
    private void testNet() throws Exception {
        expSites = new SiteWrapper[] { branSites.get( 0 ), branSites.get( 1 ) };
        SceneThread4 sThd4 = new SceneThread4();
        SceneThread5 sThd5 = new SceneThread5();
        sThd4.start();
        sThd5.start();
        Assert.assertTrue( sThd4.isSuccess(), sThd4.getErrorMsg() );
        Assert.assertTrue( sThd5.isSuccess(), sThd5.getErrorMsg() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        ScmSession ss = null;
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ss = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), ss );
                for ( ScmId fileId : fileIdList4 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                for ( ScmId fileId : fileIdList5 ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != ss ) {
                ss.close();
            }
        }
    }

    private void writeFile( ScmSession ss, List< ScmId > fileIdList,
            List< String > filePathList, int writeFileNum )
            throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                ss );
        for ( int i = 0; i < writeFileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setAuthor( author );
            scmfile.setFileName( author + "_" + UUID.randomUUID() + i );
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
            filePathList.add( filePath );
        }
    }

    private void readFile( ScmSession ss, List< ScmId > fileIdList,
            List< String > filePathList ) throws ScmException {
        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    ss );

            for ( int i = 0; i < fileIdList.size(); ++i ) {
                ScmFile scmfile = ScmFactory.File.getInstance( ws,
                        fileIdList.get( i ) );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                scmfile.getContent( downloadPath );

                Assert.assertEquals( TestTools.getMD5( filePathList.get( i ) ),
                        TestTools.getMD5( downloadPath ) );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkMetaAndLob( List< ScmId > fileIdList,
            List< String > filePathList, SiteWrapper[] expSites )
            throws Exception {
        ScmFileUtils.checkMetaAndData( wsp, fileIdList, expSites, localPath,
                filePath );
    }

    /**
     * 场景四：文件在A中心，在B中心读取文件；
     */
    private class SceneThread4 extends TestThreadBase {
        private ScmSession ssA = null;
        private ScmSession ssB = null;

        @Override
        public void exec() throws Exception {
            try {
                ssA = TestScmTools.createSession( branSites.get( 0 ) );
                ssB = TestScmTools.createSession( branSites.get( 1 ) );
                writeFile( ssA, fileIdList4, filePathList4, 40 );
                readFile( ssB, fileIdList4, filePathList4 );
                checkMetaAndLob( fileIdList4, filePathList4, expSites );
            } finally {
                if ( null != ssA ) {
                    ssA.close();
                }
                if ( null != ssB ) {
                    ssB.close();
                }
            }
        }

    }

    /**
     * 场景五：文件在A中心，B中心读取文件后写入新的文件，分中心A再次读取所有文件；
     */
    private class SceneThread5 extends TestThreadBase {
        private ScmSession ssA = null;
        private ScmSession ssB = null;

        @Override
        public void exec() throws Exception {
            try {
                ssA = TestScmTools.createSession( branSites.get( 0 ) );
                ssB = TestScmTools.createSession( branSites.get( 1 ) );
                writeFile( ssA, fileIdList5, filePathList5, 20 );
                readFile( ssB, fileIdList5, filePathList5 );
                checkMetaAndLob( fileIdList5, filePathList5, expSites );
                writeFile( ssB, fileIdList5, filePathList5, 20 );
                readFile( ssA, fileIdList5, filePathList5 );
                checkMetaAndLob( fileIdList5, filePathList5, expSites );
            } finally {
                if ( null != ssA ) {
                    ssA.close();
                }
                if ( null != ssB ) {
                    ssB.close();
                }
            }
        }

    }

}