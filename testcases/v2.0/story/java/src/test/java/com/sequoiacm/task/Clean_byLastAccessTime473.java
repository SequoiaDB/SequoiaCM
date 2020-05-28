package com.sequoiacm.task;

import java.io.File;
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
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
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
 * @Testcase: SCM-473:按最近访问时间清理文件
 * @author huangxiaoni init
 * @date 2017.6.26
 */

public class Clean_byLastAccessTime473 extends TestScmBase {
    private boolean runSuccess = false;

    private ScmSession sessionM = null; // mainCenter
    private ScmSession sessionA = null; // subCenterA
    private ScmWorkspace wsA = null;
    private ScmId taskId = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private String authorName = "clean473";
    private int fileSize = 100;
    private int fileNum = 10;
    private int startNum = 2;
    private List< Object > lastAccessTimeList = new ArrayList<>();

    private File localPath = null;
    private String filePath = null;

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            // login
            sessionA = TestScmTools.createSession( branceSite );
            sessionM = TestScmTools.createSession( rootSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
            // ready scm file
            writeFileFromA();
            readFileFromM( sessionM );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testTransfer() {
        try {
            startCleanTaskFromA( sessionA );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            ScmTask taskInfo = ScmSystem.Task.getTask( sessionA, taskId );
            Assert.assertEquals( taskInfo.getProgress(), 100 );
            checkMetaAndLobs();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsA, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( ScmException e ) {
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

    private void startCleanTaskFromA( ScmSession ss ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                ss );

        BSONObject obj = ScmQueryBuilder.start( ScmAttributeName.File.SITE_ID )
                .is( branceSite.getSiteId() )
                .and( ScmAttributeName.File.LAST_ACCESS_TIME )
                .greaterThanEquals( lastAccessTimeList.get( startNum ) ).get();
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).put( ScmAttributeName.File.SITE_LIST )
                .elemMatch( obj ).get();

        taskId = ScmSystem.Task.startCleanTask( ws, cond );
    }

    private void writeFileFromA() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setContent( filePath );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void readFileFromM( ScmSession ss ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                ss );
        for ( int i = 0; i < fileNum; i++ ) {
            // read file
            ScmId fileId = fileIdList.get( i );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            file.getContent( downloadPath );

            // get lastAccessTime
            file = ScmFactory.File.getInstance( ws, fileId );
            for ( int j = 0; j < file.getLocationList().size(); j++ ) {
                if ( file.getLocationList().get( j ).getSiteId() == branceSite
                        .getSiteId() ) {
                    long lastAccessTime = file.getLocationList().get( j )
                            .getDate().getTime();
                    lastAccessTimeList.add( lastAccessTime );
                }
            }
        }
    }

    private void checkMetaAndLobs() {
        try {
            SiteWrapper[] expSiteList1 = { rootSite };
            ScmFileUtils.checkMetaAndData( ws_T,
                    fileIdList.subList( startNum, fileNum ), expSiteList1,
                    localPath, filePath );

            SiteWrapper[] expSiteList2 = { rootSite, branceSite };
            ScmFileUtils.checkMetaAndData( ws_T,
                    fileIdList.subList( 0, startNum ), expSiteList2, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}