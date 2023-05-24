package com.sequoiacm.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.sequoiacm.client.element.ScmCleanTaskConfig;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.client.element.ScmTransferTaskConfig;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @Descreption SCM-6203:迁移任务startTransferTaskV2接口功能验证
 *              SCM-6204:清理任务startCleanTaskV2接口功能验证
 *              SCM-6205:迁移清理任务startMoveTaskV2接口功能验证
 * @Author zhangYaNan
 * @CreateDate 2023.05.16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version 1.0
 */
public class Task6203_6204_6205 extends TestScmBase {
    private final static int fileNum = 10;
    private final static int fileSize = 100;
    private final static String name = "scheduleTask6200";
    private boolean runSuccess = false;
    private ScmSession ssA = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsM = null;
    private ScmSession ssM = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private ArrayList< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath = null;
    private BSONObject queryCond = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        ssA = ScmSessionUtils.createSession( branSite );
        ssM = ScmSessionUtils.createSession( rootSite );

        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), ssA );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), ssM );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );

        readyScmFile( wsA, 0, fileNum, fileIds );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        SiteWrapper[] expSites1 = { rootSite, branSite };
        SiteWrapper[] expSites2 = { rootSite };
        SiteWrapper[] expSites3 = { branSite };

        // 验证迁移任务
        createCopyTask();
        ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites1 );

        // 验证清理任务
        createCleanTask();
        ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites2 );

        // 验证迁移清理任务
        createMoveTask();
        ScmScheduleUtils.checkScmFile( wsA, fileIds, expSites3 );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFileUtils.cleanFile( wsp, queryCond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }
            if ( ssM != null ) {
                ssM.close();
            }
        }
    }

    private void readyScmFile( ScmWorkspace ws, int startNum, int endNum,
            ArrayList< ScmId > fileIds ) throws ScmException {
        for ( int i = startNum; i < endNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( "file" + name + i );
            file.setAuthor( name );
            file.setContent( filePath );
            fileIds.add( file.save() );
        }

    }

    private void createCopyTask() throws Exception {
        ScmTransferTaskConfig config = new ScmTransferTaskConfig();
        config.setWorkspace( wsA );
        config.setTargetSite( rootSite.getSiteName() );
        config.setCondition( queryCond );
        config.setScope( ScmType.ScopeType.SCOPE_CURRENT );
        ScmId scmId = ScmSystem.Task.startTransferTaskV2( config );
        ScmTaskUtils.waitTaskFinish( ssA, scmId );
    }

    private void createCleanTask() throws Exception {
        ScmCleanTaskConfig config = new ScmCleanTaskConfig();
        config.setWorkspace( wsA );
        config.setCondition( queryCond );
        config.setScope( ScmType.ScopeType.SCOPE_CURRENT );
        ScmId scmId = ScmSystem.Task.startCleanTaskV2( config );
        ScmTaskUtils.waitTaskFinish( ssA, scmId );
    }

    private void createMoveTask() throws Exception {
        ScmMoveTaskConfig config = new ScmMoveTaskConfig();
        config.setWorkspace( wsM );
        config.setTargetSite( branSite.getSiteName() );
        config.setCondition( queryCond );
        config.setScope( ScmType.ScopeType.SCOPE_CURRENT );
        ScmId scmId = ScmSystem.Task.startMoveTaskV2( config );
        ScmTaskUtils.waitTaskFinish( ssA, scmId );
    }
}