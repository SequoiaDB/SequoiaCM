package com.sequoiacm.scmfile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-285:同一个中心并发删除不同文件
 * @author huangxiaoni init
 * @date 2017.5.24
 */

public class DeleteScmFile285 extends TestScmBase {
    private static final int fileNum = 10;
    private static final int threadNum = 20;
    private boolean runSuccess = false;
    private List< SiteWrapper > sites = null;
    private SiteWrapper pubSite = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "delete285";
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

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

            sites = ScmInfo.getAllSites();
            wsp = ScmInfo.getWs();
            for ( int i = 0; i < fileNum * threadNum; i++ ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR )
                        .is( fileName + "_" + i ).get();
                ScmFileUtils.cleanFile( wsp, cond );
            }
            pubSite = sites.get( new Random().nextInt( sites.size() ) );
            session = ScmSessionUtils.createSession( pubSite );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

            this.writeFile();
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            List< DeleteFile > delList = new ArrayList<>();
            List< SiteWrapper > siteList = new ArrayList<>();
            for ( int i = 0; i < threadNum; i++ ) {
                SiteWrapper site = sites
                        .get( new Random().nextInt( sites.size() ) );
                DeleteFile deleteFile = new DeleteFile( site, i );
                deleteFile.start();

                delList.add( deleteFile );
                siteList.add( site );
            }

            for ( int j = 0; j < delList.size(); j++ ) {
                DeleteFile deleteFile = delList.get( j );
                SiteWrapper site = siteList.get( j );
                Assert.assertTrue( deleteFile.isSuccess(),
                        "siteId = " + site.getSiteId() + ", msg = "
                                + deleteFile.getErrorMsg() );
            }

            checkResults();
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
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void writeFile() throws ScmException {
        for ( int i = 0; i < fileNum * threadNum; i++ ) {
            ScmId fileId = ScmFileUtils.create( ws, fileName + "_" + i,
                    filePath );
            fileIdList.add( fileId );
        }
    }

    private void checkResults() throws Exception {
        // check meta
        BSONObject cond = new BasicBSONObject( "name", fileName );
        long cnt = ScmFactory.File.countInstance( ws, ScopeType.SCOPE_CURRENT,
                cond );
        Assert.assertEquals( cnt, 0 );

        // check data
        for ( ScmId fileId : fileIdList ) {
            try {
                ScmFileUtils.checkData( ws, fileId, localPath, filePath );
                Assert.assertFalse( true,
                        "File is unExisted, except throw e, but success." );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND,
                        e.getMessage() );
            }
        }
    }

    private class DeleteFile extends TestThreadBase {
        private SiteWrapper site;
        private int startNum;

        DeleteFile( SiteWrapper site, int startNum ) {
            this.site = site;
            this.startNum = startNum;
        }

        @Override
        public void exec() {
            ScmSession ss = null;
            try {
                ss = ScmSessionUtils.createSession( site );
                ScmWorkspace sws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), ss );

                for ( int i = fileNum * startNum; i < fileNum
                        * ( startNum + 1 ); i++ ) {
                    ScmFactory.File.getInstance( sws, fileIdList.get( i ) )
                            .delete( true );
                }
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( ss != null ) {
                    ss.close();
                }
            }
        }
    }
}
