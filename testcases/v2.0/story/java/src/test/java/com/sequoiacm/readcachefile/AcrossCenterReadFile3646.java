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
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @Testcase: SCM-3646:出现内部异常，指定跨站点读不缓存
 * @author YiPan
 * @date 2021.7.9
 */
public class AcrossCenterReadFile3646 extends TestScmBase {
    private final int branSitesNum = 3;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String fileName = "file3646";
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

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        tmpPath = localPath + File.separator + "tmpFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( tmpPath, fileSize / 2 );

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

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        Object[] expDataSites = { ScmInfo.getRootSite().getSiteId(),
                branchSite1.getSiteId() };
        SiteWrapper[] expMetaSites = { ScmInfo.getRootSite(), branchSite1,
                branchSite2 };

        // 分站点1上传文件
        fileId = ScmFileUtils.create( branchSite1Ws,
                fileName + "_" + UUID.randomUUID(), filePath );

        // 分站点2读取文件缓存至本地
        AcrossCenterReadFileUtils.readFile( branchSite2Ws, fileId, localPath );

        // 替换站点2上的数据文件lob（构建内部异常）
        TestSdbTools.Lob.removeLob( branchSite2, wsp, fileId );
        TestSdbTools.Lob.putLob( branchSite2, wsp, fileId, tmpPath );

        // getContent读取,检查文件正确性,检查缓存正确性
        String contentReadFile = AcrossCenterReadFileUtils.getContentReadFile(
                branchSite1Ws, fileId, localPath,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
        Assert.assertEquals( TestTools.getMD5( contentReadFile ),
                TestTools.getMD5( filePath ) );

        // 校验
        Object[] actCacheDataSites = AcrossCenterReadFileUtils
                .getCacheDataSites( wsp.getName(), fileId, localPath,
                        filePath );
        Assert.assertEqualsNoOrder( actCacheDataSites, expDataSites );
        ScmFileUtils.checkMeta( branchSite2Ws, fileId, expMetaSites );

        // getInputStream读取,检测文件正确性,检查缓存正确性
        String inputStreamReadFile = AcrossCenterReadFileUtils
                .getInputStreamReadFile( branchSite2Ws, fileId, localPath,
                        CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( inputStreamReadFile ) );
        actCacheDataSites = AcrossCenterReadFileUtils.getCacheDataSites(
                wsp.getName(), fileId, localPath, filePath );
        Assert.assertEqualsNoOrder( actCacheDataSites, expDataSites );
        ScmFileUtils.checkMeta( branchSite2Ws, fileId, expMetaSites );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws ScmException {
        if ( runSuccess ) {
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