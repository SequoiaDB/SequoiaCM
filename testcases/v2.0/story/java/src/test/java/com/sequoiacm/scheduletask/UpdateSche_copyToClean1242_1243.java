package com.sequoiacm.scheduletask;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
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
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
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
 * @FileName SCM-1242:更新name/desc SCM-1243:原type为迁移，更新为清理，并更新源和目标站点满足清理条件
 *           SCM-1246:原type为清理，更新为迁移，并更新源和目标站点满足迁移条件
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class UpdateSche_copyToClean1242_1243 extends TestScmBase {
    private final static int fileNum = 1;
    private final static int fileSize = 100;
    private final static String name = "schetask1243";
    private final static String cron = "* * * * * ?";
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
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws Exception {
        try {
            // write scmFile and check
            this.readyScmFile( wsA, 0, fileNum );
            SiteWrapper[] expSites1 = { branSite };
            ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites1 );

            // create schedule task, type is copy, and check
            this.createScheduleTask();
            SiteWrapper[] expSites2 = { rootSite, branSite };
            ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites2 );

            // update schedule task to clean, and check
            this.updateScheTaskToCleanTask();
            SiteWrapper[] expSites3 = { rootSite };
            ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites3 );
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
            file.setAuthor( name );
            file.setCreateTime( calendar.getTime() );
            ScmId fileId = file.save();
            fileIds.add( fileId );
        }
    }

    private void createScheduleTask() {
        try {
            ScheduleType taskType = ScheduleType.COPY_FILE;
            String maxStayTime = "0d";
            ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                    branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                    queryCond );
            // System.out.println(content.toBSONObject());
            ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                    taskType, name, "desc", content, cron );
            scheduleId = sche.getId();

            // check shedule info
            ScmSchedule sche2 = ScmSystem.Schedule.get( ssA, scheduleId );
            Assert.assertEquals( sche2.getId(), scheduleId );
            Assert.assertEquals( sche2.getType(), taskType );
            Assert.assertEquals( sche2.getName(), name );
            Assert.assertEquals( sche2.getDesc(), "desc" );
            Assert.assertEquals( sche2.getContent(), content );
            Assert.assertEquals( sche2.getCron(), cron );
            Assert.assertEquals( sche2.getWorkspace(), wsp.getName() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void updateScheTaskToCleanTask() {
        String newName = name + "_clean";
        String newDesc = "desc_123";
        try {
            ScmSchedule sche = ScmSystem.Schedule.get( ssA, scheduleId );

            ScheduleType taskType = ScheduleType.CLEAN_FILE;
            String maxStayTime = "0d";
            ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                    branSite.getSiteName(), maxStayTime, queryCond );

            sche.updateName( newName );
            sche.updateDesc( newDesc );
            sche.updateSchedule( taskType, content );

            // check shedule info
            ScmSchedule sche2 = ScmSystem.Schedule.get( ssA, scheduleId );
            Assert.assertEquals( sche2.getId(), scheduleId );
            Assert.assertEquals( sche2.getType(), taskType );
            Assert.assertEquals( sche2.getName(), newName );
            Assert.assertEquals( sche2.getDesc(), newDesc );
            Assert.assertEquals( sche2.getContent(), content );
            Assert.assertEquals( sche2.getCron(), cron );
            Assert.assertEquals( sche2.getWorkspace(), wsp.getName() );
            // Assert.assertEquals(sche2.getCreaateUser(),
            // TestScmBase.scmUserName);
            // Assert.assertEquals(sche2.getCreateDate(), scheduleId);
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

}