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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * @Testcase: SCM-3648:指定偏移读取 SCM-3649:文件为空，指定偏移读取
 * @author YiPan
 * @date 2021.7.9
 */
public class AcrossCenterReadFile3648_3649 extends TestScmBase {
    private final int branSitesNum = 2;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "file3648";
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
        Object[] filesizes = { 0, 1024 * 100 };
        return filesizes;
    }

    @Test(groups = { "fourSite" }, dataProvider = "DataProvider")
    public void test( int fileSize ) throws Exception {
        TestTools.LocalFile.createFile( filePath, fileSize );
        // 上传文件
        fileId = ScmFileUtils.create( branchSite1Ws,
                fileName + "_" + UUID.randomUUID(), filePath );

        // 执行seek偏移量<文件长度读取
        String inputStreamSeekFile = AcrossCenterReadFileUtils
                .getInputStreamSeekFile( branchSite2Ws, fileId, localPath,
                        CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK,
                        fileSize / 2 );
        String tmpPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        TestTools.LocalFile.readFile( filePath, fileSize / 2, tmpPath );
        Assert.assertEquals( TestTools.getMD5( inputStreamSeekFile ),
                TestTools.getMD5( tmpPath ) );

        // 执行seek偏移量>文件长度读取
        try {
            AcrossCenterReadFileUtils.getInputStreamSeekFile( branchSite2Ws,
                    fileId, localPath,
                    CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK,
                    fileSize + 1 );
            Assert.fail( "except failure but succeed" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
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