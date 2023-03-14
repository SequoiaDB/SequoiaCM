package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase:SCM234 所有中心均存在文件，分中心B读取该文件
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6 linsuqiang
 *         modify in 2017.8.7
 */

/*
 * 1、分中心A写入文件； 2、分中心B读取文件； 3、检查主中心文件元数据信息； 4、检查所有分中心sdb是否都存在该lob文件；
 * 5、分别连接主中心、分中心A的sdb，删除该lob文件； 6、分中心B再次读取文件； 7、检查读取的文件正确性；再次检查主中心该文件元数据正确性；
 * 8、检查元数据所有站点列表最近访问时间正确性；
 */

public class AcrossCenterReadFileByStream234 extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;

    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;

    private String fileName = "readCacheFile234";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 2;
    private List< ScmFileLocation > preSiteInfoList = null;
    private List< ScmFileLocation > aftSiteInfoList = null;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException {
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
                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );

            sessionM = ScmSessionUtils.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

            sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            sessionB = ScmSessionUtils.createSession( branSites.get( 1 ) );
            wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite", "star", GroupTags.base }) // bug:315
    private void test() throws Exception {
        ScmFile file;
        fileId = ScmFileUtils.create( wsA, fileName, filePath );

        this.readFileFromB( wsB );
        TestSdbTools.Lob.removeLob( rootSite, wsp, fileId );
        TestSdbTools.Lob.removeLob( branSites.get( 0 ), wsp, fileId );

        // get lastAccessTime before read
        file = ScmFactory.File.getInstance( wsM, fileId );
        preSiteInfoList = file.getLocationList();

        this.readFileFromB( wsB );

        // get lastAccessTime after read
        file = ScmFactory.File.getInstance( wsM, fileId );
        aftSiteInfoList = file.getLocationList();

        // check results
        this.checkMetaAndData();
        this.checkLastAccessTime();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.getInstance( wsM, fileId ).delete( true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null )
                sessionM.close();
            if ( sessionA != null )
                sessionA.close();
            if ( sessionB != null )
                sessionB.close();

        }
    }

    private void readFileFromB( ScmWorkspace ws ) throws Exception {
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            sis = ScmFactory.File.createInputStream( file );
            fos = new FileOutputStream( new File( downloadPath ) );
            sis.read( fos );

            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
        }
    }

    private void checkMetaAndData() throws Exception {
        // check meta
        SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                branSites.get( 1 ) };
        ScmFileUtils.checkMeta( wsM, fileId, expSites );

        // check data
        ScmFileUtils.checkData( wsB, fileId, localPath, filePath );
        ScmWorkspace[] notExistLobSites = { wsA, wsM };
        for ( int i = 0; i < notExistLobSites.length; i++ ) {
            try {
                ScmFileUtils.checkData( notExistLobSites[ i ], fileId,
                        localPath, filePath );
                Assert.fail(
                        "Lob has be removed, expect fail, actual success." );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.DATA_NOT_EXIST
                        && e.getError() != ScmError.DATA_ERROR ) { // TODO:jira-158,expCode:-401,
                    // not
                    // -801
                    e.printStackTrace();
                    Assert.fail( e.getMessage() );
                }
            }
        }
    }

    private void checkLastAccessTime() throws Exception {
        List< Long > preTimes = new ArrayList<>();
        for ( int i = 0; i < preSiteInfoList.size(); i++ ) {
            preTimes.add( preSiteInfoList.get( i ).getDate().getTime() );
        }

        List< Long > aftTimes = new ArrayList<>();
        for ( int i = 0; i < aftSiteInfoList.size(); i++ ) {
            preTimes.add( aftSiteInfoList.get( i ).getDate().getTime() );
        }

        for ( int i = 0; i < aftTimes.size(); i++ ) {
            if ( preTimes.get( i ) != aftTimes.get( i ) ) {
                Assert.fail( "failed to check lastAccessTime, "
                        + "preSiteInfoList: " + preSiteInfoList.toString()
                        + "\naftSiteInfoList: " + aftSiteInfoList.toString() );
            }
        }
    }
}
