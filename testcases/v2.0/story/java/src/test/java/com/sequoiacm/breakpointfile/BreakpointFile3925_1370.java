package com.sequoiacm.breakpointfile;

import java.io.*;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @descreption SCM-3925:以文件方式断点续传文件（指定文件校验） SCM-1370:以文件方式断点续传文件（指定文件校验）
 * @author YiPan
 * @date 2021/10/29
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class BreakpointFile3925_1370 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "file3925";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 10;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private boolean runSuccess = false;

    @BeforeClass()
    private void setUp() throws Exception {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        downloadPath = localPath + File.separator + "downloadFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // 创建断点文件,上传部分文件
        int partFileSize = 1024 * 1024 * 5;
        BreakpointUtil.createBreakpointFile( ws, filePath, fileName,
                partFileSize, ScmChecksumType.ADLER32 );
        // 检查断点文件属性信息
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        checkBreakpointFileAttribute( breakpointFile, partFileSize );

        // 重新以文件形式续传完整文件，并将断点文件转换成scm文件
        ScmFile scmFile = uploadBreakpointFile( breakpointFile );
        // 校验scm文件属性和md5
        checkFileAttributes( scmFile );
        scmFile.getContent( downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkBreakpointFileAttribute( ScmBreakpointFile breakpointFile,
            int partFileSize ) {
        Assert.assertEquals( breakpointFile.getUploadSize(), partFileSize );
        Assert.assertEquals( breakpointFile.isCompleted(), false );
        Assert.assertEquals( breakpointFile.getWorkspace().getName(),
                ws.getName() );
        Assert.assertEquals( breakpointFile.getChecksumType().name(),
                ScmChecksumType.ADLER32.name() );
    }

    private ScmFile uploadBreakpointFile( ScmBreakpointFile breakpointFile )
            throws ScmException {
        File file = new File( filePath );
        breakpointFile.upload( file );

        ScmFile scmfile = ScmFactory.File.createInstance( ws );
        scmfile.setContent( breakpointFile );
        scmfile.setFileName( fileName );
        scmfile.setTitle( fileName );
        fileId = scmfile.save();
        return scmfile;
    }

    private void checkFileAttributes( ScmFile file ) {
        Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
        Assert.assertEquals( file.getFileId(), fileId );

        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getAuthor(), "" );
        Assert.assertEquals( file.getTitle(), fileName );
        Assert.assertEquals( file.getSize(), fileSize );

        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), 1 );

        Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
        Assert.assertNotNull( file.getCreateTime().getTime() );
    }
}
