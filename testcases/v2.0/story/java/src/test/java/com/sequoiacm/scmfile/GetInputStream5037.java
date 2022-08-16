package com.sequoiacm.scmfile;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @descreption SCM-5037:getInputStream接口测试
 * @author YiPan
 * @date 2022/7/25
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class GetInputStream5037 extends TestScmBase {
    private String fileName = "file5037";
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private int fileSize = 1024 * 10;
    private ScmSession rootSiteSession;
    private ScmSession branchSiteSession;
    private ScmWorkspace rootSiteWs;
    private ScmWorkspace branchSiteWs;
    private ScmId fileId;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        downloadPath = localPath + File.separator + "downLoadFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        WsWrapper wsp = ScmInfo.getWs();
        rootSiteSession = TestScmTools.createSession( ScmInfo.getRootSite() );
        branchSiteSession = TestScmTools
                .createSession( ScmInfo.getBranchSite() );
        rootSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                rootSiteSession );
        branchSiteWs = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                branchSiteSession );
        BSONObject query = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, query );

        ScmFile file = ScmFactory.File.createInstance( rootSiteWs );
        file.setFileName( fileName );
        file.setContent( filePath );
        fileId = file.save();
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // 主站点下载
        ScmFile rootSiteFile = ScmFactory.File.getInstance( rootSiteWs,
                fileId );
        inputStream2File( rootSiteFile.getInputStream(), downloadPath );
        checkFileMd5();

        // 分站点，不缓存下载
        ScmFile branchSiteFile = ScmFactory.File.getInstance( branchSiteWs,
                fileId );
        inputStream2File( branchSiteFile.getInputStream(
                CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE ),
                downloadPath );
        checkFileMd5();
        // 校验跨站点读不缓存
        SiteWrapper[] expSites = { ScmInfo.getRootSite() };
        ScmFileUtils.checkMeta( rootSiteWs, fileId, expSites );

        // 指定无效flag
        int[] readFlag = { CommonDefine.ReadFileFlag.SCM_READ_FILE_LOCALSITE,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_NEEDSEEK,
                CommonDefine.ReadFileFlag.SCM_READ_FILE_WITHDATA };
        for ( int i = 0; i < readFlag.length; i++ ) {
            try {
                branchSiteFile.getInputStream( readFlag[ i ] );
                Assert.fail( "except fail but success" );
            } catch ( ScmException e ) {
                if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                    throw e;
                }
            }
        }
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess ) {
            try {
                ScmFactory.File.deleteInstance( rootSiteWs, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                rootSiteSession.close();
                branchSiteSession.close();
            }
        }
    }

    private void checkFileMd5() throws IOException {
        String actMd5 = TestTools.getMD5( downloadPath );
        String expMd5 = TestTools.getMD5( filePath );
        Assert.assertEquals( actMd5, expMd5 );
        TestTools.LocalFile.removeFile( downloadPath );
    }

    private static String inputStream2File( InputStream inputStream,
            String downloadPath ) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream( downloadPath, true );
            byte[] read_buf = new byte[ 1024 ];
            int read_len = 0;
            while ( ( read_len = inputStream.read( read_buf ) ) > -1 ) {
                fos.write( read_buf, 0, read_len );
            }
        } finally {
            if ( fos != null ) {
                fos.close();
            }
        }
        return downloadPath;
    }
}
