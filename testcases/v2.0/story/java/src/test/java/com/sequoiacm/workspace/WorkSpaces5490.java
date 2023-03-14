package com.sequoiacm.workspace;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sequoiacm.client.element.*;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmSpaceRecycleScope;
import com.sequoiacm.client.element.ScmSpaceRecyclingTaskConfig;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5490:修改数据源分区规则与空间回收任务验证
 * @author ZhangYanan
 * @date 2022/11/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5490 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private String wsName = "ws5490";
    private String fileName1 = "file5490_1";
    private String fileName2 = "file5490_2";
    private static SimpleDateFormat yearFm = new SimpleDateFormat( "yyyy" );
    private static SimpleDateFormat monthFm = new SimpleDateFormat( "MM" );
    private ArrayList< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private String filePath1 = null;
    private String filePath2 = null;
    private File localPath = null;
    private boolean runSuccess = false;
    private String csName1 = null;
    private String csName2 = null;
    private ScmShardingType scmShardingType = ScmShardingType.NONE;
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
        session = ScmSessionUtils.createSession( rootSite );
        siteList.add( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );
    }

    // 问题单SEQUOIACM-1168未修改，用例暂时屏蔽
    @Test(groups = { "twoSite", "fourSite" }, enabled = false)
    public void test() throws Exception {
        csName1 = prepareLobCS( fileName1, filePath1, ScmShardingType.YEAR );

        List< ScmDataLocation > dataLocation = ScmWorkspaceUtil
                .prepareWsDataLocation( siteList, scmShardingType );
        ws.updateDataLocation( dataLocation );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );

        createMoveTask();
        boolean isExits1 = ScmScheduleUtils.checkLobCS( rootSite, csName1 );
        Assert.assertFalse( isExits1 );

        createMoveTask();
        csName2 = prepareLobCS( fileName2, filePath2, scmShardingType );
        boolean isExits2 = ScmScheduleUtils.checkLobCS( rootSite, csName2 );
        Assert.assertFalse( isExits2 );
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

    public String prepareLobCS( String fileName, String filePath,
            ScmShardingType scmShardingType )
            throws ScmException, ParseException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.save();
        file.delete( true );
        String csName = wsName + "_LOB_"
                + getCsClPostfix( new Date(), scmShardingType );
        return csName;
    }

    public static String getCsClPostfix( Date currTime,
            ScmShardingType shardType ) throws ParseException {
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

    public void createMoveTask() throws Exception {
        ScmSpaceRecyclingTaskConfig config = new ScmSpaceRecyclingTaskConfig();
        config.setWorkspace( ws );
        config.setRecycleScope( ScmSpaceRecycleScope.mothBefore( 2 ) );
        config.setMaxExecTime( 120000 );

        ScmId taskId = ScmSystem.Task.startSpaceRecyclingTask( config );
        ScmTaskUtils.waitTaskFinish( session, taskId );
    }
}