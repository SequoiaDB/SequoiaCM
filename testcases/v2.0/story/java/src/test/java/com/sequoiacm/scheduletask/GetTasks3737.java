package com.sequoiacm.scheduletask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
import com.sequoiacm.exception.ScmError;
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
 * @Description SCM-3737:getTasks接口参数校验
 * @Author zhangyanan
 * @Date 2021.8.28
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.8.28
 * @version 1.00
 */
public class GetTasks3737 extends TestScmBase {
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3737";
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

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpPath = localPath + File.separator + "tmpFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( tmpPath, fileSize / 2 );

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

    @Test(groups = { "twoSite", "fourSite", "star" })
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

        BSONObject updateOrderby = null;
        List< ScmTask > actTasks2 = copySchedule.getTasks( condition,
                updateOrderby, skip, limit );
        Collections.sort( actTasks2, new OrderBy() );
        Assert.assertEquals( expTasks.toString(), actTasks2.toString() );

        updateOrderby = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( "1" ).get();
        try {
            copySchedule.getTasks( condition, updateOrderby, skip, limit );
            Assert.fail( "error！the getTasks should failed！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.METASOURCE_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }

        updateOrderby = ScmQueryBuilder.start( ScmAttributeName.Task.ID )
                .is( 999 ).get();
        try {
            copySchedule.getTasks( condition, updateOrderby, skip, limit );
            Assert.fail( "error！the getTasks should failed！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.METASOURCE_ERROR
                    .getErrorCode() ) {
                throw e;
            }
        }

        int upadateSkip = 0;
        List< ScmTask > actTasks3 = copySchedule.getTasks( condition, orderby,
                upadateSkip, limit );
        Assert.assertEquals( expTasks.toString(), actTasks3.toString() );

        upadateSkip = -1;
        List< ScmTask > actTasks4 = copySchedule.getTasks( condition, orderby,
                upadateSkip, limit );
        Assert.assertEquals( expTasks.toString(), actTasks4.toString() );

        int updateLimit = 0;
        List< ScmTask > actTasks5 = copySchedule.getTasks( condition, orderby,
                skip, updateLimit );
        List< ScmTask > expTasks5 = new ArrayList< ScmTask >();
        Assert.assertEquals( expTasks5.toString(), actTasks5.toString() );

        updateLimit = -1;
        List< ScmTask > actTasks6 = copySchedule.getTasks( condition, orderby,
                skip, updateLimit );
        Assert.assertEquals( expTasks.toString(), actTasks6.toString() );

        updateOrderby = ScmQueryBuilder.start( ScmAttributeName.Task.ID )
                .is( -1 ).get();
        List< ScmTask > actTasks1 = copySchedule.getTasks( condition,
                updateOrderby, skip, limit );
        List< ScmTask > expTasks1 = expTasks.subList( 0, expTasks.size() );
        Collections.sort( expTasks1, new reverseOrderBy() );
        Assert.assertEquals( expTasks1.toString(), actTasks1.toString() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
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

    public class reverseOrderBy implements Comparator< ScmTask > {
        public int compare( ScmTask obj1, ScmTask obj2 ) {
            int flag = 0;
            String no1 = obj1.getId().get();
            String no2 = obj2.getId().get();
            if ( no1.compareTo( no2 ) < 0 ) {
                flag = 1;
            } else if ( no1.compareTo( no2 ) > 0 ) {
                flag = -1;
            }
            return flag;
        }
    }

    public class OrderBy implements Comparator< ScmTask > {
        public int compare( ScmTask obj1, ScmTask obj2 ) {
            int flag = 0;
            String no1 = obj1.getId().get();
            String no2 = obj2.getId().get();
            if ( no1.compareTo( no2 ) > 0 ) {
                flag = 1;
            } else if ( no1.compareTo( no2 ) < 0 ) {
                flag = -1;
            }
            return flag;
        }
    }
}
