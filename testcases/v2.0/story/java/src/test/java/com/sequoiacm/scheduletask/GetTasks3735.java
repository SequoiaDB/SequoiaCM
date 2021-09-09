package com.sequoiacm.scheduletask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;

/**
 * @Description SCM-3735:getTasks接口指定limit参数获取task信息
 * @Author zhangyanan
 * @Date 2021.8.28
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.8.28
 * @version 1.00
 */
public class GetTasks3735 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3735";
    private String region;
    private String zone;
    private SiteWrapper branchSite;
    private SiteWrapper rootSite;
    private ScmSession rootSiteSession;
    private ScmSession branchSiteSession;
    private ScmWorkspace branchSiteWorkspace;
    private List< ScmId > scheduleIds = new ArrayList<>();
    private BSONObject queryCond;
    private WsWrapper wsp = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws IOException, ScmException {
        zone = TestScmBase.zone1;
        region = TestScmBase.defaultRegion;
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        branchSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        branchSiteSession = TestScmTools.createSession( branchSite );
        rootSiteSession = TestScmTools.createSession( rootSite );
        branchSiteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        ScmId scmId = ScmFileUtils.create( branchSiteWorkspace, fileName,
                filePath );
        fileIds.add( scmId );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmSchedule copySchedule = ScmScheduleUtils.createCopySchedule(
                branchSiteSession, branchSite, rootSite, wsp, queryCond, region,
                zone );
        scheduleIds.add( copySchedule.getId() );
        ScmScheduleUtils.waitForTask( copySchedule, 10 );
        copySchedule.disable();
        ScmId taskId = copySchedule.getLatestTask().getId();
        ScmTaskUtils.waitTaskFinish( branchSiteSession, taskId );
        BSONObject condition = null;
        int skip = 0;
        int limit = -1;
        BSONObject orderby = ScmQueryBuilder.start( ScmAttributeName.Task.ID )
                .is( 1 ).get();
        List< ScmTask > expTasks = copySchedule.getTasks( condition, orderby,
                skip, limit );

        limit = 1;
        List< ScmTask > actTasks1 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        List< ScmTask > expTasks1 = expTasks.subList( 0, limit );
        Assert.assertEquals( actTasks1.toString(), expTasks1.toString() );

        limit = 5;
        List< ScmTask > actTasks2 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        List< ScmTask > expTasks2 = expTasks.subList( 0, limit );
        Assert.assertEquals( actTasks2.toString(), expTasks2.toString() );

        limit = expTasks.size();
        List< ScmTask > actTasks3 = copySchedule.getTasks( condition, orderby,
                skip, limit );
        Assert.assertEquals( actTasks3.toString(), expTasks.toString() );

        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId schedule : scheduleIds ) {
                    ScmSystem.Schedule.delete( branchSiteSession, schedule );
                    ScmScheduleUtils.cleanTask( branchSiteSession, schedule );
                }
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                rootSiteSession.close();
                branchSiteSession.close();
            }
        }
    }

    public class OrderBy implements Comparator< ScmTask > {
        public int compare( ScmTask obj1, ScmTask obj2 ) {
            int flag = 0;
            String no1 = obj1.getId().toString();
            String no2 = obj2.getId().toString();
            if ( no1.compareTo( no2 ) > 0 ) {
                flag = 1;
            } else if ( no1.compareTo( no2 ) < 0 ) {
                flag = -1;
            }
            return flag;
        }
    }

}
