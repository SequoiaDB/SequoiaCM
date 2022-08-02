/**
 *
 */
package com.sequoiacm.breakpointfile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1375.java 不校验文件，断点续传已更新文件
 * @author luweikang
 * @date 2018年5月18日
 */
public class BreakpointFile1375 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName1 = "scmfile1375_1";
    private String fileName2 = "scmfile1375_2";
    private String fileName3 = "scmfile1375_3";
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > DBSites = ScmBreakpointFileUtils.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = DBSites.get( new Random().nextInt( DBSites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException, IOException{

        // 创建断点文件,大小为1024*512
        BreakpointUtil.createBreakpointFile( ws, filePath, fileName1,
                1024 * 512, ScmChecksumType.NONE );
        BreakpointUtil.createBreakpointFile( ws, filePath, fileName2,
                1024 * 512, ScmChecksumType.NONE );
        BreakpointUtil.createBreakpointFile( ws, filePath, fileName3,
                1024 * 512, ScmChecksumType.NONE );

        // 更新断点文件,删除部分文件数据
        this.uploadBreakpointFile1();

        // 更新断点文件,修改未上传原文件内容
        this.uploadBreakpointFile2();

        // 更新断点文件,追加内容到源文件
        this.uploadBreakpointFile3();

    }

    private void uploadBreakpointFile1() throws IOException, ScmException {

        InputStream inputStream = new FileInputStream( filePath );
        byte[] fileByte = new byte[ fileSize ];
        inputStream.read( fileByte );
        inputStream.close();

        byte[] newFileByte = new byte[ fileSize - 512 * 512 ];
        System.arraycopy( fileByte, 512 * 512, newFileByte, 0,
                newFileByte.length );

        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName1 );
        InputStream inputStream2 = new ByteArrayInputStream( newFileByte );
        breakpointFile.upload( inputStream2 );
        inputStream2.close();

        checkBreakpointFile( fileName1, fileSize - 512 * 512 );
    }

    private void uploadBreakpointFile2() throws IOException, ScmException {

        InputStream inputStream = new FileInputStream( filePath );
        byte[] fileByte = new byte[ fileSize ];
        inputStream.read( fileByte, 0, fileSize );
        inputStream.close();

        byte[] basicByte = { 11, 22, 33, 44, 55, 66, 77, 88, 99 };
        for ( int i = 0; i < 100; i++ ) {
            int random = 1024 * 512 + new Random().nextInt( 1024 * 512 );
            fileByte[ random ] = basicByte[ new Random().nextInt( 9 ) ];
        }

        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName2 );
        InputStream inputStream2 = new ByteArrayInputStream( fileByte );
        breakpointFile.upload( inputStream2 );
        inputStream2.close();

        checkBreakpointFile( fileName2, fileSize );
    }

    private void uploadBreakpointFile3() throws IOException, ScmException {

        InputStream inputStream = new FileInputStream( filePath );
        byte[] newFileByte = new byte[ fileSize + 1024 ];
        inputStream.read( newFileByte, 0, fileSize );
        inputStream.close();

        byte[] basicByte = { 11, 22, 33, 44, 55, 66, 77, 88, 99 };
        for ( int i = 0; i < 1024; i++ ) {
            newFileByte[ fileSize + i ] = basicByte[ new Random()
                    .nextInt( 9 ) ];
        }

        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName3 );
        InputStream inputStream2 = new ByteArrayInputStream( newFileByte );
        breakpointFile.upload( inputStream2 );
        inputStream2.close();

        checkBreakpointFile( fileName3, fileSize + 1024 );
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName1 );
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName2 );
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName3 );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void checkBreakpointFile( String fileName, int fileSize )
            throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        Assert.assertEquals( breakpointFile.getUploadSize(), fileSize,
                "check upload size" );
    }
}
