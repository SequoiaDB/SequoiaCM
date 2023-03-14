package com.sequoiacm.breakpointfile;

import java.io.*;

import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmBreakpointFileOption;
import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;

/**
 * @Description SCM-4039:创建可缓存断点文件，以输入流方式上传数据
 * @Author zhangyanan
 * @Date 2021.11.5
 * @UpdataAuthor zhangyanan
 * @UpdateDate 2021.11.5
 * @version 1.00
 */
public class BreakpointFile4039 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "scmfile4039_";
    private File localPath = null;
    private boolean runSuccess = false;

    @BeforeClass()
    private void setUp() throws ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.oneSite, GroupTags.twoSite,
            GroupTags.fourSite }, dataProvider = "dataProvider")
    private void test( int fileSize, String fileName,
            ScmChecksumType scmChecksumType ) throws ScmException, IOException {
        String filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        String checkFilePath = localPath + File.separator + "check_localFile_"
                + fileSize + ".txt";
        BreakpointUtil.createFile( filePath, fileSize );
        // 创建断点文件
        ScmBreakpointFile breakpointFile = createBreakpointFile( fileName,
                filePath, fileSize, scmChecksumType );
        // 创建scm文件,并将创建的断点文件作为文件的内容
        breakpointFileToScmFile( breakpointFile, fileName, fileSize, filePath,
                checkFilePath, scmChecksumType );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private ScmBreakpointFile createBreakpointFile( String fileName,
            String filePath, int fileSize, ScmChecksumType scmChecksumType )
            throws ScmException, IOException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName,
                        new ScmBreakpointFileOption( scmChecksumType, fileSize,
                                true ),
                        ScmType.BreakpointFileType.BUFFERED );
        InputStream inputStream = new FileInputStream( filePath );
        breakpointFile.incrementalUpload( inputStream, true );
        inputStream.close();
        return breakpointFile;
    }

    private void breakpointFileToScmFile( ScmBreakpointFile breakpointFile,
            String fileName, int fileSize, String filePath,
            String checkFilePath, ScmChecksumType scmChecksumType )
            throws ScmException, IOException {
        long checkSum = getCheckSum( fileName + "_1", filePath,
                scmChecksumType );
        Assert.assertEquals( breakpointFile.getChecksum(), checkSum );
        Assert.assertEquals( breakpointFile.getUploadSize(), fileSize );
        BreakpointUtil.checkScmFile( ws, fileName, filePath, checkFilePath );
    }

    @DataProvider(name = "dataProvider")
    public Object[][] generateDate() {
        // 续传数据大小小于5
        int fileSize1 = 1024 * 1024 * 2;
        String fileName1 = fileName + 1;
        final ScmChecksumType adler32 = ScmChecksumType.ADLER32;

        // 续传数据大小等于5M
        int fileSize2 = 1024 * 1024 * 5;
        String fileName2 = fileName + 2;
        final ScmChecksumType none = ScmChecksumType.NONE;

        // 续传数据大小为5M整数
        int fileSize3 = 1024 * 1024 * 15;
        String fileName3 = fileName + 3;
        final ScmChecksumType crc32 = ScmChecksumType.CRC32;

        // 续传数据大小非为5M整倍数
        int fileSize4 = 1024 * 1024 * 16;
        String fileName4 = fileName + 4;

        // 上传数据大小为0M
        int fileSize5 = 1024 * 1024 * 0;
        String fileName5 = fileName + 5;

        return new Object[][] { { fileSize1, fileName1, adler32 },
                { fileSize2, fileName2, none }, { fileSize3, fileName3, crc32 },
                { fileSize4, fileName4, none },
                { fileSize5, fileName5, none } };
    }

    public long getCheckSum( String fileName, String filePath,
            ScmChecksumType scmChecksumType ) throws IOException, ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, fileName, scmChecksumType );
        InputStream inputStream = new FileInputStream( filePath );
        breakpointFile.incrementalUpload( inputStream, true );
        inputStream.close();
        long checkSum = breakpointFile.getChecksum();
        ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
        return checkSum;
    }
}
