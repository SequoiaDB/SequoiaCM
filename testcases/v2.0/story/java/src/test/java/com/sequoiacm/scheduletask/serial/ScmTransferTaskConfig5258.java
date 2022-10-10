package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileCondition;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTransferTaskConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Descreption SCM-5258:使用ScmTransferTaskConfig创建迁移任务，驱动测试
 * @Author YiPan
 * @CreateDate 2022/9/27
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class ScmTransferTaskConfig5258 extends TestScmBase {
    private String fileName = "file5258_";
    private String fileTitleA = "titleA5258";
    private String fileTitleB = "titleB5258";
    private String fileAuthor = "author5258";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private List< ScmId > fileIdsA = new ArrayList<>();
    private List< ScmId > fileIdsB = new ArrayList<>();
    private ScmSession sessionM = null;
    private long now;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws IOException, ScmException {
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
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        createFile( wsM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // a.源站点为空
        testNullSite( null, branchSite.getSiteName() );

        // b.目标站点为空
        testNullSite( wsM, "" );

        // d.设置bson条件为空
        testBson( new BasicBSONObject() );

        // e.设置bson条件待匹配条件
        createFile( wsM );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.TITLE )
                .in( fileTitleA ).get();
        testBson( cond );

        // f.设置ScmFileCondition为空
        createFile( wsM );
        testFileCondition( new ScmFileCondition() );

        // g.设置ScmFileCondition不为空,匹配A部分文件迁移
        createFile( wsM );
        ScmFileCondition fileCondition = new ScmFileCondition();
        fileCondition.setFileBeginningTime( new Date( now ) );
        fileCondition.setFileEndingTime( new Date( now + fileNum ) );
        testFileCondition( fileCondition );

        // h.测试互相覆盖
        testOverwrite();

        // f.获取对象所有属性校验
        testGetConfig();

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

    private void testOverwrite() throws ScmException {
        ScmTransferTaskConfig config = getDefaultConfig( queryCond );
        ScmFileCondition fileCondition = new ScmFileCondition();
        fileCondition.setFileBeginningTime( new Date( now ) );
        fileCondition.setFileEndingTime( new Date( now + fileNum ) );
        config.setCondition( fileCondition );
        BSONObject actBson = config.getCondition();
        BasicBSONObject value = new BasicBSONObject();
        value.put( "$gte", now );
        value.put( "$lt", now + fileNum );
        BSONObject expBson = ScmQueryBuilder
                .start( ScmAttributeName.File.CREATE_TIME ).is( value ).get();
        Assert.assertEquals( actBson, expBson );
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
                file.setCreateTime( new Date( now + i ) );
                fileIdsA.add( file.save() );
            } else {
                file.setTitle( fileTitleB );
                file.setCreateTime( new Date( now - i ) );
                fileIdsB.add( file.save() );
            }
        }
    }

    private ScmTransferTaskConfig getDefaultConfig( BSONObject moveCond ) {
        ScmTransferTaskConfig config = new ScmTransferTaskConfig();
        config.setWorkspace( wsM );
        config.setTargetSite( branchSite.getSiteName() );
        config.setCondition( moveCond );
        return config;
    }

    private void testGetConfig() {
        ScmTransferTaskConfig config = getDefaultConfig( queryCond );
        Assert.assertEquals( config.getWorkspace(), wsM );
        Assert.assertEquals( config.getTargetSite(), branchSite.getSiteName() );
        Assert.assertEquals( config.getCondition(), queryCond );
        Assert.assertEquals( config.getMaxExecTime(), 0 );
        Assert.assertEquals( config.getDataCheckLevel(),
                ScmDataCheckLevel.WEEK );
    }

    private void testBson( BSONObject queryCond ) throws Exception {
        ScmTransferTaskConfig config = getDefaultConfig( queryCond );
        ScmId taskId = ScmSystem.Task.startTransferTask( config );
        ScmTaskUtils.waitTaskFinish( sessionM, taskId );
        SiteWrapper[] expASites = { rootSite, branchSite };
        SiteWrapper[] expBSites = { rootSite };
        if ( queryCond.equals( new BasicBSONObject() ) ) {
            expBSites = new SiteWrapper[] { rootSite, branchSite };
        }
        ScmFileUtils.checkMetaAndData( wsp, fileIdsA, expASites, localPath,
                filePath );
        ScmFileUtils.checkMetaAndData( wsp, fileIdsB, expBSites, localPath,
                filePath );
    }

    private void testFileCondition( ScmFileCondition cond ) throws Exception {
        ScmTransferTaskConfig config = getDefaultConfig( queryCond );
        config.setCondition( cond );
        ScmId taskId = ScmSystem.Task.startTransferTask( config );
        ScmTaskUtils.waitTaskFinish( sessionM, taskId );
        SiteWrapper[] expASites = { rootSite, branchSite };
        SiteWrapper[] expBSites = { rootSite };
        if ( cond.getFileBeginningTime() == null
                && cond.getFileEndingTime() == null ) {
            expBSites = new SiteWrapper[] { rootSite, branchSite };
        }
        ScmFileUtils.checkMetaAndData( wsp, fileIdsA, expASites, localPath,
                filePath );
        ScmFileUtils.checkMetaAndData( wsp, fileIdsB, expBSites, localPath,
                filePath );
    }

    private void testNullSite( ScmWorkspace ws, String targetSite )
            throws Exception {
        ScmTransferTaskConfig config = getDefaultConfig( queryCond );
        config.setWorkspace( ws );
        config.setTargetSite( targetSite );
        try {
            ScmSystem.Task.startTransferTask( config );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( ws == null ) {
                Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT,
                        e.getMessage() );
            } else if ( targetSite == "" ) {
                Assert.assertEquals( e.getError(), ScmError.SITE_NOT_EXIST,
                        e.getMessage() );
            } else {
                throw e;
            }
        }
    }
}