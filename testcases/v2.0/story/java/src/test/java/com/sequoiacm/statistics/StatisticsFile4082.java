package com.sequoiacm.statistics;

import java.io.File;
import java.util.*;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @description SCM-4082:上传断点文件所有步骤成功，查询统计信息
 * @author ZhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class StatisticsFile4082 extends TestScmBase {
    private File localPath = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String fileName = "file4082";
    private ScmSession siteSession = null;
    private ScmWorkspace siteWorkspace = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private int fileNums = 1;
    private List< Integer > uploadTime = new ArrayList<>();
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = null;
    private String filePath = null;

    @BeforeClass
    public void setUp() throws Exception {
        calendar = Calendar.getInstance();
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils
                .checkDBAndCephS3DataSource();

        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        siteSession = TestScmTools.createSession( site );
        siteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                siteSession );
        // 更新网关和admin配置
        ScmFileStatisticsType statisticType = ScmFileStatisticsType.FILE_UPLOAD;
        StatisticsUtils.configureGatewayAndAdminInfo( wsp, statisticType );
        // 设置统计起始时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 1 );
        beginDate = calendar.getTime();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        int createAndUploadBreakpointFileTime = ( int ) StatisticsUtils
                .createAndUploadBreakpointFile( fileName, siteWorkspace,
                        filePath );
        int breakpointFileToFileTime = ( int ) StatisticsUtils
                .breakpointFileToFile( fileName, siteWorkspace, fileName,
                        fileIdList );
        uploadTime.add(
                createAndUploadBreakpointFileTime + breakpointFileToFileTime );
        StatisticsUtils.waitStatisticalInfoCount( fileNums );

        // 设定统计结束时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 2 );
        endDate = calendar.getTime();

        // 取最大响应时间和最小响应时间
        long maxTime = Collections.max( uploadTime );
        long minTime = Collections.min( uploadTime );
        long totalTime = 0;
        for ( long time : uploadTime ) {
            totalTime += time;
        }

        // 查询上传接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).workspace( wsp.getName() )
                .endDate( endDate ).upload().get();
        // 预期结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), null, fileNums,
                fileSize / fileNums, totalTime / fileNums, maxTime, minTime,
                0 );
        StatisticsUtils.checkScmFileStatisticInfo( uploadInfo, expUploadInfo );
        StatisticsUtils.checkScmFileStatisticNewAddInfo( uploadInfo,
                expUploadInfo );
        runSuccess = true;
    }

    @AfterClass()
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( siteWorkspace, fileId,
                            true );
                }
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                ConfUtil.deleteGateWayStatisticalConf();
                if ( siteSession != null ) {
                    siteSession.close();
                }
            }
        }
    }
}
