package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleMoveFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5239:通过ScheduleMoveFileContent创建迁移并清理调度任务，驱动测试
 * @Author YiPan
 * @CreateDate 2022/9/27
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class ScmScheduleMoveFileContent5239 extends TestScmBase {
    private String fileName = "file5239_";
    private String taskName = "task5239";
    private String fileTitleA = "titleA5239";
    private String fileTitleB = "titleB5239";
    private String fileAuthor = "author5239";
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
    public void setUp() throws IOException, ScmException {
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
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // a.源站点为空
        testNullSite( "", branchSite.getSiteName() );

        // b.目标站点为空
        testNullSite( rootSite.getSiteName(), "" );

        // c.MaxStayTime设置文件不能被匹配
        testMaxStayTime();

        // d.设置bson条件为空
        testBson( new BasicBSONObject() );

        // e.设置bson条件待匹配条件
        createFile( wsM );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.TITLE )
                .in( fileTitleA ).get();
        testBson( cond );

        // f.获取对象所有属性校验
        testGetContent();
        runSuccess = true;
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

    private void createFile( ScmWorkspace ws ) throws ScmException {
        fileIdsA.clear();
        fileIdsB.clear();
        ScmFileUtils.cleanFile( wsp, queryCond );
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

    private ScmScheduleMoveFileContent getDefaultContent( BSONObject moveCond )
            throws ScmException {
        return new ScmScheduleMoveFileContent( rootSite.getSiteName(),
                branchSite.getSiteName(), "0d", moveCond,
                ScmType.ScopeType.SCOPE_CURRENT );
    }

    private void testGetContent() throws ScmException {
        ScmScheduleMoveFileContent content = getDefaultContent( queryCond );
        Assert.assertEquals( content.getSourceSiteName(),
                rootSite.getSiteName() );
        Assert.assertEquals( content.getTargetSiteName(),
                branchSite.getSiteName() );
        Assert.assertEquals( content.getExtraCondition(), queryCond );
        Assert.assertEquals( content.getScope(),
                ScmType.ScopeType.SCOPE_CURRENT );
        Assert.assertEquals( content.getMaxExecTime(), 0 );
        Assert.assertEquals( content.getDataCheckLevel(),
                ScmDataCheckLevel.WEEK );
        Assert.assertEquals( content.getMaxStayTime(), "0d" );
    }

    private void testBson( BSONObject queryCond ) throws Exception {
        ScmScheduleMoveFileContent content = getDefaultContent( queryCond );
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.MOVE_FILE, taskName, "", content,
                "0/10 * * * * ?" );
        ScmScheduleUtils.waitForTask( sche, 2 );
        SiteWrapper[] expASites = { branchSite };
        SiteWrapper[] expBSites = { rootSite };
        if ( queryCond.equals( new BasicBSONObject() ) ) {
            expBSites = new SiteWrapper[] { branchSite };
        }
        ScmFileUtils.checkMetaAndData( wsp, fileIdsA, expASites, localPath,
                filePath );
        ScmFileUtils.checkMetaAndData( wsp, fileIdsB, expBSites, localPath,
                filePath );
        ScmSystem.Schedule.delete( sessionM, sche.getId() );
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
    }

    private void testMaxStayTime() throws Exception {
        ScmScheduleMoveFileContent content = getDefaultContent( queryCond );
        content.setMaxStayTime( "10d" );
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.MOVE_FILE, taskName, "", content,
                "0/10 * * * * ?" );
        ScmScheduleUtils.waitForTask( sche, 2 );
        SiteWrapper[] expSites = { rootSite };
        ScmFileUtils.checkMetaAndData( wsp, fileIdsA, expSites, localPath,
                filePath );
        ScmFileUtils.checkMetaAndData( wsp, fileIdsB, expSites, localPath,
                filePath );
        ScmSystem.Schedule.delete( sessionM, sche.getId() );
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
    }

    private void testNullSite( String sourceSite, String targetSite )
            throws Exception {
        ScmScheduleMoveFileContent content = getDefaultContent( queryCond );
        content.setSourceSiteName( sourceSite );
        content.setTargetSiteName( targetSite );
        try {
            sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                    ScheduleType.MOVE_FILE, taskName, "", content,
                    "0/10 * * * * ?" );
            ScmScheduleUtils.waitForTask( sche, 2 );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.HTTP_NOT_FOUND ) {
                throw e;
            }
        }
    }
}