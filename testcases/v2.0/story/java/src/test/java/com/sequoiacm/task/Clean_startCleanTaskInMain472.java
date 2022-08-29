package com.sequoiacm.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-472: 清理主中心文件
 * @Author fanyu
 * @Date 2017-06-28
 * @updateUser YiPan
 * @updateDate 2021.9.2
 * @updateRemark 星状主站点不允许创建清理任务约束已取消，适配用例
 * @version 1.1
 */

/*
 * 1、在主中心开始清理任务； 2、检查执行结果；
 */
public class Clean_startCleanTaskInMain472 extends TestScmBase {
    private boolean runSuccess = false;
    private ScmId taskId = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private int fileSize = new Random().nextInt( 1024 ) + 1024;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String authorName = "StartCleanTaskInMain472";
    private ScmSession sessionM = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsM = null;
    private ScmWorkspace wsB = null;
    private SiteWrapper rootSite;
    private SiteWrapper branchSite;
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpPath = localPath + File.separator + "tmpFile_" + fileSize + ".txt";

        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        // login in
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );
        ScmId fileID = ScmFileUtils.create( wsM, authorName, filePath );
        fileIds.add( fileID );

        // 缓存至分站点
        branchSite = ScmInfo.getBranchSite();
        sessionB = TestScmTools.createSession( branchSite );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        ScmFile instance = ScmFactory.File.getInstance( wsB, fileID );
        instance.getContent( tmpPath );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    private void test() throws Exception {
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( authorName ).get();
        taskId = ScmSystem.Task.startCleanTask( wsM, cond );

        SiteWrapper[] expSiteIdList = { branchSite };
        ScmScheduleUtils.checkScmFile( wsM, fileIds, expSiteIdList );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                for ( ScmId id : fileIds ) {
                    ScmFactory.File.deleteInstance( wsM, id, true );
                }
                TestSdbTools.Task.deleteMeta( taskId );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            sessionM.close();
            sessionB.close();
        }
    }
}
