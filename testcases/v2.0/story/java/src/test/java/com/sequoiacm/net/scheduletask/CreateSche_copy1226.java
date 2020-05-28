package com.sequoiacm.net.scheduletask;

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
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @FileName SCM-1226:创建调度任务，类型为迁移，指定源和目标站点正确
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_copy1226 extends TestScmBase {
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "schetask1226";
    private boolean runSuccess = false;
    private ScmSession ssA = null;
    private ScmWorkspace wsA = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;

    private ScmId scheduleId = null;
    private ScmScheduleContent content = null;
    private String cron = null;

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
            wsp = ScmInfo.getWs();
            ssA = TestScmTools.createSession( branSite );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), ssA );

            // clean environment
            queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                    .is( name ).get();
            ScmFileUtils.cleanFile( wsp, queryCond );

            // ready scmFile
            this.readyScmFile( wsA, 0, fileNum );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        SiteWrapper[] expSites = { rootSite, branSite };
        try {
            this.createScheduleTask();
            this.checkScheduleTaskInfo();
            ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites );

            this.readyScmFile( wsA, fileNum, fileNum + 10 );
            ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            ScmSystem.Schedule.delete( ssA, scheduleId );
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
                ScmScheduleUtils.cleanTask( ssA, scheduleId );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
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
            file.setCreateTime( calendar.getTime() );
            file.setAuthor( name );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private void createScheduleTask() {
        String maxStayTime = "0d";
        try {
            // test interface
            String srName = new ScmScheduleCopyFileContent(
                    branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                    queryCond ).getSourceSiteName();
            String trName = new ScmScheduleCopyFileContent(
                    branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                    queryCond ).getTargetSiteName();
            String msTime = new ScmScheduleCopyFileContent(
                    branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                    queryCond ).getMaxStayTime();
            BSONObject eCond = new ScmScheduleCopyFileContent(
                    branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                    queryCond ).getExtraCondition();
            Assert.assertEquals( srName, branSite.getSiteName() );
            Assert.assertEquals( trName, rootSite.getSiteName() );
            Assert.assertEquals( msTime, maxStayTime );
            Assert.assertEquals( eCond, queryCond );

            // create schedule task
            content = new ScmScheduleCopyFileContent( branSite.getSiteName(),
                    rootSite.getSiteName(), maxStayTime, queryCond );
            cron = "* * * * * ?";
            ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                    ScheduleType.COPY_FILE, name, "", content, cron );
            scheduleId = sche.getId();
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void checkScheduleTaskInfo() {
        try {
            ScmSchedule sche = ScmSystem.Schedule.get( ssA, scheduleId );
            // System.out.println("sche info :" + sche);
            Assert.assertEquals( sche.getId(), scheduleId );
            Assert.assertEquals( sche.getType(), ScheduleType.COPY_FILE );
            Assert.assertEquals( sche.getName(), name );
            Assert.assertEquals( sche.getDesc(), "" );
            Assert.assertEquals( sche.getContent(), content );
            Assert.assertEquals( sche.getCron(), cron );
            Assert.assertEquals( sche.getWorkspace(), wsp.getName() );
            // Assert.assertEquals(sche.getCreaateUser(),
            // TestScmBase.scmUserName);
            // Assert.assertEquals(sche.getCreateDate(), scheduleId);
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

}