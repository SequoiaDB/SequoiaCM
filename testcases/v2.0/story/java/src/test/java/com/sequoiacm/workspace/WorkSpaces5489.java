package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmCleanTaskConfig;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5489:修改数据源分区规则与文件清理验证
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5489 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private String wsName = "ws5489";
    private String fileName1 = "file5489_1";
    private String fileName2 = "file5489_2";
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath1 = null;
    private String filePath2 = null;
    private File localPath = null;
    private boolean runSuccess = false;
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private ScmShardingType scmShardingType = ScmShardingType.QUARTER;
    private ScmWorkspace ws;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile1_" + fileSize
                + ".txt";
        filePath2 = localPath + File.separator + "localFile2_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize );
        TestTools.LocalFile.createFile( filePath2, fileSize );

        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();
        session = TestScmTools.createSession( rootSite );
        siteList.add( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        fileId1 = uploadFile( fileName1, filePath1 );

        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, scmShardingType );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        createCleanTask( fileName1 );

        SiteWrapper[] expSite = { branSite };
        ScmFileUtils.checkMetaAndData( wsName, fileId1, expSite, localPath,
                filePath1 );

        fileId2 = uploadFile( fileName2, filePath2 );
        createCleanTask( fileName2 );
        ScmFileUtils.checkMetaAndData( wsName, fileId2, expSite, localPath,
                filePath2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public ScmId uploadFile( String fileName, String filePath )
            throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        ScmId scmId = file.save();
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmId taskId = ScmSystem.Task.startTransferTask( ws, queryCond,
                ScmType.ScopeType.SCOPE_CURRENT, branSite.getSiteName() );
        ScmTaskUtils.waitTaskFinish( session, taskId );
        return scmId;
    }

    public void createCleanTask( String fileName ) throws Exception {
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmCleanTaskConfig config = new ScmCleanTaskConfig();
        config.setCondition( queryCond );
        config.setWorkspace( ws );
        ScmId taskId = ScmSystem.Task.startCleanTask( config );
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }
}