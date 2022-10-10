package com.sequoiacm.scheduletask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @Descreption SCM-5219:迁移并清理任务网络模型差异测试
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5219 extends TestScmBase {
    private String fileName = "file5219_";
    private SiteWrapper branchSite1 = null;
    private SiteWrapper branchSite2 = null;
    private ScmSession session = null;
    private WsWrapper wsp;
    private ScmWorkspace ws;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private ScmId fileId;
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
        List< SiteWrapper > branchSites = ScmInfo.getBranchSites( 2 );
        branchSite1 = branchSites.get( 0 );
        branchSite2 = branchSites.get( 1 );
        session = TestScmTools.createSession( branchSite1 );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, queryCond );
        createFile();
    }

    @Test(groups = { "fourSite", GroupTags.star })
    public void starTest() throws Exception {
        ScmMoveTaskConfig scmMoveTaskConfig = new ScmMoveTaskConfig();
        scmMoveTaskConfig.setCondition( queryCond );
        scmMoveTaskConfig.setTargetSite( branchSite2.getSiteName() );
        scmMoveTaskConfig.setWorkspace( ws );
        try {
            ScmSystem.Task.startMoveTask( scmMoveTaskConfig );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.OPERATION_UNSUPPORTED ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @Test(groups = { "fourSite", GroupTags.net })
    public void netTest() throws Exception {
        ScmMoveTaskConfig scmMoveTaskConfig = new ScmMoveTaskConfig();
        scmMoveTaskConfig.setCondition( queryCond );
        scmMoveTaskConfig.setTargetSite( branchSite2.getSiteName() );
        scmMoveTaskConfig.setWorkspace( ws );
        ScmId task = ScmSystem.Task.startMoveTask( scmMoveTaskConfig );

        ScmTaskUtils.waitTaskFinish( session, task );
        SiteWrapper expSites[] = { branchSite2 };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmFileUtils.cleanFile( wsp, queryCond );
            } finally {
                session.close();
            }
        }
    }

    private void createFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( filePath );
        fileId = file.save();
    }
}