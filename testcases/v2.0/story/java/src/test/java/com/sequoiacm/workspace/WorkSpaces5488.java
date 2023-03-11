package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTransferTaskConfig;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5488:修改数据源分区规则与文件迁移验证
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5488 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private String wsName = "ws5488";
    private String fileName1 = "file5488_1";
    private String fileName2 = "file5488_2";
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath1 = null;
    private String filePath2 = null;
    private File localPath = null;
    private boolean runSuccess = false;
    private ScmId fileId1 = null;
    private ScmId fileId2 = null;
    private ScmShardingType scmShardingType = ScmShardingType.NONE;
    private ScmShardingType newScmShardingType = ScmShardingType.QUARTER;
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

        createTransferTask( fileName1 );

        SiteWrapper[] expSite = { rootSite, branSite };
        ScmFileUtils.checkMetaAndData( wsName, fileId1, expSite, localPath,
                filePath1 );

        fileId2 = uploadFile( fileName2, filePath2 );
        createTransferTask( fileName2 );
        ScmFileUtils.checkMetaAndData( wsName, fileId2, expSite, localPath,
                filePath2 );

        // 修改源站点和目标站点分区规则后再次验证
        siteList.add( branSite );
        dataLocation = ScmWorkspaceUtil.prepareWsDataLocation( siteList,
                newScmShardingType );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        ScmFileUtils.checkMetaAndData( wsName, fileId1, expSite, localPath,
                filePath1 );
        ScmFileUtils.checkMetaAndData( wsName, fileId2, expSite, localPath,
                filePath2 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public ScmId uploadFile( String fileName, String filePath )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        ScmId scmId = file.save();
        return scmId;
    }

    public void createTransferTask( String fileName ) throws Exception {
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmTransferTaskConfig config = new ScmTransferTaskConfig();
        config.setCondition( queryCond );
        config.setTargetSite( branSite.getSiteName() );
        config.setWorkspace( ws );
        ScmId taskId = ScmSystem.Task.startTransferTask( config );
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }
}