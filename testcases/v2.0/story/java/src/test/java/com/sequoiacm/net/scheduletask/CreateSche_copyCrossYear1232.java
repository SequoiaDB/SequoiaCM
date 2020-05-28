package com.sequoiacm.net.scheduletask;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @FileName SCM-1232:创建调度任务，指定存活时间跨年
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_copyCrossYear1232 extends TestScmBase {
    private final static int fileNum = 1;
    private final static int fileSize = 100;
    private final static String name = "schetask1232";
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private ScmSession session = null;
    private String wsName = "ws1232";
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;

    private ScmId scheduleId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
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
        session = TestScmTools.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
    }

    @Test(groups = { "twoSite", "fourSite" }) // jira-232
    private void test() throws Exception {
        Calendar cal = Calendar.getInstance();
        String maxStayTime = "366d";
        // create schedule task
        this.readyScmFile( 0, 1, null );
        // write scmFile
        cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) - 1 );
        cal.set( Calendar.DAY_OF_YEAR, cal.get( Calendar.DAY_OF_YEAR ) - 2 );
        this.readyScmFile( 1, fileNum, cal.getTime() );
        // create schedule task
        this.createScheduleTask( maxStayTime );
        // check scmFile
        SiteWrapper[] expSites = { rootSite, branSite };
        this.checkScmFile( 1, fileNum, expSites );

        cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) - 1 );
        // write scmFile again and check
        this.readyScmFile( fileNum, fileNum + 1, cal.getTime() );
        SiteWrapper[] expSites2 = { rootSite, branSite };
        this.checkScmFile( fileNum, fileNum + 1, expSites2 );

        // check one ScmFile
        SiteWrapper[] expSites4 = { branSite };
        this.checkScmFile( 0, 1, expSites4 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( null != scheduleId ) {
            this.deleteScheduleTask();
        }
        ScmWorkspaceUtil.deleteWs( wsName, session );
        if ( session != null ) {
            session.close();
        }
        if ( runSuccess || TestScmBase.forceClear ) {
            TestTools.LocalFile.removeFile( localPath );
        }
    }

    private void readyScmFile( int startNum, int endNum, Date dateStr )
            throws ScmException, ParseException {
        ScmSession ss = null;
        try {
            ss = TestScmTools.createSession( branSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, ss );

            for ( int i = startNum; i < endNum; i++ ) {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( name + "_" + i );
                if ( dateStr != null ) {
                    file.setCreateTime( dateStr );
                }
                file.setAuthor( name );
                ScmId fileId = file.save();
                fileIds.add( fileId );
            }
        } finally {
            if ( null != ss )
                ss.close();
        }
    }

    private void createScheduleTask( String maxStayTime ) throws ScmException {
        ScmSession ss = null;
        try {
            ss = TestScmTools.createSession( branSite );

            ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                    branSite.getSiteName(), rootSite.getSiteName(), maxStayTime,
                    queryCond );
            // System.out.println(content.toBSONObject());
            String cron = "* * * * * ?";
            ScmSchedule sche = ScmSystem.Schedule.create( ss, wsName,
                    ScheduleType.COPY_FILE, name, "", content, cron );
            scheduleId = sche.getId();

            ScmSchedule sche2 = ScmSystem.Schedule.get( ss, scheduleId );
            Assert.assertEquals( sche2.getContent(), content );
            Assert.assertEquals( sche2.getCron(), cron );
        } finally {
            if ( null != ss )
                ss.close();
        }
    }

    private void deleteScheduleTask() throws Exception {
        ScmSession ss = null;
        try {
            ss = TestScmTools.createSession( branSite );
            ScmSystem.Schedule.delete( ss, scheduleId );
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmScheduleUtils.cleanTask( ss, scheduleId );
            }
        } finally {
            if ( null != ss )
                ss.close();
        }
    }

    private void checkScmFile( int startNum, int endNum,
            SiteWrapper[] expSites ) throws Exception {
        ScmSession ss = null;
        try {
            ss = TestScmTools.createSession( branSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, ss );
            ScmScheduleUtils.checkScmFile( ws, fileIds, startNum, endNum,
                    expSites );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != ss )
                ss.close();
        }
    }
}