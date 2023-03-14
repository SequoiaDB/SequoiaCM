package com.sequoiacm.statistics;

import java.io.File;
import java.util.*;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmFileStatisticInfo;
import com.sequoiacm.client.element.ScmFileStatisticsType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import com.sequoiacm.testcommon.scmutils.StatisticsUtils;

/**
 * @description SCM-4085:断点文件转换为文件失败，查询统计信息
 * @author ZhangYanan
 * @createDate 2021.10.15
 * @updateUser ZhangYanan
 * @updateDate 2021.10.15
 * @updateRemark
 * @version v1.0
 */
public class StatisticsFile4085 extends TestScmBase {
    private File localPath = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String fileName = "file4085";
    private ScmSession siteSession = null;
    private ScmWorkspace siteWorkspace = null;
    private ScmId fileID = null;
    private List< ScmId > fileIdList = new ArrayList<>();
    private int fileNums = 1;
    private int fileSize = 1024 * 1024;
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

        fileID = ScmFileUtils.create( siteWorkspace, fileName, filePath );

        // 更新网关和admin配置
        StatisticsUtils.configureGatewayAndAdminInfo( wsp );
        // 设置统计起始时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) - 1 );
        beginDate = calendar.getTime();

    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    public void test() throws Exception {
        ScmBreakpointFileUtils.createAndUploadBreakpointFile( fileName, siteWorkspace,
                filePath );
        try {
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .getInstance( siteWorkspace, fileName );
            ScmFile file = ScmFactory.File.createInstance( siteWorkspace );
            file.setFileName( fileName );
            file.setAuthor( fileName );
            file.setContent( breakpointFile );
            fileIdList.add( file.save() );
            Assert.fail( "BreakPointFileToFile should failed but success!" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_EXIST.getErrorCode() ) {
                throw e;
            }
        }

        StatisticsUtils.waitStatisticalInfoCount( fileNums );

        // 设定统计结束时间
        calendar.set( Calendar.DAY_OF_YEAR,
                calendar.get( Calendar.DAY_OF_YEAR ) + 2 );
        endDate = calendar.getTime();

        // 查询上传接口统计信息
        ScmFileStatisticInfo uploadInfo = ScmSystem.Statistics
                .fileStatistician( siteSession ).user( TestScmBase.scmUserName )
                .beginDate( beginDate ).workspace( wsp.getName() )
                .endDate( endDate ).upload().get();
        // 预期结果
        ScmFileStatisticInfo expUploadInfo = new ScmFileStatisticInfo(
                ScmFileStatisticsType.FILE_UPLOAD, beginDate, endDate,
                TestScmBase.scmUserName, wsp.getName(), null, fileNums, 0, 0, 0,
                0, fileNums );
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
                ScmFactory.File.deleteInstance( siteWorkspace, fileID, true );
                ScmFactory.BreakpointFile.deleteInstance( siteWorkspace,
                        fileName );
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
