package com.sequoiacm.statistics;

import java.io.File;
import java.util.*;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @Description: SCM-3642:使用删除配置接口删除配置，检查动态生效
 * @author YiPan
 * @Date:2021/05/8
 * @version:1.0
 */
public class StatisticsFile3642 extends TestScmBase {
    private ScmSession session;
    private String fileName = "file3642";
    private ScmWorkspace ws1;
    private ScmWorkspace ws2;
    private ScmWorkspace ws3;
    private String ws1Name = "ws1_3642";
    private String ws2Name = "ws2_3642";
    private String ws3Name = "ws3_3642";
    private ScmId file1Id;
    private ScmId file2Id;
    private ScmId file3Id;
    private ScmFileStatisticInfo scmFileStatisticInfo1;
    private ScmFileStatisticInfo scmFileStatisticInfo2;
    private ScmFileStatisticInfo scmFileStatisticInfo3;
    private Set< String > keySet = new HashSet<>();
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 200 * 1024;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        session = TestScmTools.createSession( ScmInfo.getSite() );
        ScmWorkspaceUtil.deleteWs( ws1Name, session );
        ScmWorkspaceUtil.deleteWs( ws2Name, session );
        ScmWorkspaceUtil.deleteWs( ws3Name, session );
        ScmWorkspaceUtil.createWS( session, ws1Name, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.createWS( session, ws2Name, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.createWS( session, ws3Name, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, ws1Name );
        ScmWorkspaceUtil.wsSetPriority( session, ws2Name );
        ScmWorkspaceUtil.wsSetPriority( session, ws3Name );
        ws1 = ScmFactory.Workspace.getWorkspace( ws1Name, session );
        ws2 = ScmFactory.Workspace.getWorkspace( ws2Name, session );
        ws3 = ScmFactory.Workspace.getWorkspace( ws3Name, session );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        // 初始环境
        keySet.add( "scm.statistics.types" );
        keySet.add( "scm.statistics.types.file_upload.conditions.workspaces" );
        ConfUtil.deleteConf( session, ConfUtil.GATEWAY_SERVICE_NAME, keySet );

        // 配置统计ws1和ws2的上传信息
        Map< String, String > confMap = new HashMap<>();
        confMap.put( "scm.statistics.types", "file_upload" );
        confMap.put( "scm.statistics.types.file_upload.conditions.workspaces",
                ws1Name + "," + ws2Name );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap );
        StatisticsUtils.clearStatisticalInfo();

        // ws1,ws2,ws3上传文件
        uploadfile();
        StatisticsUtils.waitStatisticalInfoCount( 2 );

        // 查询统计信息校验,ws1和ws2有统计信息
        statistics();
        Assert.assertEquals( scmFileStatisticInfo1.getRequestCount(), 1 );
        Assert.assertEquals( scmFileStatisticInfo2.getRequestCount(), 1 );
        Assert.assertEquals( scmFileStatisticInfo3.getRequestCount(), 0 );
        ScmFactory.File.deleteInstance( ws1, file1Id, true );
        ScmFactory.File.deleteInstance( ws2, file2Id, true );
        ScmFactory.File.deleteInstance( ws3, file3Id, true );

        // 删除配置项动态生效
        ConfUtil.deleteConf( session, ConfUtil.GATEWAY_SERVICE_NAME, keySet );
        StatisticsUtils.clearStatisticalInfo();

        // 上传文件
        uploadfile();

        // 查询统计信息校验,ws1,ws2,ws3都没有统计信息
        statistics();
        Assert.assertEquals( scmFileStatisticInfo1.getRequestCount(), 0 );
        Assert.assertEquals( scmFileStatisticInfo2.getRequestCount(), 0 );
        Assert.assertEquals( scmFileStatisticInfo3.getRequestCount(), 0 );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            TestTools.LocalFile.removeFile( localPath );
            ScmWorkspaceUtil.deleteWs( ws1Name, session );
            ScmWorkspaceUtil.deleteWs( ws2Name, session );
            ScmWorkspaceUtil.deleteWs( ws3Name, session );
        } finally {
            ConfUtil.deleteConf( session, ConfUtil.GATEWAY_SERVICE_NAME,
                    keySet );
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void statistics() throws ScmException {
        Date begin = new Date( new Date().getTime() - 1000 * 60 * 60 * 24 * 3 );
        Date end = new Date( new Date().getTime() + 1000 * 60 * 60 * 24 );
        // 获取统计信息
        scmFileStatisticInfo1 = ScmSystem.Statistics.fileStatistician( session )
                .user( TestScmBase.scmUserName ).beginDate( begin )
                .endDate( end ).workspace( ws1Name ).upload().get();
        scmFileStatisticInfo2 = ScmSystem.Statistics.fileStatistician( session )
                .user( TestScmBase.scmUserName ).beginDate( begin )
                .endDate( end ).workspace( ws2Name ).upload().get();
        scmFileStatisticInfo3 = ScmSystem.Statistics.fileStatistician( session )
                .user( TestScmBase.scmUserName ).beginDate( begin )
                .endDate( end ).workspace( ws3Name ).upload().get();
    }

    private void uploadfile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws1 );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setContent( filePath );
        file1Id = file.save();
        file = ScmFactory.File.createInstance( ws2 );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setContent( filePath );
        file2Id = file.save();
        file = ScmFactory.File.createInstance( ws3 );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        file.setContent( filePath );
        file3Id = file.save();
    }
}