package com.sequoiacm.net.readcachefile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.*;
import java.util.List;
import java.util.UUID;

/**
 * @Testcase: SCM-3645:指定跨站点读不缓存 SCM-3647:指定跨站点不缓存读取文件，文件为空
 * @author YiPan
 * @date 2021.7.9
 */
public class AcrossCenterReadFile3645_3647 extends TestScmBase {
    private final int branSitesNum = 2;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3645";
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
        filePath = localPath + File.separator + "localFile_" + fileName
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );

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

    @DataProvider(name = "DataProvider")
    public Object[] FileSize() {
        Object[] filesizes = { 0, 1024 * 1024 * 100 };
        return filesizes;
    }

    @Test(groups = { "fourSite" }, dataProvider = "DataProvider")
    public void test( int fileSize ) throws Exception {
        TestTools.LocalFile.createFile( filePath, fileSize );
        SiteWrapper[] expSites = { branchSite1 };
        // branchSite1创建文件
        fileId = ScmFileUtils.create( branchSite1Ws,
                fileName + "_" + UUID.randomUUID(), filePath );

        // branchSite2跨站点读不缓存，检查文件缓存
        String contentReadFile = AcrossCenterReadFileUtils.getContentReadFile(
                branchSite2Ws, fileId, localPath,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( contentReadFile ) );
        ScmFileUtils.checkMeta( branchSite2Ws, fileId, expSites );

        // branchSite2跨站点读不缓存，检查文件缓存
        String inputStreamReadFile = AcrossCenterReadFileUtils
                .getInputStreamReadFile( branchSite2Ws, fileId, localPath,
                        CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE );
        Assert.assertEquals( TestTools.getMD5( filePath ),
                TestTools.getMD5( inputStreamReadFile ) );
        ScmFileUtils.checkMeta( branchSite2Ws, fileId, expSites );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
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