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
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5917:修改目标数据源分区规则后，目标站点存在相同lob，迁移文件
 * @author YiPan
 * @date 2023/2/6
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5917 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private String wsName = "ws5917";
    private String fileName = "file5917";
    private ArrayList< SiteWrapper > branchSiteLocation = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath = null;
    private File localPath = null;
    private ScmId fileId = null;
    private ScmShardingType scmShardingType = ScmShardingType.NONE;
    private ScmWorkspace ws;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile1_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        session = TestScmTools.createSession( rootSite );
        branchSiteLocation.add( branchSite );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 主站点创建文件
        fileId = uploadFile( fileName, filePath );

        // 更新分站点工作区
        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( branchSiteLocation, scmShardingType );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        // 目标站点已存在lob文件
        TestSdbTools.Lob.putLob( branchSite, getWrappgeter( wsName ), fileId,
                filePath );

        // 创建迁移调度任务
        createMoveTask( fileName );

        // 校验文件和元数据
        SiteWrapper[] expSite = { rootSite, branchSite };
        ScmFileUtils.checkMetaAndData( wsName, fileId, expSite, localPath,
                filePath );
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            TestTools.LocalFile.removeFile( localPath );
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
        return file.save();
    }

    public void createMoveTask( String fileName ) throws Exception {
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmTransferTaskConfig config = new ScmTransferTaskConfig();
        config.setCondition( queryCond );
        config.setTargetSite( branchSite.getSiteName() );
        config.setWorkspace( ws );
        ScmId taskId = ScmSystem.Task.startTransferTask( config );
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }

    public WsWrapper getWrappgeter( String wsName ) throws ScmException {
        ScmCursor< ScmWorkspaceInfo > cursor = null;
        ScmWorkspaceInfo wsInfo = null;
        try {
            cursor = ScmFactory.Workspace.listWorkspace( session );
            while ( cursor.hasNext() ) {
                ScmWorkspaceInfo info = cursor.getNext();
                if ( info.getName().equals( wsName ) ) {
                    wsInfo = info;
                }
            }
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
        }
        return new WsWrapper( wsInfo );
    }
}