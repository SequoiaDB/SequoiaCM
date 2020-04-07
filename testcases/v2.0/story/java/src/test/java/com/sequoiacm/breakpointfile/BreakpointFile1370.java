package com.sequoiacm.breakpointfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONException;
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
import com.sequoiadb.exception.BaseException;

/**
 * @Description BreakpointFile1370.java 以文件方式断点续传文件（指定文件校验） 
 * @author luweikang
 * @date 2018年5月11日
 */
public class BreakpointFile1370 extends TestScmBase {

    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile1370";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 5;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws JSONException, ScmException, IOException {
        //创建断点文件,上传部分文件
        int partFileSize = 1024 * 1024 * 5;
        BreakpointUtil
                .createBreakpointFile( ws, filePath, fileName, partFileSize,
                        ScmChecksumType.ADLER32 );
        //检查断点文件信息
        this.checkBreakpointFile( partFileSize );
        //重新上传文件
        ScmBreakpointFile breakpointFile = this.uploadBreakpointFile();
        //创建scm文件,并将创建的断点文件保存为文件的内容
        breakpointFile2ScmFile( breakpointFile );

    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( ws, fileId, true );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkBreakpointFile( int partFileSize ) throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );

        Assert.assertEquals( breakpointFile.getUploadSize(), partFileSize );
        Assert.assertEquals( breakpointFile.isCompleted(), false );
        Assert.assertEquals( breakpointFile.getWorkspace().getName(),
                ws.getName() );
        Assert.assertEquals( breakpointFile.getChecksumType().name(),
                ScmChecksumType.ADLER32.name() );
    }

    private ScmBreakpointFile uploadBreakpointFile() {
        ScmBreakpointFile breakpointFile = null;
        try {
            breakpointFile = ScmFactory.BreakpointFile
                    .getInstance( ws, fileName );
            InputStream inputStream = new FileInputStream( filePath );
            breakpointFile.upload( inputStream );
            inputStream.close();
        } catch ( ScmException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return breakpointFile;
    }

    private void breakpointFile2ScmFile( ScmBreakpointFile breakpointFile )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( breakpointFile );
        file.setFileName( fileName );
        file.setTitle( fileName );
        fileId = file.save();

        // check file's attribute
        checkFileAttributes( file );
    }

    private void checkFileAttributes( ScmFile file ) {
        try {
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
        } catch ( BaseException e ) {
            throw e;
        }
    }
}
