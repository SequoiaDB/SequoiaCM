package com.sequoiacm.scheduletask;

import java.io.File;
import java.util.Calendar;

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
 * @FileName SCM-1234:创建调度任务，调度内容中condition包含不支持的文件属性
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_fileAttrNotExist1234 extends TestScmBase {
    private final static int fileSize = 100;
    private final static String name = "schetask1234";
    private boolean runSuccess = false;
    private ScmSession ssA = null;
    private ScmWorkspace wsA = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private ScmId fileId = null;
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
            Calendar calendar = Calendar.getInstance();
            calendar.set( Calendar.HOUR, calendar.get( Calendar.HOUR ) - 3 );
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setContent( filePath );
            file.setFileName( name );
            file.setAuthor( name );
            file.setCreateTime( calendar.getTime() );
            fileId = file.save();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            this.createScheduleTask();
            this.checkScheduleTaskInfo();

            SiteWrapper[] expSites = { branSite };
            ScmFileUtils.checkMeta( wsA, fileId, expSites );
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

    private void createScheduleTask() {
        try {
            BSONObject cond = ScmQueryBuilder.start( "test" ).is( 1 ).get();
            String maxStayTime = "0d";
            content = new ScmScheduleCopyFileContent( branSite.getSiteName(),
                    rootSite.getSiteName(), maxStayTime, cond );
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
            Assert.assertEquals( sche.getId(), scheduleId );
            Assert.assertEquals( sche.getContent(), content );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}