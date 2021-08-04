package com.sequoiacm.net.readcachefile;

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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @Testcase: SCM-3657:ScmFactory.File.createInputStream()参数校验
 * @author YiPan
 * @date 2021.7.9
 */
public class AcrossCenterReadFile3657 extends TestScmBase {
    private final int branSitesNum = 3;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private String tmpPath = null;
    private String downloadPath = null;
    private String fileName = "file3657";
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
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize / 2 );

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

    @Test(groups = { "fourSite" })
    public void test() throws Exception {
        // 分站点1上传文件
        fileId = ScmFileUtils.create( branchSite1Ws,
                fileName + "_" + UUID.randomUUID(), filePath );

        // 单个无效值
        getInputStreamReadFileWithInvalid( branchSite2,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_LOCALSITE, false );
        getInputStreamReadFileWithInvalid( branchSite2,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_WITHDATA, false );

        // 指定跨站点不缓存读，指定seek
        getInputStreamReadFileWithInvalid( branchSite2,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE, true );

        // 多个值包含无效值
        getInputStreamReadFileWithInvalid( branchSite2,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_LOCALSITE
                        | CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE,
                false );
        getInputStreamReadFileWithInvalid( branchSite2,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_WITHDATA
                        | CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE,
                false );
        runSuccess = true;
    }

    @AfterClass
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

    private void getInputStreamReadFileWithInvalid( SiteWrapper branchSite,
            int readFileFlag, boolean isSeek ) throws Exception {
        OutputStream os = null;
        ScmInputStream is = null;
        ScmError exceptError = ScmError.INVALID_ARGUMENT;
        ScmSession session = TestScmTools.createSession( branchSite );
        try {
            ScmWorkspace workspace = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), session );
            ScmFile instance = ScmFactory.File.getInstance( workspace, fileId );
            is = ScmFactory.File.createInputStream( instance, readFileFlag );
            if ( isSeek ) {
                exceptError = ScmError.OPERATION_UNSUPPORTED;
                is.seek( CommonDefine.SeekType.SCM_FILE_SEEK_SET,
                        fileSize / 2 );
            }
            os = new FileOutputStream( downloadPath );
            is.read( os );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( e.getError() != exceptError ) {
                throw e;
            }
        } finally {
            if ( is != null ) {
                is.close();
            }
            if ( os != null ) {
                os.close();
            }
            session.close();
        }
    }

}