package com.sequoiacm.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.statistics.common.ScmTimeAccuracy;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @Description: SCM-3594:上传文件失败或其它非上传接口操作，查询上传接口的统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3594 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file3594";
    private ScmId fileId = null;
    private int fileSize = 701 * 1024;
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = null;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        calendar = Calendar.getInstance();
        List<SiteWrapper> siteList = ScmInfo.getAllSites();
        for ( SiteWrapper siteWrapper : siteList ) {
            if ( siteWrapper.getDataType()
                    .equals( ScmType.DatasourceType.SEQUOIADB ) ) {
                site = siteWrapper;
                break;
            }
        }
        if ( site == null ) {
            throw new SkipException( "需要db数据源，跳过此用例！！！" );
        }
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        session = TestScmTools.createSession( site );
        wsp = ScmInfo.getWs();
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        // 清理环境和更新配置
        prepareEnv();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // 挑选部分相关的接口进行测试
        createBreakpointFile();
        updateFile();
        crdDir();
        ScmFactory.File.deleteInstance( ws, fileId, true );
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 10 );
        beginDate = calendar.getTime();
        StatisticsUtils.waitStatisticalInfoCount( 0 );
        // 查询上传接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate )
                .workspace( wsp.getName() ).timeAccuracy( ScmTimeAccuracy.DAY )
                .upload().get();
        // 检查结果
        ScmFileStatisticInfo expDownloadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), ScmTimeAccuracy.DAY, 0,
                0, 0 );
        StatisticsUtils.checkScmFileStatisticInfo( uploadInfo,
                expDownloadInfo );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            }
        } finally {
            ConfUtil.deleteGateWayStatisticalConf();
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void prepareEnv() throws Exception {
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        fileId = ScmFileUtils.create( ws, fileName, filePath );
        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();
        // 更新网关配置
        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_upload" );
        confMap1.put( "scm.statistics.rawDataCacheSize", "10" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );

        // 清理统计表
        StatisticsUtils.clearStatisticalInfo();
    }

    private void createBreakpointFile() throws Exception {
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                throw e;
            }
        }
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName );
        InputStream inputStream = new FileInputStream( filePath );
        breakpointFile.upload( inputStream );
        inputStream.close();
    }

    private void updateFile() throws Exception {
        ScmFile scmFile = ScmFactory.File.getInstance( ws, fileId );
        scmFile.updateContent( filePath );
    }

    private void crdDir() throws ScmException {
        String dirPath = "/3594";
        if ( !ScmFactory.Directory.isInstanceExist( ws, dirPath ) ) {
            ScmFactory.Directory.createInstance( ws, dirPath );
        }
        ScmDirectory dir = ScmFactory.Directory.getInstance( ws, dirPath );
        ScmCursor< ScmFileBasicInfo > files = dir
                .listFiles( new BasicBSONObject() );
        Assert.assertFalse( files.hasNext() );

        ScmFactory.Directory.deleteInstance( ws, dirPath );
    }
}