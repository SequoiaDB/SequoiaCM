package com.sequoiacm.readcachefile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * @Testcase: SCM-3650:出现内部异常，指定偏移读
 * @author YiPan
 * @date 2021.7.9
 */
public class AcrossCenterReadFile3650 extends TestScmBase {
    private final int branSitesNum = 2;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String downloadPath = null;
    private String fileName = "file3650";
    private WsWrapper wsp = null;
    private List< SiteWrapper > branSites = null;
    private ScmSession branchSite1session;
    private ScmSession branchSite2session;
    private ScmWorkspace branchSite1Ws;
    private ScmWorkspace branchSite2Ws;
    private SiteWrapper branchSite1;
    private SiteWrapper branchSite2;
    private ScmId fileId = null;
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpPath = localPath + File.separator + "tmpFile_" + fileSize + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize / 2 );
        TestTools.LocalFile.createFile( tmpPath, fileSize );

        downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );

        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();
        branchSite1 = branSites.get( 0 );
        branchSite2 = branSites.get( 1 );
        branchSite1session = TestScmTools.createSession( branchSite1 );
        branchSite2session = TestScmTools.createSession( branchSite2 );
        branchSite1Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite1session );
        branchSite2Ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSite2session );
    }

    @Test(groups = { "fourSite", "net" })
    public void netTest() throws Exception {
        Object[] expDataSites = new Object[] { branchSite1.getSiteId() };
        SiteWrapper[] expMetaSites = new SiteWrapper[] { branchSite1,
                branchSite2 };

        // 分站点1上传文件
        fileId = ScmFileUtils.create( branchSite1Ws,
                fileName + "_" + UUID.randomUUID(), filePath );

        // 分站点2读取文件缓存至本地
        AcrossCenterReadFileUtils.readFile( branchSite2Ws, fileId, localPath );

        // 替换站点2上的数据文件lob（构建内部异常）
        TestSdbTools.Lob.removeLob( branchSite2, wsp, fileId );
        TestSdbTools.Lob.putLob( branchSite2, wsp, fileId, tmpPath );

        // 偏移读取,检测文件正确性,检查缓存正确性
        TestTools.LocalFile.removeFile( downloadPath );
        try {
            AcrossCenterReadFileUtils.getInputStreamSeekFile( branchSite2Ws,
                    fileId, localPath,
                    CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK,
                    fileSize / 3 );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DATA_CORRUPTED ) {
                throw e;
            }
        }
        Object[] actcacheDataSites = AcrossCenterReadFileUtils
                .getCacheDataSites( wsp.getName(), fileId, localPath,
                        filePath );
        Assert.assertEqualsNoOrder( actcacheDataSites, expDataSites );
        ScmFileUtils.checkMeta( branchSite2Ws, fileId, expMetaSites );
        runSuccess = true;
    }

    @Test(groups = { "fourSite", "star" })
    public void starTest() throws Exception {
        Object[] expDataSites = new Object[] {
                ScmInfo.getRootSite().getSiteId(), branchSite1.getSiteId() };
        SiteWrapper[] expMetaSites = new SiteWrapper[] { ScmInfo.getRootSite(),
                branchSite1, branchSite2 };

        // 分站点1上传文件
        fileId = ScmFileUtils.create( branchSite1Ws,
                fileName + "_" + UUID.randomUUID(), filePath );

        // 分站点2读取文件缓存至本地
        AcrossCenterReadFileUtils.readFile( branchSite2Ws, fileId, localPath );

        // 替换站点2上的数据文件lob（构建内部异常）
        TestSdbTools.Lob.removeLob( branchSite2, wsp, fileId );
        TestSdbTools.Lob.putLob( branchSite2, wsp, fileId, tmpPath );

        // 偏移读取,检测文件正确性,检查缓存正确性
        TestTools.LocalFile.removeFile( downloadPath );
        try {
            AcrossCenterReadFileUtils.getInputStreamSeekFile( branchSite2Ws,
                    fileId, localPath,
                    CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK,
                    fileSize / 3 );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DATA_CORRUPTED ) {
                throw e;
            }
        }
        Object[] actcacheDataSites = AcrossCenterReadFileUtils
                .getCacheDataSites( wsp.getName(), fileId, localPath,
                        filePath );
        Assert.assertEqualsNoOrder( actcacheDataSites, expDataSites );
        ScmFileUtils.checkMeta( branchSite2Ws, fileId, expMetaSites );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmFactory.File.deleteInstance( branchSite1Ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                branchSite1session.close();
                branchSite2session.close();
            }
        }
    }
}
