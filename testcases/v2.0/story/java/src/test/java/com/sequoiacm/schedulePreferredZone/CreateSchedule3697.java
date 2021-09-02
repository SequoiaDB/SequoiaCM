package com.sequoiacm.schedulePreferredZone;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
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
 * @description SCM-3697:region不存在，创建调度任务
 * @author YiPan
 * @createDate 2021.8.26
 * @updateUser YiPan
 * @updateDate 2021.8.26
 * @updateRemark
 * @version v1.0
 */
public class CreateSchedule3697 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3697";
    private String region;
    private String defaultRegion;
    private String zone;
    private WsWrapper wsp = null;
    private ScmSession rootStieSession;
    private ScmSession branchSite1Session;
    private SiteWrapper rootStie;
    private SiteWrapper branchSite1;
    private ScmWorkspace rootStieWs;
    private ScmWorkspace branchSite1Ws;
    private List< ScmId > fileIds = new ArrayList<>();
    private List< ScmId > scheduleIds = new ArrayList<>();
    private BSONObject queryCond;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        region = "wrong";
        defaultRegion = TestScmBase.defaultRegion;
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
        rootStie = ScmInfo.getRootSite();
        branchSite1 = ScmInfo.getBranchSite();
        rootStieSession = TestScmTools.createSession( rootStie );
        branchSite1Session = TestScmTools.createSession( branchSite1 );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        rootStieWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootStieSession );
        branchSite1Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1Session );

        ScmId fileId = ScmFileUtils.create( branchSite1Ws, fileName, filePath );
        fileIds.add( fileId );
    }

    @Test
    public void test() throws Exception {
        // 迁移任务
        ScmSchedule copySchedule = ScmScheduleUtils.createCopySchedule(
                rootStieSession, branchSite1, rootStie, wsp, queryCond, region,
                zone );
        scheduleIds.add( copySchedule.getId() );

        Assert.assertEquals( copySchedule.getPreferredRegion(), region );
        Assert.assertEquals( copySchedule.getPreferredZone(), zone );
        SiteWrapper[] expCopySites = { rootStie, branchSite1 };
        ScmScheduleUtils.checkScmFile( rootStieWs, fileIds, expCopySites );
        List< ScmTask > tasks = ScmScheduleUtils
                .getSuccessTasks( copySchedule );
        ScmScheduleUtils.checkNodeRegion( tasks, rootStieSession,
                defaultRegion );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId schedule : scheduleIds ) {
                    ScmSystem.Schedule.delete( rootStieSession, schedule );
                    ScmScheduleUtils.cleanTask( rootStieSession, schedule );
                }
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                rootStieSession.close();
                branchSite1Session.close();
            }
        }
    }
}