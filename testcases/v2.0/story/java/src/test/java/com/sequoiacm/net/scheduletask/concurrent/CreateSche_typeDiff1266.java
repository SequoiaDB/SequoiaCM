package com.sequoiacm.net.scheduletask.concurrent;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Testcase: SCM-1266:并发创建多个调度任务，覆盖多种调度任务类型，指定不同ws
 * @author huangxiaoni init
 * @date 2018.4.24
 */

public class CreateSche_typeDiff1266 extends TestScmBase {
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "schetask1266";
    private boolean runSuccess = false;
    private ScmSession ssR = null;
    private ScmSession ssA = null;
    private ScmSession ssA1 = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsA1 = null;
    private ScmWorkspace wsR = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite1 = null;
    private SiteWrapper branSite1 = null;
    private List< WsWrapper > wss = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;

    private List< ScmId > scheIds = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            // ready local file
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            // get site and workspace, create session
            wss = ScmInfo.getWss( 2 );
            List< SiteWrapper > sites = ScmNetUtils
                    .getCleanSites( wss.get( 0 ) );
            rootSite = sites.get( 1 );
            branSite = sites.get( 0 );

            List< SiteWrapper > sites1 = ScmNetUtils
                    .getCleanSites( wss.get( 1 ) );
            rootSite1 = sites1.get( 1 );
            branSite1 = sites1.get( 0 );

            ssA = TestScmTools.createSession( branSite );
            ssR = TestScmTools.createSession( rootSite1 );
            ssA1 = TestScmTools.createSession( branSite1 );

            wsA = ScmFactory.Workspace.getWorkspace( wss.get( 0 ).getName(),
                    ssA );

            wsR = ScmFactory.Workspace.getWorkspace( wss.get( 1 ).getName(),
                    ssR );
            wsA1 = ScmFactory.Workspace.getWorkspace( wss.get( 1 ).getName(),
                    ssA1 );

            // clean environment
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( name ).get();
            ScmFileUtils.cleanFile( wss.get( 0 ), queryCond );
            ScmFileUtils.cleanFile( wss.get( 1 ), queryCond );

            // ready scmFile
            this.writeScmFile( wsA, 0, fileNum / 2 );

            this.writeScmFile( wsR, fileNum / 2, fileNum );
            this.readyScmFile( wsA1, fileNum / 2, fileNum );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            CreateCopeSche copeSche = new CreateCopeSche( wss.get( 0 ) );
            copeSche.start();

            CreateCleanSche cleanSche = new CreateCleanSche( wss.get( 1 ) );
            cleanSche.start();

            if ( !( copeSche.isSuccess() && cleanSche.isSuccess() ) ) {
                Assert.fail( copeSche.getErrorMsg() + cleanSche.getErrorMsg() );
            }

            SiteWrapper[] expSites1 = { rootSite, branSite };
            ScmScheduleUtils.checkScmFile( wsA, fileIds, 0, fileNum / 2,
                    expSites1 );

            SiteWrapper[] expSites2 = { rootSite1 };
            ScmScheduleUtils.checkScmFile( wsR, fileIds, fileNum / 2, fileNum,
                    expSites2 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            for ( ScmId scheduleId : scheIds ) {
                if ( null != scheduleId ) {
                    ScmSystem.Schedule.delete( ssA, scheduleId );
                    if ( runSuccess || forceClear ) {
                        ScmScheduleUtils.cleanTask( ssA, scheduleId );
                    }
                }
            }

            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wss.get( 0 ), queryCond );
                TestTools.LocalFile.removeFile( localPath );

                ScmFileUtils.cleanFile( wss.get( 1 ), queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( ssR != null ) {
                ssR.close();
            }
            if ( ssA != null ) {
                ssA.close();
            }

        }
    }

    private void writeScmFile( ScmWorkspace ws, int startNum, int endNum )
            throws ScmException, ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.HOUR, calendar.get( Calendar.HOUR ) - 3 );
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( name + "_" + i );
            file.setAuthor( name );
            file.setCreateTime( calendar.getTime() );
            file.setTitle( "" + startNum );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private void readyScmFile( ScmWorkspace ws, int startNum, int endNum )
            throws Exception {
        ScmCursor< ScmFileBasicInfo > cursor = null;
        try {
            for ( int i = startNum; i < endNum; i++ ) {
                cursor = ScmFactory.File.listInstance( ws,
                        ScopeType.SCOPE_CURRENT, queryCond );
                while ( cursor.hasNext() ) {
                    ScmFileBasicInfo info = cursor.getNext();
                    ScmId fileId = info.getFileId();
                    ScmFile file2 = ScmFactory.File.getInstance( ws, fileId );
                    String downloadPath = TestTools.LocalFile.initDownloadPath(
                            localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
                    file2.getContent( downloadPath );
                }
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != cursor )
                cursor.close();
        }
    }

    private class CreateCopeSche extends TestThreadBase {
        private WsWrapper wsp = null;

        public CreateCopeSche( WsWrapper wsp ) {
            this.wsp = wsp;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                String maxStayTime = "0d";
                ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                        branSite.getSiteName(), rootSite.getSiteName(),
                        maxStayTime, queryCond );
                String cron = "* * * * * ?";
                ScmSchedule sche = ScmSystem.Schedule.create( ssA,
                        wsp.getName(), ScheduleType.COPY_FILE, name, "",
                        content, cron );
                ScmId scheduleId = sche.getId();
                scheIds.add( scheduleId );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CreateCleanSche extends TestThreadBase {
        private WsWrapper wsp = null;

        public CreateCleanSche( WsWrapper wsp ) {
            this.wsp = wsp;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                String maxStayTime = "0d";
                ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                        branSite1.getSiteName(), maxStayTime, queryCond );
                String cron = "* * * * * ?";
                ScmSchedule sche = ScmSystem.Schedule.create( ssA1,
                        wsp.getName(), ScheduleType.CLEAN_FILE, name, "",
                        content, cron );
                ScmId scheduleId = sche.getId();
                scheIds.add( scheduleId );
            } catch ( ScmException e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

}
