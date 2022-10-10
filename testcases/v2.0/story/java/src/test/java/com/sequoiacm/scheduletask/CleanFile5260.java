package com.sequoiacm.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmDataCheckLevel;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-5260:创建清理任务，设置强一致检测
 * @Author YiPan
 * @CreateDate 2022/9/26
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class CleanFile5260 extends TestScmBase {
    private String fileName = "file5260_";
    private String taskName = "task5260";
    private String fileAuthor = "author5260";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private ScmSession sessionM = null;
    private ScmSession sessionB = null;
    private WsWrapper wsp;
    private ScmWorkspace wsM;
    private ScmWorkspace wsB;
    private BSONObject queryCond;
    private ScmSchedule sche;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String lobPath = null;
    private String filePath = null;
    private int fileNum = 10;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        lobPath = localPath + File.separator + "lob" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( lobPath, fileSize );

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
        createFile();
        cacheFileToRootSite();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 创建清理任务对象
        ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                rootSite.getSiteName(), "0d", queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, 1200000,
                ScmDataCheckLevel.STRICT, false, false );

        // 启动清理调度任务，清理主站点
        String cron = "0/10 * * * * ?";
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.CLEAN_FILE, taskName, "", content, cron );

        // 校验任务执行结果
        ScmScheduleUtils.waitForTask( sche, 2 );
        SiteWrapper[] expSites = { branchSite };
        ScmFileUtils.checkMetaAndData( wsp, fileIds, expSites, localPath,
                filePath );
        sche.disable();
        ScmScheduleUtils.cleanTask( sessionM, sche.getId() );

        // 修改分站点文件的lob大小一致，md5不一致
        cacheFileToRootSite();
        updateFileLob();

        // 再次执行任务
        sche.enable();
        ScmScheduleUtils.waitForTask( sche, 2 );
        expSites = new SiteWrapper[] { rootSite, branchSite };
        ScmFileUtils.checkMeta( wsM, fileIds, expSites );
        ScmTask task = ScmScheduleUtils.getFirstTask( sche );
        Assert.assertEquals( task.getEstimateCount(), fileNum );
        Assert.assertEquals( task.getActualCount(), fileNum );
        Assert.assertEquals( task.getSuccessCount(), 0 );
        Assert.assertEquals( task.getFailCount(), fileNum );
        Assert.assertEquals( task.getProgress(), 100 );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
                ScmSystem.Schedule.delete( sessionM, sche.getId() );
                ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
            } finally {
                sessionM.close();
                sessionB.close();
            }
        }
    }

    private void updateFileLob() throws Exception {
        for ( int i = 0; i < fileIds.size(); i++ ) {
            TestSdbTools.Lob.removeLob( branchSite, wsp, fileIds.get( i ) );
            TestSdbTools.Lob.putLob( branchSite, wsp, fileIds.get( i ),
                    lobPath );
        }
    }

    private void createFile() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsB );
            file.setFileName( fileName + i );
            file.setAuthor( fileAuthor );
            file.setContent( filePath );
            fileIds.add( file.save() );
        }
        // 降低时间不同步的敏感度
        Thread.sleep( 500 );
    }

    private void cacheFileToRootSite() throws Exception {
        ScmId taskId = ScmSystem.Task.startTransferTask( wsB, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, rootSite.getSiteName() );
        ScmTaskUtils.waitTaskFinish( sessionB, taskId );
    }
}