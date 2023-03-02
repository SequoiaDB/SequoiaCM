package com.sequoiacm.workspace.serial;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmMoveTaskConfig;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5487:修改数据源分区规则与文件迁移清理验证
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5487 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private String wsName = "ws5487";
    private String fileName1 = "file5487_1";
    private String fileName2 = "file5487_2";
    private String fileName3 = "file5487_3";
    private String fileName4 = "file5487_4";
    private static SimpleDateFormat yearFm = new SimpleDateFormat( "yyyy" );
    private static SimpleDateFormat monthFm = new SimpleDateFormat( "MM" );
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath1 = null;
    private String filePath2 = null;
    private File localPath = null;
    private boolean runSuccess = false;
    private ScmShardingType scmShardingType = ScmShardingType.YEAR;
    private ScmShardingType newScmShardingType = ScmShardingType.MONTH;
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
        ScmId fileId1 = uploadFile( fileName1, filePath1 );
        ScmId fileId2 = uploadFile( fileName2, filePath2 );

        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, scmShardingType );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        createMoveTask( fileName1, false, ScmShardingType.YEAR );
        createMoveTask( fileName2, true, ScmShardingType.YEAR );
        SiteWrapper[] expSite = { branSite };
        ScmFileUtils.checkMetaAndData( wsName, fileId1, expSite, localPath,
                filePath1 );
        ScmFileUtils.checkMetaAndData( wsName, fileId2, expSite, localPath,
                filePath2 );

        ScmId fileId3 = uploadFile( fileName3, filePath1 );
        ScmId fileId4 = uploadFile( fileName4, filePath2 );

        createMoveTask( fileName3, false, scmShardingType );
        createMoveTask( fileName4, true, scmShardingType );
        ScmFileUtils.checkMetaAndData( wsName, fileId3, expSite, localPath,
                filePath1 );
        ScmFileUtils.checkMetaAndData( wsName, fileId4, expSite, localPath,
                filePath2 );

        // 修改分区规则后再次验证
        dataLocation = ScmWorkspaceUtil.prepareWsDataLocation( siteList,
                newScmShardingType );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        ScmFileUtils.checkMetaAndData( wsName, fileId3, expSite, localPath,
                filePath1 );
        ScmFileUtils.checkMetaAndData( wsName, fileId4, expSite, localPath,
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
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        return file.save();
    }

    public void createMoveTask( String fileName, Boolean isRecycleSpace,
            ScmShardingType scmShardingType ) throws Exception {
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmMoveTaskConfig config = new ScmMoveTaskConfig();
        config.setRecycleSpace( isRecycleSpace );
        config.setCondition( queryCond );
        config.setTargetSite( branSite.getSiteName() );
        config.setWorkspace( ws );
        ScmId taskId = ScmSystem.Task.startMoveTask( config );
        ScmTaskUtils.waitTaskFinish( session, taskId );

        // 校验空间回收
        if ( isRecycleSpace ) {
            String csName = wsName + "_LOB_"
                    + getCsClPostfix( new Date(), scmShardingType );
            boolean isExits1 = ScmScheduleUtils.checkLobCS( rootSite, csName );
            Assert.assertFalse( isExits1 );
        }
    }

    public static String getCsClPostfix( Date currTime,
            ScmShardingType shardType ) {
        String currY = yearFm.format( currTime );
        String currM = monthFm.format( currTime );
        String postfix = null;
        if ( shardType.equals( ScmShardingType.NONE ) ) {
            postfix = "";
        } else if ( shardType.equals( ScmShardingType.YEAR ) ) {
            postfix = currY;
        } else if ( shardType.equals( ScmShardingType.QUARTER ) ) {
            int quarter = ( int ) Math.ceil( Double.parseDouble( currM ) / 3 );
            postfix = currY + "Q" + quarter;
        } else if ( shardType.equals( ScmShardingType.MONTH ) ) {
            postfix = currY + currM;
        }
        return postfix;
    }
}