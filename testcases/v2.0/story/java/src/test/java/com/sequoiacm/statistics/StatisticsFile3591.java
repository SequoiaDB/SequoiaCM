package com.sequoiacm.statistics;

import java.util.Calendar;
import java.util.Date;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @Description: SCM-3591:统计表中无统计信息，用户查询上传/下载接口的统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3591 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private Calendar calendar = Calendar.getInstance();
    private Date beginDate = null;
    private Date endDate = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        // 删除统计表中所有统计信息
        StatisticsUtils.clearStatisticalInfo();
    }

    @Test
    private void test() throws Exception {
        calendar.set( Calendar.HOUR_OF_DAY,
                calendar.get( Calendar.HOUR_OF_DAY ) - 10 );
        endDate = calendar.getTime();
        calendar.set( Calendar.HOUR_OF_DAY,
                calendar.get( Calendar.HOUR_OF_DAY ) - 8 );
        beginDate = calendar.getTime();
        // 查询上传接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate ).upload().get();
        // 检查结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, null, null, 0, 0, 0 );
        StatisticsUtils.checkScmFileStatisticInfo( uploadInfo, expUploadInfo );

        // 查询下载接口统计信息
        ScmFileStatisticInfo downloadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate ).download().get();
        // 检查结果
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_DOWNLOAD, beginDate, endDate,
                TestScmBase.scmUserName, null, null, 0, 0, 0 );
        StatisticsUtils.checkScmFileStatisticInfo( downloadInfo,
                expDownloadInfo );
    }

    @AfterClass
    private void tearDown() throws Exception {
        if ( session != null ) {
            session.close();
        }
    }
}