package com.sequoiacm.statistics;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.sequoiacm.client.exception.ScmException;
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
 * @Description: SCM-3603:scm.statistics.types.xx-type.conditions.workspaces配置项测试
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3603 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3603";
    private List< ScmId > fileIdList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // 清理文件
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        ConfUtil.deleteGateWayStatisticalConf();
        // 更新admin-server配置
        Map< String, String > confMap = new HashMap<>();
        confMap.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap );
    }

    @Test
    private void test() throws Exception {
        // 配置项为空串 跟开发确认，配置为空串会有问题，该问题没有办法修改
        // 因此暂时注释掉相关测试点
        Map< String, String > confMap1 = new HashMap<>();
        // confMap1.put( "scm.statistics.types", "file_upload" );
        // confMap1.put(
        // "scm.statistics.types.file_upload.conditions.workspaces",
        // "" );
        // confMap1.put( "scm.statistics.rawDataCacheSize", "10" );
        // confMap1.put( "scm.statistics.rawDataReportPeriod", "1" );
        // ScmUpdateConfResultSet result1 = ConfUtil.updateConf(
        // ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        // Assert.assertEquals( result1.getSuccesses().size() > 0, true );
        // Assert.assertEquals( result1.getFailures().size(), 0 );
        //
        // // 上传文件
        // fileIdList.add( createFile() );
        // queryStatisticInfoAndCheck( 0 );

        // 配置项正确
        confMap1.put( "scm.statistics.types", "file_upload" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
        ScmUpdateConfResultSet result3 = ConfUtil
                .updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );
        Assert.assertEquals( result3.getSuccesses().size() > 0, true );
        Assert.assertEquals( result3.getFailures().size(), 0 );

        // 上传文件
        fileIdList.add( createFile() );
        StatisticsUtils.waitStatisticalInfoCount( 1 );
        queryStatisticInfoAndCheck( 1 );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
            }
        } finally {
            ConfUtil.deleteGateWayStatisticalConf();
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void queryStatisticInfoAndCheck( long requestCount )
            throws ScmException {
        // 查询上传接口统计信息
        Date now = new Date();
        ScmFileStatisticInfo statisticInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( new Date( now.getTime() - 1000 * 60 * 60 * 24 ) )
                .endDate( new Date( now.getTime() + 1000 * 60 * 60 * 24 * 3 ) )
                .workspace( wsp.getName() ).timeAccuracy( ScmTimeAccuracy.DAY )
                .upload().get();
        // 检查结果
        Assert.assertEquals( statisticInfo.getRequestCount(), requestCount );
        Assert.assertEquals( statisticInfo.getAvgTrafficSize(), 0 );
    }

    private ScmId createFile() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName + "_" + UUID.randomUUID() );
        file.setAuthor( fileName );
        return file.save();
    }
}