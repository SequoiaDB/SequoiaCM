package com.sequoiacm.scheduletask.concurrent;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
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
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Testcase: SCM-1265:并发创建相同调度任务，指定不同ws
 * @author huangxiaoni init
 * @date 2018.4.24
 */

public class CreateSche_wsDiff1265 extends TestScmBase {
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "schetask1265";
    private boolean runSuccess = false;
    private ScmSession ssA = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsB = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private List< WsWrapper > wss = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;

    private List< ScmId > scheIds = Collections
            .synchronizedList( new ArrayList< ScmId >() );

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
            rootSite = ScmInfo.getRootSite();
            branSite = ScmInfo.getBranchSite();
            wss = ScmInfo.getWss( 2 );
            ssA = TestScmTools.createSession( branSite );
            wsA = ScmFactory.Workspace.getWorkspace( wss.get( 0 ).getName(),
                    ssA );
            wsB = ScmFactory.Workspace.getWorkspace( wss.get( 1 ).getName(),
                    ssA );

            // clean environment
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( name ).get();
            ScmFileUtils.cleanFile( wss.get( 0 ), queryCond );
            ScmFileUtils.cleanFile( wss.get( 1 ), queryCond );

            // ready scmFile
            this.readyScmFile( wsA, 0, fileNum / 2 );
            this.readyScmFile( wsB, fileNum / 2, fileNum );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            CreateSchedule crtSche1 = new CreateSchedule( wss.get( 0 ) );
            crtSche1.start();

            CreateSchedule crtSche2 = new CreateSchedule( wss.get( 1 ) );
            crtSche2.start();

            if ( !( crtSche1.isSuccess() && crtSche2.isSuccess() ) ) {
                Assert.fail( crtSche1.getErrorMsg() + crtSche2.getErrorMsg() );
            }

            SiteWrapper[] expSites = { rootSite, branSite };
            ScmScheduleUtils.checkScmFile( wsA, fileIds, 0, fileNum / 2,
                    expSites );
            ScmScheduleUtils.checkScmFile( wsB, fileIds, fileNum / 2, fileNum,
                    expSites );
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
                ScmSystem.Schedule.delete( ssA, scheduleId );
                if ( runSuccess || forceClear ) {
                    ScmScheduleUtils.cleanTask( ssA, scheduleId );
                }
            }

            if ( runSuccess || forceClear ) {
                ScmFileUtils.cleanFile( wss.get( 0 ), queryCond );
                ScmFileUtils.cleanFile( wss.get( 1 ), queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }

        }
    }

    private void readyScmFile( ScmWorkspace ws, int startNum, int endNum )
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

    private class CreateSchedule extends TestThreadBase {
        private WsWrapper wsp = null;

        public CreateSchedule( WsWrapper wsp ) {
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
}
