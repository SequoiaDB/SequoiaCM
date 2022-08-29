package com.sequoiacm.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.bson.BSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmOutputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.MimeType;
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
 * @Description: SCM-3593:覆盖所有上传接口，查询上传接口的统计信息
 * @author fanyu
 * @Date:2021/03/30
 * @version:1.0
 */
public class StatisticsFile3593 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileNameBase = "file3593";
    private List< ScmId > fileIdList = new ArrayList<>();
    private int fileSize = 201 * 1024;
    private int totalFileSize = 0;
    private int totalUploadTime = 0;
    private Date endDate = null;
    private Date beginDate = null;
    private Calendar calendar = null;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws Exception {
        calendar = Calendar.getInstance();
        List< SiteWrapper > siteList = ScmInfo.getAllSites();
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

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        // 上传文件
        totalUploadTime += createFileByInputStream();
        totalUploadTime += createFileByOutputStream();
        totalUploadTime += createFileByOffSet();
        totalUploadTime += createFileByBreakpointFile();
        cancelCreateFile();
        // 覆盖文件
        totalUploadTime += overwriteFile();

        StatisticsUtils.waitStatisticalInfoCount( fileIdList.size() + 1 );
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 1 );
        endDate = calendar.getTime();
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 10 );
        beginDate = calendar.getTime();
        // 查询上传接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( session ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).endDate( endDate )
                .workspace( wsp.getName() ).timeAccuracy( ScmTimeAccuracy.DAY )
                .upload().get();
        // 检查结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), ScmTimeAccuracy.DAY,
                fileIdList.size() + 1,
                totalFileSize / ( fileIdList.size() + 1 ) - 1,
                totalUploadTime / ( fileIdList.size() + 1 ) );
        StatisticsUtils.checkScmFileStatisticInfo( uploadInfo, expUploadInfo );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
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

    private void prepareEnv() throws Exception {
        // 清理环境
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileNameBase ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        // 更新网关配置
        ConfUtil.deleteGateWayStatisticalConf();

        Map< String, String > confMap1 = new HashMap<>();
        confMap1.put( "scm.statistics.types", "file_upload" );
        confMap1.put( "scm.statistics.types.file_upload.conditions.workspaces",
                wsp.getName() );
        confMap1.put( "scm.statistics.rawDataCacheSize", "10" );
        confMap1.put( "scm.statistics.rawDataReportPeriod", "1000" );
        ConfUtil.updateConf( ConfUtil.GATEWAY_SERVICE_NAME, confMap1 );

        // 更新admin-server配置
        Map< String, String > confMap2 = new HashMap<>();
        confMap2.put( "scm.statistics.timeGranularity", "DAY" );
        ConfUtil.updateConf( ConfUtil.ADMINSERVER_SERVICE_NAME, confMap2 );
    }

    private long createFileByInputStream() throws Exception {
        long uploadBeginTime = System.currentTimeMillis();
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileNameBase + "_" + UUID.randomUUID() );
        file.setAuthor( fileNameBase );
        file.setContent( new FileInputStream( filePath ) );
        fileIdList.add( file.save() );
        totalFileSize += fileSize;
        return System.currentTimeMillis() - uploadBeginTime;
    }

    private long createFileByOutputStream() throws ScmException, IOException {
        long uploadBeginTime = System.currentTimeMillis();
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileNameBase + "_" + UUID.randomUUID() );
        file.setAuthor( fileNameBase );
        ScmOutputStream sos = ScmFactory.File.createOutputStream( file );
        byte[] buffer = TestTools.getBuffer( filePath );
        sos.write( buffer );
        sos.commit();
        fileIdList.add( file.getFileId() );
        totalFileSize += fileSize;
        return System.currentTimeMillis() - uploadBeginTime;
    }

    private long createFileByOffSet() throws ScmException, IOException {
        long uploadBeginTime = System.currentTimeMillis();
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileNameBase + "_" + UUID.randomUUID() );
        file.setAuthor( fileNameBase );
        file.setMimeType( MimeType.CSS );
        ScmOutputStream sos = ScmFactory.File.createOutputStream( file );
        byte[] buffer = TestTools.getBuffer( filePath );
        int off = 0;
        int len = fileSize / 2;
        sos.write( buffer, off, len );
        sos.commit();
        fileIdList.add( file.getFileId() );
        totalFileSize += fileSize / 2;
        return System.currentTimeMillis() - uploadBeginTime;
    }

    private void cancelCreateFile() throws ScmException, IOException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileNameBase + "_" + UUID.randomUUID() );
        file.setAuthor( fileNameBase );
        ScmOutputStream sos = ScmFactory.File.createOutputStream( file );
        byte[] buffer = TestTools.getBuffer( filePath );
        sos.write( buffer );
        sos.cancel();
    }

    private long overwriteFile() throws Exception {
        long uploadBeginTime = System.currentTimeMillis();
        ScmFile file = ScmFactory.File.createInstance( ws );
        String fileName = fileNameBase + "_" + UUID.randomUUID();
        file.setFileName( fileName );
        file.setAuthor( fileNameBase );
        file.setContent( filePath );
        file.save();
        totalFileSize += fileSize;

        ScmFile overwriteFile = ScmFactory.File.createInstance( ws );
        overwriteFile.setFileName( fileName );
        overwriteFile.setAuthor( fileNameBase );
        overwriteFile.setContent( filePath );
        totalFileSize += fileSize;
        fileIdList.add( overwriteFile.save( new ScmUploadConf( true ) ) );
        return System.currentTimeMillis() - uploadBeginTime;
    }

    private long createFileByBreakpointFile() throws ScmException, IOException {
        long uploadBeginTime = System.currentTimeMillis();
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileNameBase + UUID.randomUUID() );
        InputStream inputStream = new FileInputStream( filePath );
        breakpointFile.upload( inputStream );
        inputStream.close();
        ScmFile file = ScmFactory.File.createInstance( ws );
        String fileName = fileNameBase + "_" + UUID.randomUUID();
        file.setFileName( fileName );
        file.setAuthor( fileNameBase );
        file.setContent( breakpointFile );
        fileIdList.add( file.save() );
        totalFileSize += fileSize;
        return System.currentTimeMillis() - uploadBeginTime;
    }
}