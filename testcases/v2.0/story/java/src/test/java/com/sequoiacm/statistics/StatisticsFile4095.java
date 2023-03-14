package com.sequoiacm.statistics;

import java.io.File;
import java.util.*;

import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;

/**
 * @author ZhangYanan
 * @version v1.0
 * @description SCM-4095:并发上传同一断点文件，查看统计信息
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 */
public class StatisticsFile4095 extends TestScmBase {
    private File localPath = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String fileName = "file4095";
    private ScmSession siteSession = null;
    private ScmWorkspace siteWorkspace = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private List< Integer > uploadTime = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private int fileNums = 1;
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
        siteSession = ScmSessionUtils.createSession( site );
        siteWorkspace = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                siteSession );

        // 更新网关和admin配置
        StatisticsUtils.configureGatewayAndAdminInfo( wsp );
        // 设置统计起始时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 1 );
        beginDate = calendar.getTime();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        ThreadExecutor es = new ThreadExecutor( 180 * 1000 );
        UploadBreakpointFileThread uploadFile1 = new UploadBreakpointFileThread();
        UploadBreakpointFileThread uploadFile2 = new UploadBreakpointFileThread();
        es.addWorker( uploadFile1 );
        es.addWorker( uploadFile2 );
        es.run();
        Assert.assertEquals( uploadFile1.getRetCode(), 0 );
        Assert.assertEquals( uploadFile2.getRetCode(), 0 );

        // 设置查询截止时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 2 );
        endDate = calendar.getTime();
        StatisticsUtils.waitStatisticalInfoCount( fileNums );
        // 取最大响应时间和最小响应时间
        long upMaxTime = Collections.max( uploadTime );
        long upMinTime = Collections.min( uploadTime );

        // 查询upload接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate ).upload().get();
        // 检查结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, null, null, fileNums, fileSize,
                uploadTime.get( 0 ), upMaxTime, upMinTime, 0 );
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

    private class UploadBreakpointFileThread extends ResultStore {
        @ExecuteOrder(step = 1)
        private void UploadFile() throws Exception {
            try ( ScmSession session = ScmSessionUtils.createSession( site )) {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                int createAndUploadBreakpointFileTime = ( int ) ScmBreakpointFileUtils
                        .createAndUploadBreakpointFile( fileName, ws,
                                filePath );
                int breakpointFileToFileTime = ( int ) ScmBreakpointFileUtils
                        .breakpointFileToFile( fileName, ws, fileName,
                                fileIdList );
                uploadTime.add( createAndUploadBreakpointFileTime
                        + breakpointFileToFileTime );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.FILE_EXIST.getErrorCode() ) {
                    throw e;
                }
            }
        }
    }
}