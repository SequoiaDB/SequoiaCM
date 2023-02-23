package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5264:使用ScheduleCleanFileContent创建清理任务，驱动测试
 * @Author YiPan
 * @CreateDate 2022/9/27
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class ScmScheduleCleanFileContent5264 extends TestScmBase {
    private String fileName = "file5264_";
    private String taskName = "task5264";
    private String fileTitleA = "titleA5264";
    private String fileTitleB = "titleB5264";
    private String fileAuthor = "author5264";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private List< ScmId > fileIdsA = new ArrayList<>();
    private List< ScmId > fileIdsB = new ArrayList<>();
    private ScmSession sessionM = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private ScmSchedule sche;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // a.b.e.测试默认参数
        testDefaultOption( queryCond );

        // f.测试获取参数
        testGetOption();

        // c.测试匹配条件bson为空
        cacheToBranchSite();
        testBson( new BasicBSONObject() );

        // d.测试匹配条件title=titleA5264的文件
        cacheToBranchSite();
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.TITLE )
                .is( fileTitleA ).get();
        testBson( cond );

        runSuccess = true;
    }

    private void testGetOption() throws Exception {
        ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                branchSite.getSiteName(), "0d", queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, 12000,
                ScmDataCheckLevel.STRICT, true, true );
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.CLEAN_FILE, taskName, "", content,
                "0/1 * * * * ?" );
        Assert.assertEquals( content.getSiteName(), branchSite.getSiteName() );
        Assert.assertEquals( content.getMaxStayTime(), "0d" );
        Assert.assertEquals( content.getExtraCondition(), queryCond );
        Assert.assertEquals( content.getScope(),
                ScmType.ScopeType.SCOPE_CURRENT );
        Assert.assertEquals( content.getMaxExecTime(), 12000 );
        Assert.assertEquals( content.getDataCheckLevel(),
                ScmDataCheckLevel.STRICT );
        ScmSystem.Schedule.delete( sessionM, sche.getId() );
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
    }

    private void testDefaultOption( BSONObject queryCond ) throws Exception {
        ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                branchSite.getSiteName(), "0d", queryCond,
                ScmType.ScopeType.SCOPE_CURRENT );
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.CLEAN_FILE, taskName, "", content,
                "0/1 * * * * ?" );
        BSONObject bsonObject = sche.getContent().toBSONObject();
        Assert.assertEquals( bsonObject.get( "data_check_level" ), "week" );
        Assert.assertEquals( bsonObject.get( "quick_start" ), false );
        Assert.assertEquals( bsonObject.get( "is_recycle_space" ), false );
        ScmSystem.Schedule.delete( sessionM, sche.getId() );
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
    }

    private void testBson( BSONObject queryCond ) throws Exception {
        ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                branchSite.getSiteName(), "0d", queryCond,
                ScmType.ScopeType.SCOPE_CURRENT );
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.CLEAN_FILE, taskName, "", content,
                "0/1 * * * * ?" );
        ScmScheduleUtils.waitForTask( sche, 2 );
        SiteWrapper[] expASites = { rootSite };
        SiteWrapper[] expBSites = { rootSite, branchSite };
        if ( queryCond.equals( new BasicBSONObject() ) ) {
            expBSites = new SiteWrapper[] { rootSite };
        }
        ScmFileUtils.checkMetaAndData( wsp, fileIdsA, expASites, localPath,
                filePath );
        ScmFileUtils.checkMetaAndData( wsp, fileIdsB, expBSites, localPath,
                filePath );
        ScmSystem.Schedule.delete( sessionM, sche.getId() );
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
            } finally {
                sessionM.close();
            }
        }
    }

    private void createFile( ScmWorkspace ws ) throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            if ( i % 2 == 0 ) {
                file.setTitle( fileTitleA );
                fileIdsA.add( file.save() );
            } else {
                file.setTitle( fileTitleB );
                fileIdsB.add( file.save() );
            }
        }
    }

    private void cacheToBranchSite() throws Exception {
        ScmId taskId = ScmSystem.Task.startTransferTask( wsM, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, branchSite.getSiteName() );
        ScmTaskUtils.waitTaskFinish( sessionM, taskId );
    }
}