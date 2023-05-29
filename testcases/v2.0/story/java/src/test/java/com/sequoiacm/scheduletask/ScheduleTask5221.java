package com.sequoiacm.scheduletask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Description SCM-5221:创建迁移并清理任务，设置强一致检测
 * @Author zhangyanan
 * @Date 2022.09.21
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2022.09.21
 * @version 1.00
 */
public class ScheduleTask5221 extends TestScmBase {
    private final static int fileSize = 1024;
    private final static String filename = "file5221";
    private ScmSession rootSiteSession = null;
    private ScmWorkspace rootSiteWs = null;
    private SiteWrapper rootSite = null;
    private ScmSession branSiteSession = null;
    private ScmWorkspace branSiteWs = null;
    private SiteWrapper branchSite = null;
    private WsWrapper wsp = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private BSONObject queryCond = null;
    private ScmSchedule scmSchedule;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpPath = localPath + File.separator + "tmpFile_" + fileSize + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( tmpPath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        rootSiteSession = ScmSessionUtils.createSession( rootSite );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );

        branSiteSession = ScmSessionUtils.createSession( branchSite );
        branSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branSiteSession );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( filename ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        prepareDate();
    }

    // 问题单http://jira.web:8080/browse/SEQUOIACM-1399未修改，屏蔽用例
    @Test(groups = { "twoSite", "fourSite" },enabled = false)
    public void test() throws Exception {
        // 创建迁移清理任务,指定强校验
        scmSchedule = ScmScheduleUtils.createMoveSchedule( rootSiteSession,
                rootSite, branchSite, wsp.getName(), queryCond,
                ScmDataCheckLevel.STRICT, false, false );

        SiteWrapper[] expSites1 = { branchSite };
        ScmScheduleUtils.waitForTask( scmSchedule, 2 );
        ScmScheduleUtils.checkScmFile( branSiteWs, fileIdList, expSites1 );

        scmSchedule.disable();
        ScmSystem.Schedule.delete( rootSiteSession, scmSchedule.getId() );
        ScmScheduleUtils.cleanTask( rootSiteSession, scmSchedule.getId() );

        ScmFactory.File.asyncCache( rootSiteWs, fileIdList.get( 0 ) );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileIdList.get( 0 ),
                2 );

        // 替换分站点上的数据文件lob
        TestSdbTools.Lob.removeLob( branchSite, wsp, fileIdList.get( 0 ) );
        TestSdbTools.Lob.putLob( branchSite, wsp, fileIdList.get( 0 ),
                tmpPath );

        // 创建迁移清理任务,指定强校验
        scmSchedule = ScmScheduleUtils.createMoveSchedule( rootSiteSession,
                rootSite, branchSite, wsp.getName(), queryCond,
                ScmDataCheckLevel.STRICT, false, false );
        checkTaskFailCount( 1 );
        SiteWrapper[] expSites2 = { rootSite, branchSite };
        ScmScheduleUtils.checkScmFile( rootSiteWs, fileIdList, expSites2 );
        scmSchedule.disable();
        ScmSystem.Schedule.delete( rootSiteSession, scmSchedule.getId() );
        ScmScheduleUtils.cleanTask( rootSiteSession, scmSchedule.getId() );

        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( rootSiteSession != null ) {
                rootSiteSession.close();
            }
            if ( branSiteSession != null ) {
                branSiteSession.close();
            }
        }
    }

    public void prepareDate() throws Exception {
        // 主站点创建文件后缓存至分站点
        ScmId fileId = ScmFileUtils.create( rootSiteWs, filename, filePath );
        fileIdList.add( fileId );
        ScmFactory.File.asyncCache( branSiteWs, fileId );
        ScmTaskUtils.waitAsyncTaskFinished( rootSiteWs, fileId, 2 );
    }

    public void checkTaskFailCount( int failedNums ) throws Exception {
        int time = 0;
        boolean isEnd = false;
        while ( true ) {
            List< ScmTask > tasks = scmSchedule.getTasks( new BasicBSONObject(),
                    new BasicBSONObject(), 0, -1 );
            for ( ScmTask task : tasks ) {
                if ( task.getFailCount() == failedNums ) {
                    isEnd = true;
                }
            }
            if ( isEnd ) {
                break;
            }
            time++;
            if ( time > 120 ) {
                throw new Exception( "waiting for task timeout" );
            }
            Thread.sleep( 1000 );
        }

    }
}