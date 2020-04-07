package com.sequoiacm.net.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
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
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.ScmFileLocation;
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

/**
 * @Testcase: SCM-465:清理分中心部分文件
 * @author huangxiaoni init linsuqiang modify
 * @date 2017.8.8
 */

/*
 * 1、在分中心A开始清理任务，清理条件匹配清理分中心部分文件； 2、任务执行完成后检查清理文件结果正确性；
 * 3、在本中心A读取被清理和未被清理的文件，检查执行结果正确性；
 */

public class Clean_partFile465 extends TestScmBase {
    private boolean runSuccess = false;

    private ScmSession sessionM = null; // mainCenter
    private ScmSession sessionA = null; // subCenterA
    private ScmWorkspace wsA = null;
    private ScmId taskId = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private String authorName = "clean465";
    private int fileSize = 100;
    private int fileNum = 10;
    private int startNum = 2;
    private File localPath = null;
    private List< String > filePathList = new ArrayList< String >();

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            for ( int i = 0; i < fileNum; i++ ) {
                String filePath =
                        localPath + File.separator + "localFile_" + fileSize +
                                i + ".txt";
                TestTools.LocalFile.createFile( filePath, fileSize + i );
                filePathList.add( filePath );
            }

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getCleanSites( ws_T );
            rootSite = siteList.get( 1 );
            branceSite = siteList.get( 0 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );
            // login
            sessionM = TestScmTools.createSession( rootSite );
            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            // ready scm file
            writeFileFromA();
            readFileFromM( sessionM );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            List< List< ScmFileLocation > > befLocLists = getLocationLists(
                    fileIdList );
            startCleanTaskFromA( sessionA );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            List< List< ScmFileLocation > > aftLocLists = getLocationLists(
                    fileIdList );
            checkLocationLists( befLocLists, aftLocLists );
            checkTaskAtt( sessionA );
            this.checkResults();
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
                    ScmFactory.File.getInstance( wsA, fileId ).delete( true );
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
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( ws_T.getName(), ss );
        int value = fileSize + startNum;
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                .put( ScmAttributeName.File.SIZE ).greaterThanEquals( value )
                .get();
        taskId = ScmSystem.Task.startCleanTask( ws, condition );
    }

    private void writeFileFromA() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setContent( filePathList.get( i ) );
            file.setFileName( authorName + "_" + UUID.randomUUID() );
            file.setAuthor( authorName );
            ScmId fileId = file.save();
            fileIdList.add( fileId );
        }
    }

    private void readFileFromM( ScmSession ss ) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( ws_T.getName(), ss );
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            file.getContent( downloadPath );
        }
    }

    private void checkTaskAtt( ScmSession session ) throws ScmException {
        ScmTask taskInfo = ScmSystem.Task.getTask( session, taskId );
        Assert.assertEquals( taskInfo.getProgress(), 100 );
        Assert.assertEquals( taskInfo.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
        Assert.assertEquals( taskInfo.getType(),
                CommonDefine.TaskType.SCM_TASK_CLEAN_FILE );
        Assert.assertEquals( taskInfo.getWorkspaceName(), ws_T.getName() );
        Assert.assertNotNull( taskInfo.getId() );
        Assert.assertNotNull( taskInfo.getStartTime() );
        Assert.assertNotNull( taskInfo.getStopTime() );
    }

    private void checkResults() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = fileIdList.get( i );
            String filePath = filePathList.get( i );
            if ( i >= startNum ) {
                SiteWrapper[] expSiteList = { rootSite };
                ScmFileUtils
                        .checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                                filePath );
            } else {
                SiteWrapper[] expSiteList = { rootSite, branceSite };
                ScmFileUtils
                        .checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                                filePath );
            }
        }
    }

    private void checkLocationLists(
            List< List< ScmFileLocation > > befLocLists,
            List< List< ScmFileLocation > > aftLocLists )
            throws Exception {
        Assert.assertEquals( befLocLists.size(), aftLocLists.size(),
                "file count is different!" );
        for ( int i = 0; i < startNum; ++i ) {
            checkLastAccessTime( befLocLists.get( i ), aftLocLists.get( i ),
                    false );
        }
        for ( int i = startNum; i < befLocLists.size(); ++i ) {
            checkLastAccessTime( befLocLists.get( i ), aftLocLists.get( i ),
                    true );
        }
    }

    private void checkLastAccessTime( List< ScmFileLocation > befLocList,
            List< ScmFileLocation > aftLocList,
            boolean isClean ) throws Exception {

        Assert.assertEquals( aftLocList.size(), isClean ? 1 : 2,
                "site count wrong after clean" );
        Date befDate = getLastAccessTime( befLocList, rootSite.getSiteId() );
        Date aftDate = getLastAccessTime( aftLocList, rootSite.getSiteId() );
        Assert.assertEquals( aftDate.getTime(), befDate.getTime() );
    }

    private Date getLastAccessTime( List< ScmFileLocation > locList,
            int siteId ) throws Exception {
        ScmFileLocation matchLoc = null;
        for ( ScmFileLocation loc : locList ) {
            if ( loc.getSiteId() == siteId ) {
                matchLoc = loc;
                break;
            }
        }
        if ( null == matchLoc ) {
            throw new Exception( "no such site id on the location list" );
        }
        return matchLoc.getDate();
    }

    private List< List< ScmFileLocation > > getLocationLists(
            List< ScmId > fileIdList ) throws ScmException {
        ScmSession ss = null;
        try {
            List< List< ScmFileLocation > > locationLists = new ArrayList<>();
            ss = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), ss );
            for ( ScmId fileId : fileIdList ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                List< ScmFileLocation > locationList = file.getLocationList();
                locationLists.add( locationList );
            }
            return locationLists;
        } finally {
            if ( null != ss ) {
                ss.close();
            }
        }
    }

}