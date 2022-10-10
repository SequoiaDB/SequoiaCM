package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.*;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Descreption SCM-5265:使用ScmCleanTaskConfig创建清理任务，驱动测试
 * @Author YiPan
 * @CreateDate 2022/9/27
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class ScmCleanTaskConfig5265 extends TestScmBase {
    private String fileName = "file5265_";
    private String fileTitleA = "titleA5265";
    private String fileTitleB = "titleB5265";
    private String fileAuthor = "author5265";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private long now;
    private List< ScmId > fileIdsA = new ArrayList<>();
    private List< ScmId > fileIdsB = new ArrayList<>();
    private ScmSession sessionM = null;
    private ScmSession sessionB = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private ScmWorkspace wsB;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        now = System.currentTimeMillis();
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
        sessionB = TestScmTools.createSession( branchSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // a.b.h.测试默认参数
        testDefaultOption();

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

        // e.测试ScmFileCondition为空
        cacheToBranchSite();
        testFileCondition( new ScmFileCondition() );

        // f.测试ScmFileCondition设置时间条件
        cacheToBranchSite();
        ScmFileCondition fileCondition = new ScmFileCondition();
        fileCondition.setFileBeginningTime( new Date( now ) );
        fileCondition.setFileEndingTime( new Date( now + fileNum ) );
        testFileCondition( new ScmFileCondition() );
        runSuccess = true;
    }

    private void testGetOption() {
        ScmCleanTaskConfig config = new ScmCleanTaskConfig();
        config.setCondition( queryCond );
        config.setWorkspace( wsM );
        config.setRecycleSpace( true );
        config.setQuickStart( true );
        config.setMaxExecTime( 120000 );
        config.setScope( ScmType.ScopeType.SCOPE_ALL );
        config.setDataCheckLevel( ScmDataCheckLevel.STRICT );
        Assert.assertEquals( config.getWorkspace(), wsM );
        Assert.assertEquals( config.getCondition(), queryCond );
        Assert.assertTrue( config.isRecycleSpace() );
        Assert.assertTrue( config.isQuickStart() );
        Assert.assertEquals( config.getScope(), ScmType.ScopeType.SCOPE_ALL );
        Assert.assertEquals( config.getMaxExecTime(), 120000 );
        Assert.assertEquals( config.getDataCheckLevel(),
                ScmDataCheckLevel.STRICT );
    }

    private void testDefaultOption() {
        ScmCleanTaskConfig config = new ScmCleanTaskConfig();
        Assert.assertEquals( config.getDataCheckLevel(),
                ScmDataCheckLevel.WEEK );
        Assert.assertFalse( config.isRecycleSpace() );
        Assert.assertFalse( config.isQuickStart() );
    }

    private void testBson( BSONObject queryCond ) throws Exception {
        ScmCleanTaskConfig config = new ScmCleanTaskConfig();
        config.setWorkspace( wsB );
        config.setCondition( queryCond );
        ScmId taskId = ScmSystem.Task.startCleanTask( config );
        ScmTaskUtils.waitTaskFinish( sessionM, taskId );
        SiteWrapper[] expASites = { rootSite };
        SiteWrapper[] expBSites = { rootSite, branchSite };
        if ( queryCond.equals( new BasicBSONObject() ) ) {
            expBSites = new SiteWrapper[] { rootSite };
        }
        ScmFileUtils.checkMetaAndData( wsp, fileIdsA, expASites, localPath,
                filePath );
        ScmFileUtils.checkMetaAndData( wsp, fileIdsB, expBSites, localPath,
                filePath );
    }

    private void testFileCondition( ScmFileCondition cond ) throws Exception {
        ScmCleanTaskConfig config = new ScmCleanTaskConfig();
        config.setWorkspace( wsB );
        config.setCondition( queryCond );
        ScmId taskId = ScmSystem.Task.startCleanTask( config );
        ScmTaskUtils.waitTaskFinish( sessionM, taskId );
        SiteWrapper[] expASites = { rootSite };
        SiteWrapper[] expBSites = { rootSite, branchSite };
        if ( cond.getFileBeginningTime() == null
                && cond.getFileEndingTime() == null ) {
            expBSites = new SiteWrapper[] { rootSite };
        }
        ScmFileUtils.checkMetaAndData( wsp, fileIdsA, expASites, localPath,
                filePath );
        ScmFileUtils.checkMetaAndData( wsp, fileIdsB, expBSites, localPath,
                filePath );
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
            } finally {
                sessionM.close();
                sessionB.close();
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
                file.setCreateTime( new Date( now + i ) );
                fileIdsA.add( file.save() );
            } else {
                file.setTitle( fileTitleB );
                file.setCreateTime( new Date( now - i ) );
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