package com.sequoiacm.schedulePreferredZone;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description SCM-3711:创建调度任务参数校验
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class CreateSchedule3711B extends TestScmBase {
    private final int fileSize = 1024 * 100;
    private final String fileName = "file3711B";
    private String region;
    private String zone;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private WsWrapper wsp = null;
    private ScmSession sourceSiteSession;
    private ScmSession targetSiteSession;
    private ScmWorkspace sourceSiteWs;
    private SiteWrapper sourceSite;
    private SiteWrapper targetSite;
    private List< ScmId > fileIds = new ArrayList<>();
    private BSONObject queryCond;
    private ScmScheduleBuilder schBuilder;
    private ScmSchedule schedule;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        region = TestScmBase.defaultRegion;
        zone = TestScmBase.zone2;
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpPath = localPath + File.separator + "tmpFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( tmpPath, fileSize / 2 );

        wsp = ScmInfo.getWs();
        sourceSite = ScmInfo.getBranchSite();
        targetSite = ScmInfo.getRootSite();
        sourceSiteSession = TestScmTools.createSession( sourceSite );
        targetSiteSession = TestScmTools.createSession( targetSite );
        sourceSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                sourceSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ScmId scmId = ScmFileUtils.create( sourceSiteWs, fileName, filePath );
        fileIds.add( scmId );
        schBuilder = ScmSystem.Schedule.scheduleBuilder( sourceSiteSession );
    }

    @Test
    public void test() throws ScmException {
        String maxStayTime = "0d";
        // 更新校验
        String scheduleName = "testCopy" + fileName;
        ScmScheduleCopyFileContent copyContent = new ScmScheduleCopyFileContent(
                sourceSite.getSiteName(), targetSite.getSiteName(), maxStayTime,
                queryCond );
        schBuilder.type( ScheduleType.COPY_FILE ).workspace( wsp.getName() )
                .name( scheduleName ).description( "copy " + fileName )
                .content( copyContent ).cron( "* * * * * ?" ).enable( true )
                .preferredRegion( region ).preferredZone( zone );
        schedule = schBuilder.build();
        try {
            schedule.updatePreferredRegion( null );
            Assert.fail( "except fail but succeed" );
        } catch ( ScmException e ) {
            if ( !( e.getError().equals( ScmError.INVALID_ARGUMENT ) ) ) {
                throw e;
            }
        }

        try {
            schedule.updatePreferredZone( null );
            Assert.fail( "except fail but succeed" );
        } catch ( ScmException e ) {
            if ( !( e.getError().equals( ScmError.INVALID_ARGUMENT ) ) ) {
                throw e;
            }
        }

        try {
            schBuilder = ScmSystem.Schedule.scheduleBuilder( null );
            Assert.fail( "except fail but succeed" );
        } catch ( ScmException e ) {
            if ( !( e.getError().equals( ScmError.INVALID_ARGUMENT ) ) ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmSystem.Schedule.delete( sourceSiteSession,
                        schedule.getId() );
                ScmScheduleUtils.cleanTask( sourceSiteSession,
                        schedule.getId() );
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } catch ( Exception e ) {
                e.printStackTrace();
            } finally {
                sourceSiteSession.close();
                targetSiteSession.close();
            }
        }
    }
}