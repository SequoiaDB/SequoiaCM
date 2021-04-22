package com.sequoiacm.statistics;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @Description: SCM-3602:scm.statistics.types配置项测试
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3602 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3602";
    private ScmId fileId = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // 清理统计表中统计信息
        StatisticsUtils.clearStatisticalInfo();
        // 清理文件
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        // 更新admin-server配置
        Map< String, String > confMap = new HashMap<>();
        confMap.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap );
    }

    @Test
    private void test() throws Exception {
        // 配置项为空串, 暂时忽略该问题
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "" );
        ScmUpdateConfResultSet result1 = ConfUtil
                .updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        Assert.assertEquals( result1.getSuccesses().size() > 0, true );
        Assert.assertEquals( result1.getFailures().size(), 0 );

        // 配置项正确
        confMap1.put( "scm.statistics.types", "file_upload" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize", "10" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 上传文件
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setAuthor( fileName );
        fileId = file.save();

        StatisticsUtils.waitStatisticalInfoCount( 1 );

        // 查询上传接口统计信息
        Date now = new Date();
        ScmFileStatisticInfo statisticInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate(
                        new Date( now.getTime() - 1000 * 60 * 60 * 24 * 3 ) )
                .endDate( new Date( now.getTime() + 1000 * 60 * 60 * 24 ) )
                .workspace( wsp.getName() ).timeAccuracy( ScmTimeAccuracy.DAY )
                .upload().get();
        // 检查结果
        Assert.assertEquals( statisticInfo.getRequestCount(), 1 );
        Assert.assertEquals( statisticInfo.getAvgTrafficSize(), 0 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                StatisticsUtils.clearStatisticalInfo();
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}